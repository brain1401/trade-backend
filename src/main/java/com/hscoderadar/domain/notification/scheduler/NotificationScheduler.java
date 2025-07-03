package com.hscoderadar.domain.notification.scheduler;

import com.hscoderadar.domain.notification.service.NotificationSendingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationScheduler {

  private final NotificationSendingService notificationSendingService;

  // 매 1분마다 실행 (테스트용)
  // 실제 운영 환경에서는 '0 0 9 * * *' (매일 오전 9시) 와 같이 설정
  @Scheduled(cron = "0 0 0 * * 1")
  public void runNotificationJob() {
    log.info("알림 발송 스케줄러를 시작합니다.");
    // notificationSendingService.processNotifications(); - 스케줄러 로직 비활성화 해뒀음.
    log.info("알림 발송 스케줄러를 종료합니다.");
  }
}