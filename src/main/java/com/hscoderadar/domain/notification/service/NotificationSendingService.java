package com.hscoderadar.domain.notification.service;

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
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 통합 알림 발송 서비스
 */
@Slf4j
@Service
public class NotificationSendingService {

  // Redis 키 상수
  private static final String UNIFIED_QUEUE_KEY = "daily_notification:queue:";
  private static final String DETAIL_KEY_PREFIX = "daily_notification:detail:";

  private final RedisTemplate<String, Object> queueRedisTemplate;
  private final UserRepository userRepository;
  private final SmsService smsService;
  private final EmailService emailService;
  private final NotificationLogRepository notificationLogRepository;

  public NotificationSendingService(
      @Qualifier("queueRedisTemplate") RedisTemplate<String, Object> queueRedisTemplate,
      UserRepository userRepository,
      SmsService smsService,
      EmailService emailService,
      NotificationLogRepository notificationLogRepository) {
    this.queueRedisTemplate = queueRedisTemplate;
    this.userRepository = userRepository;
    this.smsService = smsService;
    this.emailService = emailService;
    this.notificationLogRepository = notificationLogRepository;
  }

  /**
   * Redis 큐에 대기중인 모든 알림을 발송
   */
  public void sendAllPendingNotifications() {
    processUnifiedQueue();
  }

  /**
   * 통합된 단일 알림 큐를 처리
   */
  private void processUnifiedQueue() {
    ListOperations<String, Object> listOps = queueRedisTemplate.opsForList();

    Long queueSize = listOps.size(UNIFIED_QUEUE_KEY);
    if (queueSize == null || queueSize == 0) {
      log.info("처리할 알림이 큐에 없습니다.");
      return;
    }

    log.info("총 {}건의 알림을 처리합니다.", queueSize);

    // 큐에 있는 모든 알림을 처리
    for (long i = 0; i < queueSize; i++) {
      String taskId = (String) listOps.rightPop(UNIFIED_QUEUE_KEY);
      if (taskId != null) {
        try {
          log.debug("알림 작업 수신: {}", taskId);
          processSingleNotification(taskId);
        } catch (Exception e) {
          log.error("알림 처리 중 taskId={}에 대한 오류 발생", taskId, e);
        }
      }
    }
  }

  /**
   * 개별 알림 작업을 처리하고 Redis에서 관련 데이터를 삭제
   * 
   * @param taskId Redis에 저장된 알림의 UUID
   */
  @Transactional
  public void processSingleNotification(String taskId) {
    String detailKey = DETAIL_KEY_PREFIX + taskId;

    try {
      Map<Object, Object> details = queueRedisTemplate.opsForHash().entries(detailKey);

      if (details.isEmpty()) {
        log.warn("알림 상세 정보를 찾을 수 없음 (이미 처리되었거나 삭제됨): taskId={}", taskId);
        return;
      }

      NotificationRequest request = convertMapToRequest(taskId, details);
      sendNotificationForUser(request);
      queueRedisTemplate.delete(detailKey);

      log.info("알림 발송 및 데이터 삭제 성공: taskId={}, type={}", taskId, request.getEffectiveTitle());

    } catch (Exception e) {
      log.error("알림 처리 중 예외 발생: taskId={}. 에러: {}", taskId, e.getMessage(), e);
    }
  }

  /**
   * Redis의 Hash 데이터를 NotificationRequest DTO로 변환
   */
  private NotificationRequest convertMapToRequest(String taskId, Map<Object, Object> details) {
    Long userId = Long.parseLong((String) details.get("user_id"));
    String message = (String) details.get("message");
    String type = (String) details.get("type"); // "EMAIL" or "SMS"

    String email = null;
    String phoneNumber = null;

    if ("EMAIL".equals(type)) {
      email = userRepository.findById(userId).map(User::getEmail).orElse(null);
    } else if ("SMS".equals(type)) {
      phoneNumber = userRepository.findById(userId).map(User::getPhoneNumber).orElse(null);
    }

    return NotificationRequest.forPythonServer(
        taskId,
        userId,
        email,
        phoneNumber,
        message,
        message);
  }

  /**
   * 사용자 설정을 확인하여 최종적으로 알림을 발송
   */
  private void sendNotificationForUser(NotificationRequest request) {
    User user = userRepository.findById(request.userId())
        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + request.userId()));

    UserSettings globalSettings = user.getUserSettings();
    if (globalSettings == null) {
      log.warn("사용자(id:{})의 전역 알림 설정을 찾을 수 없어 알림을 보내지 않습니다.", user.getId());
      saveLog(user, NotificationType.EMAIL, user.getEmail(), request, NotificationStatus.FAILED, "사용자 설정 없음");
      return;
    }

    // SMS 알림 발송
    if (request.phoneNumber() != null && globalSettings.isSmsNotificationEnabled() && user.isPhoneVerified()) {
      try {
        smsService.sendMessage(request.phoneNumber(), request.getEffectiveContent());
        saveLog(user, NotificationType.SMS, request.phoneNumber(), request, NotificationStatus.SENT, null);
        log.info("SMS 발송 성공: userId={}", user.getId());
      } catch (Exception e) {
        saveLog(user, NotificationType.SMS, request.phoneNumber(), request, NotificationStatus.FAILED, e.getMessage());
        log.error("SMS 발송 실패: userId={}", user.getId(), e);
      }
    }

    // 이메일 알림 발송
    if (request.email() != null && globalSettings.isEmailNotificationEnabled()) {
      try {
        emailService.sendEmail(request.email(), request.getEffectiveTitle(), request.getEffectiveContent());
        saveLog(user, NotificationType.EMAIL, request.email(), request, NotificationStatus.SENT, null);
        log.info("이메일 발송 성공: userId={}", user.getId());
      } catch (Exception e) {
        saveLog(user, NotificationType.EMAIL, request.email(), request, NotificationStatus.FAILED, e.getMessage());
        log.error("이메일 발송 실패: userId={}", user.getId(), e);
      }
    }
  }

  /**
   * 알림 발송 로그를 DB에 저장
   */
  private void saveLog(User user, NotificationType type, String recipient, NotificationRequest request,
      NotificationStatus status, String errorMessage) {
    NotificationLog log = NotificationLog.builder()
        .user(user)
        .notificationId(request.taskId())
        .notificationType(type)
        .messageType(MessageType.DAILY_NOTIFICATION.name())
        .recipient(recipient)
        .title(request.getEffectiveTitle())
        .content(request.getEffectiveContent())
        .build();

    log.updateStatus(status, LocalDateTime.now(), errorMessage);
    notificationLogRepository.save(log);
  }
}