package com.hscoderadar.domain.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hscoderadar.domain.notification.dto.NotificationRequest;
import com.hscoderadar.domain.notification.entity.NotificationLog;
import com.hscoderadar.domain.notification.entity.NotificationLog.MessageType;
import com.hscoderadar.domain.notification.entity.NotificationLog.NotificationStatus;
import com.hscoderadar.domain.notification.entity.NotificationLog.NotificationType;
import com.hscoderadar.domain.notification.repository.NotificationLogRepository;
import com.hscoderadar.domain.user.entity.User;
import com.hscoderadar.domain.user.entity.UserSettings;
import com.hscoderadar.domain.user.repository.UserRepository;
import com.hscoderadar.domain.sms.service.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationSendingService {

    private static final String NOTIFICATION_QUEUE_KEY = "notification:queue";
    private final RedisTemplate<String, Object> redisObjectTemplate;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final SmsService smsService;
    private final EmailService emailService;
    private final NotificationLogRepository notificationLogRepository;

    public void processNotifications() {
        Long queueSize = redisObjectTemplate.opsForList().size(NOTIFICATION_QUEUE_KEY);
        if (queueSize != null && queueSize > 0) {
            log.info("처리할 알림 {}건을 확인했습니다.", queueSize);
        }

        while (true) {
            Object rawData = redisObjectTemplate.opsForList().leftPop(NOTIFICATION_QUEUE_KEY);
            if (rawData == null) {
                break;
            }

            try {
                NotificationRequest request = objectMapper.readValue((String) rawData, NotificationRequest.class);
                log.info("알림 처리 시작: userId={}, bookmarkId={}", request.getUserId(), request.getBookmarkId());
                sendNotificationForUser(request);
            } catch (JsonProcessingException e) {
                log.error("알림 데이터를 파싱하는데 실패했습니다: {}", rawData, e);
            } catch (Exception e) {
                log.error("알림 처리 중 알 수 없는 오류가 발생했습니다.", e);
            }
        }
    }

    @Transactional
    public void sendNotificationForUser(NotificationRequest request) {
        User user = userRepository.findById(request.getUserId())
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + request.getUserId()));

        UserSettings globalSettings = user.getUserSettings();
        if (globalSettings == null) {
            log.warn("사용자(id:{})의 전역 알림 설정을 찾을 수 없어 알림을 보내지 않습니다.", user.getId());
            return;
        }

        // SMS 알림 발송: 전역 설정과 북마크 개별 설정 모두 true인지 확인
        if (globalSettings.isSmsNotificationEnabled() && request.isSmsEnabled() && user.isPhoneVerified()) {
            try {
                String messageContent = request.getTitle() + "\n" + request.getContent();
                smsService.sendMessage(user.getPhoneNumber(), messageContent);
                saveLog(user, NotificationType.SMS, user.getPhoneNumber(), request, NotificationStatus.SENT, null);
                log.info("SMS 발송 성공: userId={}", user.getId());
            } catch (Exception e) {
                saveLog(user, NotificationType.SMS, user.getPhoneNumber(), request, NotificationStatus.FAILED, e.getMessage());
            }
        }

        // 이메일 알림 발송: 전역 설정과 북마크 개별 설정 모두 true인지 확인
        if (globalSettings.isEmailNotificationEnabled() && request.isEmailEnabled()) {
            try {
                emailService.sendEmail(user.getEmail(), request.getTitle(), request.getContent());
                saveLog(user, NotificationType.EMAIL, user.getEmail(), request, NotificationStatus.SENT, null);
                log.info("이메일 발송 성공: userId={}", user.getId());
            } catch (Exception e) {
                saveLog(user, NotificationType.EMAIL, user.getEmail(), request, NotificationStatus.FAILED, e.getMessage());
            }
        }
    }

    private void saveLog(User user, NotificationType type, String recipient, NotificationRequest request, NotificationStatus status, String errorMessage) {
        NotificationLog log = NotificationLog.builder()
            .user(user)
            .notificationType(type)
            .messageType(MessageType.DAILY_NOTIFICATION)
            .recipient(recipient)
            .title(request.getTitle())
            .content(request.getContent())
            .build();
        
        log.updateStatus(status, null, errorMessage);
        notificationLogRepository.save(log);
    }
}