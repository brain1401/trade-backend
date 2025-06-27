package com.hscoderadar.domain.sms.service;

import com.hscoderadar.domain.sms.dto.SmsVerificationRequest;
import com.hscoderadar.domain.sms.dto.SmsVerificationResponse;
import com.hscoderadar.domain.sms.dto.SmsVerifyRequest;
import com.hscoderadar.domain.sms.dto.SmsLogResponse;
import com.hscoderadar.domain.sms.entity.SmsVerificationSession;
import com.hscoderadar.domain.sms.entity.SmsLog;
import com.hscoderadar.domain.sms.repository.SmsVerificationSessionRepository;
import com.hscoderadar.domain.sms.repository.SmsLogRepository;
import com.hscoderadar.domain.users.entity.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

/**
 * v4.1 SMS 알림 시스템 서비스 구현체 (Redis 기반)
 * 
 * 휴대폰 인증부터 SMS 알림 발송까지 완전한 SMS 생태계 구현
 * Redis TTL을 활용한 자동 만료 처리 및 고성능 인증 시스템
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SmsServiceImpl implements SmsService {

  private final SmsVerificationSessionRepository sessionRepository;
  private final SmsLogRepository smsLogRepository;
  private final SmsRedisService smsRedisService;
  private final PasswordEncoder passwordEncoder;

  private static final Random RANDOM = new Random();

  /**
   * SMS 인증 코드 발송
   */
  @Override
  @Transactional
  public SmsVerificationResponse sendVerificationCode(User user, SmsVerificationRequest request) {
    String phoneNumber = request.getPhoneNumber();

    log.info("SMS 인증 코드 발송 요청 - 사용자: {}, 번호: {}",
        user.getEmail(), maskPhoneNumber(phoneNumber));

    // 1. 제한 사항 확인 (쿨다운, 일일/시간당 한도)
    SmsRedisService.SmsLimitCheckResult limitCheck = smsRedisService.checkLimits(user.getId(), phoneNumber);
    if (limitCheck.isBlocked()) {
      throw new RuntimeException(limitCheck.getBlockReason());
    }

    // 2. 기존 활성 세션 확인 및 정리
    cleanupExistingUserSessions(user.getId(), phoneNumber);

    // 3. 인증 코드 생성 및 세션 생성
    String verificationCode = generateVerificationCode();
    String verificationId = generateVerificationId();

    SmsVerificationSession session = SmsVerificationSession.builder()
        .verificationId(verificationId)
        .userId(user.getId())
        .phoneNumber(phoneNumber)
        .verificationCodeHash(passwordEncoder.encode(verificationCode))
        .attemptCount(0)
        .maxAttempts(5)
        .isVerified(false)
        .createdAt(LocalDateTime.now())
        .ttl(300L) // 5분
        .build();

    // 4. Redis에 세션 저장
    sessionRepository.save(session);

    // 5. 실제 SMS 발송 (현재는 로그만)
    sendSmsMessage(user, phoneNumber, verificationCode);

    // 6. 제한 카운터 업데이트
    smsRedisService.incrementDailyAttempts(user.getId());
    smsRedisService.incrementHourlyAttempts(user.getId());
    smsRedisService.setCooldown(user.getId(), phoneNumber);

    // 7. SMS 발송 로그 기록
    logSmsMessage(user, phoneNumber, "인증코드: " + verificationCode, SmsLog.MessageType.VERIFICATION);

    // 8. 응답 생성
    LocalDateTime now = LocalDateTime.now();
    return SmsVerificationResponse.builder()
        .verificationId(verificationId)
        .expiresAt(now.plusMinutes(5).atZone(ZoneId.systemDefault()).toString())
        .cooldownUntil(now.plusMinutes(2).atZone(ZoneId.systemDefault()).toString())
        .build();
  }

  /**
   * SMS 인증 코드 확인
   */
  @Override
  @Transactional
  public Map<String, Object> verifyCode(User user, SmsVerifyRequest request) {
    String verificationId = request.getVerificationId();
    String inputCode = request.getVerificationCode();

    log.info("SMS 인증 코드 확인 요청 - 사용자: {}, verificationId: {}",
        user.getEmail(), verificationId);

    // 1. 인증 세션 조회 (TTL 만료시 자동으로 null)
    Optional<SmsVerificationSession> sessionOpt = sessionRepository.findByVerificationId(verificationId);
    if (sessionOpt.isEmpty()) {
      return createErrorResponse("EXPIRED_OR_INVALID", "인증 세션이 만료되었거나 유효하지 않습니다.");
    }

    SmsVerificationSession session = sessionOpt.get();

    // 2. 사용자 확인
    if (!session.getUserId().equals(user.getId())) {
      return createErrorResponse("UNAUTHORIZED", "권한이 없습니다.");
    }

    // 3. 이미 인증 완료된 세션 확인
    if (session.getIsVerified()) {
      return createErrorResponse("ALREADY_VERIFIED", "이미 인증이 완료된 세션입니다.");
    }

    // 4. 최대 시도 횟수 확인
    if (session.isMaxAttemptsExceeded()) {
      return createErrorResponse("MAX_ATTEMPTS_EXCEEDED",
          String.format("최대 시도 횟수(%d회)를 초과했습니다.", session.getMaxAttempts()));
    }

    // 5. 인증 코드 검증
    if (!passwordEncoder.matches(inputCode, session.getVerificationCodeHash())) {
      // 시도 횟수 증가
      session.incrementAttemptCount();
      sessionRepository.save(session);

      int remainingAttempts = session.getMaxAttempts() - session.getAttemptCount();
      return createErrorResponse("INVALID_CODE",
          String.format("인증 코드가 일치하지 않습니다. 남은 시도 횟수: %d회", remainingAttempts));
    }

    // 6. 인증 성공 처리
    session.markAsVerified();
    sessionRepository.save(session);

    // 7. 사용자 휴대폰 번호 업데이트
    user.setPhoneNumber(session.getPhoneNumber());
    user.setPhoneVerified(true);
    user.setPhoneVerifiedAt(LocalDateTime.now());

    log.info("SMS 인증 완료 - 사용자: {}, 번호: {}",
        user.getEmail(), maskPhoneNumber(session.getPhoneNumber()));

    Map<String, Object> response = new HashMap<>();
    response.put("success", true);
    response.put("message", "휴대폰 인증이 완료되었습니다.");
    response.put("phoneNumber", maskPhoneNumber(session.getPhoneNumber()));
    response.put("verifiedAt", session.getVerifiedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

    return response;
  }

  @Override
  public Object registerPhoneNumber(User user, Object request) {
    // 이미 verifyCode에서 처리됨
    log.info("휴대폰 번호 등록 - 사용자: {}", user.getEmail());

    Map<String, Object> response = new HashMap<>();
    response.put("success", true);
    response.put("message", "휴대폰 번호가 등록되었습니다.");
    response.put("phoneNumber", maskPhoneNumber(user.getPhoneNumber()));

    return response;
  }

  @Override
  public Object getSmsSettings(User user) {
    log.info("SMS 알림 설정 조회 - 사용자: {}", user.getEmail());

    Map<String, Object> settings = new HashMap<>();
    settings.put("phoneNumber", user.getPhoneNumber() != null ? maskPhoneNumber(user.getPhoneNumber()) : null);
    settings.put("phoneVerified", user.getPhoneVerified());
    settings.put("smsNotificationEnabled", true);

    return settings;
  }

  @Override
  public Object updateSmsSettings(User user, Object request) {
    log.info("SMS 알림 설정 수정 요청 - 사용자: {}", user.getEmail());

    Map<String, Object> response = new HashMap<>();
    response.put("success", true);
    response.put("message", "SMS 설정이 업데이트되었습니다.");

    return response;
  }

  @Override
  public Page<SmsLogResponse> getSmsLogs(User user, Pageable pageable, String type, String status) {
    log.info("SMS 발송 이력 조회 - 사용자: {}, offset: {}, limit: {}",
        user.getEmail(), pageable.getOffset(), pageable.getPageSize());

    // SMS 로그는 여전히 MySQL에서 조회
    Page<SmsLog> smsLogs = smsLogRepository.findByUserOrderByCreatedAtDesc(user, pageable);

    return smsLogs.map(smsLog -> SmsLogResponse.builder()
        .id(smsLog.getId())
        .phoneNumber(smsLog.getMaskedPhoneNumber())
        .messageType(smsLog.getMessageType())
        .content(smsLog.getContent())
        .status(smsLog.getStatus())
        .costKrw(smsLog.getCostKrw())
        .sentAt(smsLog.getSentAt())
        .createdAt(smsLog.getCreatedAt())
        .build());
  }

  /**
   * 6자리 인증 코드 생성
   */
  private String generateVerificationCode() {
    return String.format("%06d", RANDOM.nextInt(1000000));
  }

  /**
   * 인증 세션 ID 생성
   */
  private String generateVerificationId() {
    return "verify_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
  }

  /**
   * 기존 사용자 세션 정리
   */
  private void cleanupExistingUserSessions(Long userId, String phoneNumber) {
    // Redis에서 해당 사용자의 기존 세션들 조회 및 삭제
    sessionRepository.findByUserIdAndPhoneNumber(userId, phoneNumber)
        .forEach(session -> {
          if (!session.getIsVerified()) {
            sessionRepository.delete(session);
            log.debug("기존 미인증 세션 삭제: {}", session.getVerificationId());
          }
        });
  }

  /**
   * 실제 SMS 발송 (현재는 로그만)
   */
  private void sendSmsMessage(User user, String phoneNumber, String verificationCode) {
    // TODO: 실제 SMS 서비스 연동
    log.info("SMS 발송 - 수신자: {}, 내용: 인증코드 [{}]",
        maskPhoneNumber(phoneNumber), verificationCode);
  }

  /**
   * SMS 발송 로그 기록
   */
  private void logSmsMessage(User user, String phoneNumber, String content, SmsLog.MessageType messageType) {
    SmsLog smsLog = SmsLog.builder()
        .user(user)
        .phoneNumber(maskPhoneNumber(phoneNumber))
        .messageType(messageType)
        .content(content)
        .status(SmsLog.SmsStatus.SENT)
        .costKrw(50) // 임시 비용
        .sentAt(LocalDateTime.now())
        .build();

    smsLogRepository.save(smsLog);
  }

  /**
   * 휴대폰 번호 마스킹 처리
   */
  private String maskPhoneNumber(String phoneNumber) {
    if (phoneNumber == null || phoneNumber.length() < 8) {
      return phoneNumber;
    }
    return phoneNumber.substring(0, 3) + "****" + phoneNumber.substring(phoneNumber.length() - 4);
  }

  /**
   * 에러 응답 생성
   */
  private Map<String, Object> createErrorResponse(String code, String message) {
    Map<String, Object> response = new HashMap<>();
    response.put("success", false);
    response.put("errorCode", code);
    response.put("message", message);
    return response;
  }
}