package com.hscoderadar.domain.sms.controller;

import com.hscoderadar.common.response.ApiResponse;
import com.hscoderadar.domain.sms.dto.SmsVerificationRequest;
import com.hscoderadar.domain.sms.dto.SmsVerificationResponse;
import com.hscoderadar.domain.sms.dto.SmsVerifyRequest;
import com.hscoderadar.domain.sms.dto.SmsLogResponse;
import com.hscoderadar.domain.sms.service.SmsService;
import com.hscoderadar.domain.users.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * v4.0 SMS 알림 시스템 컨트롤러
 * 
 * 휴대폰 인증부터 SMS 알림 발송까지 완전한 SMS 생태계 제공
 * - 6자리 인증코드 기반 휴대폰 번호 검증
 * - 실시간 SMS 알림 (북마크 변동사항)
 * - 세밀한 알림 설정 (북마크별, 타입별)
 * - 발송 이력 및 비용 추적
 */
@RestController
@RequestMapping("/sms")
@RequiredArgsConstructor
@Slf4j
public class SmsController {

  private final SmsService smsService;

  /**
   * v4.0 휴대폰 인증 코드 발송 API
   * 
   * @param user    인증된 사용자
   * @param request 휴대폰 번호 정보
   * @return 인증 세션 정보
   */
  @PostMapping("/verification/send")
  public ResponseEntity<ApiResponse<SmsVerificationResponse>> sendVerificationCode(
      @AuthenticationPrincipal User user,
      @Valid @RequestBody SmsVerificationRequest request) {

    log.info("SMS 인증 코드 발송 요청 - 사용자: {}, 번호: {}",
        user.getEmail(), maskPhoneNumber(request.getPhoneNumber()));

    try {
      SmsVerificationResponse response = smsService.sendVerificationCode(user, request);

      log.info("SMS 인증 코드 발송 완료 - verificationId: {}", response.getVerificationId());

      return ResponseEntity.ok(
          ApiResponse.success("인증 코드가 발송되었습니다", response));

    } catch (Exception e) {
      log.error("SMS 인증 코드 발송 실패: {}", e.getMessage(), e);
      throw e;
    }
  }

  /**
   * v4.0 휴대폰 인증 코드 확인 API
   * 
   * @param user    인증된 사용자
   * @param request 인증 정보
   * @return 인증 결과
   */
  @PostMapping("/verification/verify")
  public ResponseEntity<ApiResponse<Object>> verifyCode(
      @AuthenticationPrincipal User user,
      @Valid @RequestBody SmsVerifyRequest request) {

    log.info("SMS 인증 코드 확인 요청 - 사용자: {}, verificationId: {}",
        user.getEmail(), request.getVerificationId());

    try {
      Object response = smsService.verifyCode(user, request);

      log.info("SMS 인증 코드 확인 완료 - verificationId: {}", request.getVerificationId());

      return ResponseEntity.ok(
          ApiResponse.success("휴대폰 인증이 완료되었습니다", response));

    } catch (Exception e) {
      log.error("SMS 인증 코드 확인 실패: {}", e.getMessage(), e);
      throw e;
    }
  }

  /**
   * v4.0 휴대폰 번호 등록 API
   * 
   * @param user    인증된 사용자
   * @param request 인증 세션 정보
   * @return 등록 결과
   */
  @PostMapping("/phone/register")
  public ResponseEntity<ApiResponse<Object>> registerPhoneNumber(
      @AuthenticationPrincipal User user,
      @Valid @RequestBody Object request) {

    log.info("휴대폰 번호 등록 요청 - 사용자: {}", user.getEmail());

    try {
      Object response = smsService.registerPhoneNumber(user, request);

      log.info("휴대폰 번호 등록 완료 - 사용자: {}", user.getEmail());

      return ResponseEntity.ok(
          ApiResponse.success("휴대폰 번호가 등록되었습니다", response));

    } catch (Exception e) {
      log.error("휴대폰 번호 등록 실패: {}", e.getMessage(), e);
      throw e;
    }
  }

  /**
   * v4.0 SMS 알림 설정 조회 API
   */
  @GetMapping("/settings")
  public ResponseEntity<ApiResponse<Object>> getSmsSettings(@AuthenticationPrincipal User user) {

    log.info("SMS 알림 설정 조회 - 사용자: {}", user.getEmail());

    try {
      Object settings = smsService.getSmsSettings(user);

      return ResponseEntity.ok(
          ApiResponse.success("SMS 알림 설정 조회됨", settings));

    } catch (Exception e) {
      log.error("SMS 알림 설정 조회 실패: {}", e.getMessage(), e);
      throw e;
    }
  }

  /**
   * v4.0 SMS 알림 설정 수정 API
   */
  @PutMapping("/settings")
  public ResponseEntity<ApiResponse<Object>> updateSmsSettings(
      @AuthenticationPrincipal User user,
      @Valid @RequestBody Object request) {

    log.info("SMS 알림 설정 수정 요청 - 사용자: {}", user.getEmail());

    try {
      Object response = smsService.updateSmsSettings(user, request);

      log.info("SMS 알림 설정 수정 완료 - 사용자: {}", user.getEmail());

      return ResponseEntity.ok(
          ApiResponse.success("SMS 알림 설정이 수정되었습니다", response));

    } catch (Exception e) {
      log.error("SMS 알림 설정 수정 실패: {}", e.getMessage(), e);
      throw e;
    }
  }

  /**
   * v4.0 SMS 발송 이력 조회 API
   */
  @GetMapping("/logs")
  public ResponseEntity<ApiResponse<Page<SmsLogResponse>>> getSmsLogs(
      @AuthenticationPrincipal User user,
      @RequestParam(defaultValue = "0") int offset,
      @RequestParam(defaultValue = "20") int limit,
      @RequestParam(required = false) String type,
      @RequestParam(required = false) String status) {

    log.info("SMS 발송 이력 조회 - 사용자: {}, offset: {}, limit: {}",
        user.getEmail(), offset, limit);

    try {
      Pageable pageable = PageRequest.of(offset / limit, limit);
      Page<SmsLogResponse> logs = smsService.getSmsLogs(user, pageable, type, status);

      return ResponseEntity.ok(
          ApiResponse.success("SMS 발송 이력 조회 완료", logs));

    } catch (Exception e) {
      log.error("SMS 발송 이력 조회 실패: {}", e.getMessage(), e);
      throw e;
    }
  }

  /**
   * 휴대폰 번호 마스킹 처리 (로그용)
   */
  private String maskPhoneNumber(String phoneNumber) {
    if (phoneNumber == null || phoneNumber.length() < 8) {
      return phoneNumber;
    }
    return phoneNumber.substring(0, 3) + "****" + phoneNumber.substring(phoneNumber.length() - 4);
  }
}