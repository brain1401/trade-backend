package com.hscoderadar.domain.notification.repository;

import com.hscoderadar.domain.notification.entity.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 알림 발송 로그를 위한 Repository
 */
@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {
}