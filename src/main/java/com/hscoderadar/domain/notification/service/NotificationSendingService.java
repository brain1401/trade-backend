package com.hscoderadar.domain.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hscoderadar.domain.notification.dto.request.NotificationRequest;
import com.hscoderadar.domain.notification.entity.NotificationLog;
import com.hscoderadar.domain.notification.entity.NotificationLog.MessageType;
import com.hscoderadar.domain.notification.entity.NotificationLog.NotificationStatus;
import com.hscoderadar.domain.notification.entity.NotificationLog.NotificationType;
import com.hscoderadar.domain.notification.repository.NotificationLogRepository;
import com.hscoderadar.domain.user.entity.User;
import com.hscoderadar.domain.user.entity.UserSettings;
import com.hscoderadar.domain.user.repository.UserRepository;
import com.hscoderadar.domain.sms.service.SmsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 통합 알림 발송 서비스
 * 기존 시스템과 Python AI 서버 모두 지원
 */
@Slf4j
@Service
public class NotificationSendingService implements ApplicationRunner {

  // Redis 키 상수
  private static final String LEGACY_QUEUE_KEY = "notification:queue";
  private static final String PYTHON_QUEUE_KEY_PREFIX = "daily_notification:queue:";
  private static final String PROCESSING_KEY_PREFIX = "daily_notification:processing_queue:";
  private static final String DETAIL_KEY_PREFIX = "daily_notification:detail:";
  private static final Duration BLOCKING_TIMEOUT = Duration.ofSeconds(10);

  private final RedisTemplate<String, Object> redisObjectTemplate;
  private final RedisTemplate<String, Object> queueRedisTemplate;
  private final ObjectMapper objectMapper;
  private final UserRepository userRepository;
  private final SmsService smsService;
  private final EmailService emailService;
  private final NotificationLogRepository notificationLogRepository;
  private final Executor taskExecutor;

  // 소비자 실행 상태
  private final AtomicBoolean running = new AtomicBoolean(false);

  public NotificationSendingService(
      @Qualifier("redisObjectTemplate") RedisTemplate<String, Object> redisObjectTemplate,
      @Qualifier("queueRedisTemplate") RedisTemplate<String, Object> queueRedisTemplate,
      ObjectMapper objectMapper,
      UserRepository userRepository,
      SmsService smsService,
      EmailService emailService,
      NotificationLogRepository notificationLogRepository,
      @Qualifier("notificationTaskExecutor") Executor taskExecutor) {
    this.redisObjectTemplate = redisObjectTemplate;
    this.queueRedisTemplate = queueRedisTemplate;
    this.objectMapper = objectMapper;
    this.userRepository = userRepository;
    this.smsService = smsService;
    this.emailService = emailService;
    this.notificationLogRepository = notificationLogRepository;
    this.taskExecutor = taskExecutor;
  }

  @Override
  public void run(ApplicationArguments args) {
    log.info("통합 알림 서비스 시작");
    startConsumers();
  }

  /**
   * 모든 소비자 시작
   */
  public void startConsumers() {
    if (running.compareAndSet(false, true)) {
      // 기존 알림 큐 소비자
      taskExecutor.execute(this::processLegacyNotifications);

      // Python AI 서버 이메일 알림 소비자
      taskExecutor.execute(() -> consumePythonNotifications("EMAIL"));

      // Python AI 서버 SMS 알림 소비자 (필요시 활성화)
      // taskExecutor.execute(() -> consumePythonNotifications("SMS"));

      log.info("모든 알림 소비자가 시작되었습니다");
    }
  }

  /**
   * 소비자 중지
   */
  public void stopConsumers() {
    running.set(false);
    log.info("알림 소비자 중지 요청됨");
  }

  /**
   * 기존 알림 시스템 처리 (레거시)
   */
  public void processLegacyNotifications() {
    log.info("기존 알림 큐 소비자 스레드 시작");

    while (running.get()) {
      try {
        Long queueSize = redisObjectTemplate.opsForList().size(LEGACY_QUEUE_KEY);
        if (queueSize != null && queueSize > 0) {
          log.info("처리할 알림 {}건을 확인했습니다.", queueSize);
        }

        Object rawData = redisObjectTemplate.opsForList().leftPop(LEGACY_QUEUE_KEY);
        if (rawData == null) {
          // 큐가 비어있으면 잠시 대기
          Thread.sleep(5000);
          continue;
        }

        try {
          NotificationRequest request = objectMapper.readValue((String) rawData, NotificationRequest.class);
          log.info("기존 알림 처리 시작: userId={}, bookmarkId={}", request.userId(), request.bookmarkId());
          sendNotificationForUser(request);
        } catch (JsonProcessingException e) {
          log.error("기존 알림 데이터를 파싱하는데 실패했습니다: {}", rawData, e);
        }
      } catch (InterruptedException ie) {
        Thread.currentThread().interrupt();
        break;
      } catch (Exception e) {
        log.error("기존 알림 처리 중 오류", e);
        try {
          Thread.sleep(5000);
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
          break;
        }
      }
    }

    log.info("기존 알림 큐 소비자 스레드 종료");
  }

  /**
   * Python AI 서버 알림 소비 메인 루프
   */
  private void consumePythonNotifications(String notificationType) {
    log.info("{} 알림 소비자 스레드 시작", notificationType);

    String queueKey = PYTHON_QUEUE_KEY_PREFIX + notificationType;
    String processingKey = PROCESSING_KEY_PREFIX + notificationType;
    ListOperations<String, Object> listOps = queueRedisTemplate.opsForList();

    while (running.get()) {
      try {
        // BLMOVE 명령어로 안전한 작업 이동 (원자적 연산)
        String taskId = (String) listOps.rightPopAndLeftPush(
            queueKey,
            processingKey,
            BLOCKING_TIMEOUT);

        if (taskId != null) {
          log.debug("Python 알림 작업 수신: {} ({})", taskId, notificationType);
          processPythonNotification(taskId, notificationType, processingKey);
        }
      } catch (Exception e) {
        log.error("{} 알림 처리 중 오류", notificationType, e);
        // 잠시 대기 후 재시도
        try {
          Thread.sleep(5000);
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
          break;
        }
      }
    }

    log.info("{} 알림 소비자 스레드 종료", notificationType);
  }

  /**
   * Python AI 서버 개별 알림 처리
   */
  private void processPythonNotification(String taskId, String notificationType, String processingKey) {
    String detailKey = DETAIL_KEY_PREFIX + taskId;

    try {
      // HGETALL로 알림 상세 정보 조회
      Map<Object, Object> details = queueRedisTemplate.opsForHash().entries(detailKey);

      if (details.isEmpty()) {
        log.warn("알림 상세 정보를 찾을 수 없음: {}", taskId);
        removeFromProcessingQueue(processingKey, taskId);
        return;
      }

      // Map을 NotificationRequest로 변환
      NotificationRequest request = convertMapToRequest(taskId, details);

      // 알림 발송
      sendNotificationForUser(request);

      // 성공 시 처리 큐에서 제거
      removeFromProcessingQueue(processingKey, taskId);

      // 상세 정보 삭제
      queueRedisTemplate.delete(detailKey);

      log.info("Python 알림 발송 성공: {} ({})", taskId, notificationType);

    } catch (Exception e) {
      log.error("Python 알림 처리 중 예외 발생: {} ({})", taskId, notificationType, e);
      // 예외 발생 시 재시도를 위해 처리 큐에 남겨둠
    }
  }

  /**
   * Map을 NotificationRequest로 변환
   */
  private NotificationRequest convertMapToRequest(String taskId, Map<Object, Object> details) {
    String userIdStr = (String) details.get("user_id");
    Long userId = userIdStr != null ? Long.parseLong(userIdStr) : null;

    return NotificationRequest.forPythonServer(
        taskId,
        userId,
        (String) details.get("email"),
        (String) details.get("phone_number"),
        (String) details.get("subject"),
        (String) details.get("message"));
  }

  /**
   * 처리 큐에서 작업 제거
   */
  private void removeFromProcessingQueue(String processingKey, String taskId) {
    Long removed = queueRedisTemplate.opsForList().remove(processingKey, 1, taskId);
    if (removed != null && removed > 0) {
      log.debug("처리 큐에서 제거됨: {}", taskId);
    } else {
      log.warn("처리 큐에서 제거 실패: {}", taskId);
    }
  }

  /**
   * 통합 알림 발송 처리
   */
  @Transactional
  public void sendNotificationForUser(NotificationRequest request) {
    User user = userRepository.findById(request.userId())
        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + request.userId()));

    UserSettings globalSettings = user.getUserSettings();
    if (globalSettings == null) {
      log.warn("사용자(id:{})의 전역 알림 설정을 찾을 수 없어 알림을 보내지 않습니다.", user.getId());
      return;
    }

    // SMS 알림 발송
    if (shouldSendSms(request, globalSettings, user)) {
      try {
        String messageContent = request.getEffectiveTitle() + "\n" + request.getEffectiveContent();
        String phoneNumber = request.phoneNumber() != null ? request.phoneNumber() : user.getPhoneNumber();
        smsService.sendMessage(phoneNumber, messageContent);
        saveLog(user, NotificationType.SMS, phoneNumber, request, NotificationStatus.SENT, null);
        log.info("SMS 발송 성공: userId={}", user.getId());
      } catch (Exception e) {
        String phoneNumber = request.phoneNumber() != null ? request.phoneNumber() : user.getPhoneNumber();
        saveLog(user, NotificationType.SMS, phoneNumber, request, NotificationStatus.FAILED, e.getMessage());
      }
    }

    // 이메일 알림 발송
    if (shouldSendEmail(request, globalSettings)) {
      try {
        String email = request.email() != null ? request.email() : user.getEmail();
        emailService.sendEmail(email, request.getEffectiveTitle(), request.getEffectiveContent());
        saveLog(user, NotificationType.EMAIL, email, request, NotificationStatus.SENT, null);
        log.info("이메일 발송 성공: userId={}", user.getId());
      } catch (Exception e) {
        String email = request.email() != null ? request.email() : user.getEmail();
        saveLog(user, NotificationType.EMAIL, email, request, NotificationStatus.FAILED, e.getMessage());
      }
    }
  }

  /**
   * SMS 발송 여부 결정
   */
  private boolean shouldSendSms(NotificationRequest request, UserSettings globalSettings, User user) {
    if (request.isFromPythonServer()) {
      // Python AI 서버에서 온 요청: phoneNumber가 있으면 발송
      return request.phoneNumber() != null;
    } else {
      // 기존 시스템: 전역 설정과 개별 설정 모두 확인
      return globalSettings.isSmsNotificationEnabled() &&
          request.smsEnabled() &&
          user.isPhoneVerified();
    }
  }

  /**
   * 이메일 발송 여부 결정
   */
  private boolean shouldSendEmail(NotificationRequest request, UserSettings globalSettings) {
    if (request.isFromPythonServer()) {
      // Python AI 서버에서 온 요청: email이 있으면 발송
      return request.email() != null;
    } else {
      // 기존 시스템: 전역 설정과 개별 설정 모두 확인
      return globalSettings.isEmailNotificationEnabled() && request.emailEnabled();
    }
  }

  /**
   * 알림 로그 저장
   */
  private void saveLog(User user, NotificationType type, String recipient, NotificationRequest request,
      NotificationStatus status, String errorMessage) {
    NotificationLog log = NotificationLog.builder()
        .user(user)
        .taskId(request.taskId())
        .notificationType(type)
        .messageType(MessageType.DAILY_NOTIFICATION)
        .recipient(recipient)
        .title(request.getEffectiveTitle())
        .content(request.getEffectiveContent())
        .build();

    log.updateStatus(status, LocalDateTime.now(), errorMessage);
    notificationLogRepository.save(log);
  }

  /**
   * 현재 큐 상태 조회 (모니터링용)
   */
  public Map<String, Long> getQueueStatus() {
    return Map.of(
        "legacy_queue", redisObjectTemplate.opsForList().size(LEGACY_QUEUE_KEY),
        "email_queue", queueRedisTemplate.opsForList().size(PYTHON_QUEUE_KEY_PREFIX + "EMAIL"),
        "email_processing", queueRedisTemplate.opsForList().size(PROCESSING_KEY_PREFIX + "EMAIL"),
        "sms_queue", queueRedisTemplate.opsForList().size(PYTHON_QUEUE_KEY_PREFIX + "SMS"),
        "sms_processing", queueRedisTemplate.opsForList().size(PROCESSING_KEY_PREFIX + "SMS"));
  }
}