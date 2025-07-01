package com.hscoderadar.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hscoderadar.domain.notification.dto.NotificationRequest;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

// 모니터링 기능으로 알림 생성하는 로직 만들고 난 후 꼭 삭제 !!!
@Slf4j
@Component
@Profile("dev") // dev 프로파일에서만 동작하도록 설정
@RequiredArgsConstructor
public class DummyDataConfig {

    private final RedisTemplate<String, Object> redisObjectTemplate;
    private final ObjectMapper objectMapper;
    private static final String NOTIFICATION_QUEUE_KEY = "notification:queue";

    @PostConstruct
    public void init() {
        redisObjectTemplate.delete(NOTIFICATION_QUEUE_KEY);
        log.info("기존 알림 큐 데이터를 삭제했습니다.");

        try {
            
            addDummyNotification(4L, 1L, false, false,
                "[HS코더] HS Code 변경 알림", "북마크하신 '8517.12'(스마트폰)의 관세율이 8%에서 6%로 변경되었습니다.");

            
            addDummyNotification(4L, 7L, false, false,
                "[HS코더] 화물 추적 업데이트", "북마크하신 화물 'KRPU1234567890'의 통관이 완료되었습니다.");

        } catch (Exception e) {
            log.error("임시 알림 데이터를 생성하는 중 오류가 발생했습니다.", e);
        }
    }

    private void addDummyNotification(Long userId, Long bookmarkId, boolean smsEnabled, boolean emailEnabled, String title, String content) throws Exception {
        NotificationRequest request = new NotificationRequest(userId, bookmarkId, smsEnabled, emailEnabled, title, content);
        String jsonRequest = objectMapper.writeValueAsString(request);
        redisObjectTemplate.opsForList().rightPush(NOTIFICATION_QUEUE_KEY, jsonRequest);
        log.info("임시 알림 데이터 추가 완료: userId={}, bookmarkId={}", userId, bookmarkId);
    }
}