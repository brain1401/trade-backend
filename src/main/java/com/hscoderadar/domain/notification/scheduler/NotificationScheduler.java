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

  /**
   * 주기적으로 Redis의 알림 큐를 확인하고 모든 알림을 발송하는 스케줄러
   */
  @Scheduled(cron = "20 16 19 * * *")
  public void runNotificationJob() {
    log.info("레디스 알림 발송 스케줄러를 시작합니다.");
    notificationSendingService.sendAllPendingNotifications();
    log.info("레디스 알림 발송 스케줄러를 종료합니다.");
  }
}