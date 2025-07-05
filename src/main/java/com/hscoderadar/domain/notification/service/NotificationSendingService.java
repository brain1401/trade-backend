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
import org.springframework.data.redis.RedisConnectionFailureException;
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
 * Redis 연결 실패에 대한 복원력 강화
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

  // Redis 연결 실패 재시도 설정
  private static final int MAX_RETRY_ATTEMPTS = 3;
  private static final long RETRY_DELAY_MS = 5000;

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
  private final AtomicBoolean redisAvailable = new AtomicBoolean(true);

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

    // Redis 연결 상태 확인
    if (checkRedisConnection()) {
      startConsumers();
    } else {
      log.warn("Redis 연결 실패로 인해 알림 큐 소비자를 시작하지 않음 (알림 기능은 직접 호출로만 작동)");
    }
  }

  /**
   * Redis 연결 상태 확인
   */
  private boolean checkRedisConnection() {
    try {
      redisObjectTemplate.opsForValue().get("connection_test");
      redisAvailable.set(true);
      log.info("Redis 연결 상태 정상");
      return true;
    } catch (RedisConnectionFailureException e) {
      redisAvailable.set(false);
      log.error("Redis 연결 실패 감지: {}", e.getMessage());
      return false;
    } catch (Exception e) {
      redisAvailable.set(false);
      log.error("Redis 상태 확인 중 예외 발생: {}", e.getMessage());
      return false;
    }
  }

  /**
   * 모든 소비자 시작 (Redis 연결 가능 시에만)
   */
  public void startConsumers() {
    if (!redisAvailable.get()) {
      log.warn("Redis 연결이 불가능하여 소비자를 시작할 수 없음");
      return;
    }

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
   * Redis 작업 실행 with 재시도 로직
   */
  private <T> T executeRedisOperation(String operationName, RedisOperation<T> operation) {
    for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
      try {
        T result = operation.execute();
        if (attempt > 1) {
          log.info("Redis {} 작업 재시도 성공 ({}번째 시도)", operationName, attempt);
          redisAvailable.set(true);
        }
        return result;
      } catch (RedisConnectionFailureException e) {
        redisAvailable.set(false);
        log.warn("Redis {} 작업 실패 ({}번째 시도): {}", operationName, attempt, e.getMessage());

        if (attempt == MAX_RETRY_ATTEMPTS) {
          log.error("Redis {} 작업 최종 실패 - 최대 재시도 횟수 초과", operationName);
          throw e;
        }

        try {
          Thread.sleep(RETRY_DELAY_MS * attempt);
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
          throw new RuntimeException("Redis 재시도 중 인터럽트 발생", ie);
        }
      }
    }
    return null; // 이 지점에 도달하지 않음
  }

  /**
   * Redis 작업을 위한 함수형 인터페이스
   */
  @FunctionalInterface
  private interface RedisOperation<T> {
    T execute();
  }

  /**
   * 기존 알림 시스템 처리 (레거시) - 연결 복원력 강화
   */
  public void processLegacyNotifications() {
    log.info("기존 알림 큐 소비자 스레드 시작");

    while (running.get()) {
      try {
        if (!redisAvailable.get()) {
          log.warn("Redis 연결 불가능 - 연결 상태 재확인");
          if (!checkRedisConnection()) {
            Thread.sleep(30000); // 30초 대기 후 재시도
            continue;
          }
        }

        Long queueSize = executeRedisOperation("queueSize",
            () -> redisObjectTemplate.opsForList().size(LEGACY_QUEUE_KEY));

        if (queueSize != null && queueSize > 0) {
          log.info("처리할 알림 {}건을 확인했습니다.", queueSize);
        }

        Object rawData = executeRedisOperation("leftPop",
            () -> redisObjectTemplate.opsForList().leftPop(LEGACY_QUEUE_KEY));

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
      } catch (RedisConnectionFailureException e) {
        redisAvailable.set(false);
        log.error("Redis 연결 실패로 인한 기존 알림 처리 중단", e);
        try {
          Thread.sleep(30000); // 30초 대기 후 재시도
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
          break;
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
   * Python AI 서버 알림 소비 메인 루프 - 연결 복원력 강화
   */
  private void consumePythonNotifications(String notificationType) {
    log.info("{} 알림 소비자 스레드 시작", notificationType);

    String queueKey = PYTHON_QUEUE_KEY_PREFIX + notificationType;
    String processingKey = PROCESSING_KEY_PREFIX + notificationType;

    while (running.get()) {
      try {
        if (!redisAvailable.get()) {
          log.warn("Redis 연결 불가능 - {} 알림 소비자 대기 중", notificationType);
          if (!checkRedisConnection()) {
            Thread.sleep(30000); // 30초 대기 후 재시도
            continue;
          }
        }

        ListOperations<String, Object> listOps = queueRedisTemplate.opsForList();

        // BLMOVE 명령어로 안전한 작업 이동 (원자적 연산)
        String taskId = (String) executeRedisOperation("rightPopAndLeftPush",
            () -> listOps.rightPopAndLeftPush(queueKey, processingKey, BLOCKING_TIMEOUT));

        if (taskId != null) {
          log.debug("Python 알림 작업 수신: {} ({})", taskId, notificationType);
          processPythonNotification(taskId, notificationType, processingKey);
        }
      } catch (RedisConnectionFailureException e) {
        redisAvailable.set(false);
        log.error("{} 알림 Redis 연결 실패", notificationType, e);
        try {
          Thread.sleep(30000); // 30초 대기 후 재시도
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
          break;
        }
      } catch (Exception e) {
        log.error("{} 알림 처리 중 오류", notificationType, e);
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
   * Python AI 서버 개별 알림 처리 - 연결 복원력 강화
   */
  private void processPythonNotification(String taskId, String notificationType, String processingKey) {
    String detailKey = DETAIL_KEY_PREFIX + taskId;

    try {
      // HGETALL로 알림 상세 정보 조회
      Map<Object, Object> details = executeRedisOperation("hgetall",
          () -> queueRedisTemplate.opsForHash().entries(detailKey));

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
      executeRedisOperation("delete", () -> {
        queueRedisTemplate.delete(detailKey);
        return null;
      });

      log.info("Python 알림 발송 성공: {} ({})", taskId, notificationType);

    } catch (RedisConnectionFailureException e) {
      log.error("Python 알림 Redis 연결 실패: {} ({})", taskId, notificationType, e);
      // Redis 연결 실패 시 처리 큐에 남겨둠 (재시도 대기)
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
   * 처리 큐에서 작업 제거 - 연결 복원력 강화
   */
  private void removeFromProcessingQueue(String processingKey, String taskId) {
    try {
      Long removed = executeRedisOperation("removeFromQueue",
          () -> queueRedisTemplate.opsForList().remove(processingKey, 1, taskId));

      if (removed != null && removed > 0) {
        log.debug("처리 큐에서 제거됨: {}", taskId);
      } else {
        log.warn("처리 큐에서 제거 실패: {}", taskId);
      }
    } catch (RedisConnectionFailureException e) {
      log.error("처리 큐 제거 중 Redis 연결 실패: {}", taskId, e);
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
   * 현재 큐 상태 조회 (모니터링용) - 연결 복원력 강화
   */
  public Map<String, Long> getQueueStatus() {
    try {
      if (!redisAvailable.get()) {
        log.warn("Redis 연결 불가능 - 큐 상태 조회 실패");
        return Map.of();
      }

      return executeRedisOperation("getQueueStatus", () -> Map.of(
          "legacy_queue", safeGetSize(LEGACY_QUEUE_KEY, redisObjectTemplate),
          "email_queue", safeGetSize(PYTHON_QUEUE_KEY_PREFIX + "EMAIL", queueRedisTemplate),
          "email_processing", safeGetSize(PROCESSING_KEY_PREFIX + "EMAIL", queueRedisTemplate),
          "sms_queue", safeGetSize(PYTHON_QUEUE_KEY_PREFIX + "SMS", queueRedisTemplate),
          "sms_processing", safeGetSize(PROCESSING_KEY_PREFIX + "SMS", queueRedisTemplate)));
    } catch (RedisConnectionFailureException e) {
      log.error("큐 상태 조회 중 Redis 연결 실패", e);
      return Map.of("error", -1L);
    }
  }

  /**
   * 안전한 큐 크기 조회
   */
  private Long safeGetSize(String key, RedisTemplate<String, Object> template) {
    try {
      Long size = template.opsForList().size(key);
      return size != null ? size : 0L;
    } catch (Exception e) {
      log.debug("큐 크기 조회 실패: {} - {}", key, e.getMessage());
      return 0L;
    }
  }
}