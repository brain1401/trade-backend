package com.hscoderadar.domain.notifications.repository;

import com.hscoderadar.domain.notifications.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    /**
     * 아직 발송되지 않은 모든 알림을 생성 시간 오름차순으로 조회합니다.
     * (알림 발송 배치 서비스가 가장 오래된 알림부터 처리하기 위함)
     *
     * @return 발송 대기 중인 알림 목록
     */
    List<Notification> findByIsSentIsFalseOrderByCreatedAtAsc();

    /**
     * 발송된 알림 중 특정 시간 이전에 생성된 오래된 데이터를 일괄 삭제합니다.
     * (데이터베이스 테이블 용량 관리를 위함)
     *
     * @param cutoffDate 삭제 기준 시간
     */
    @Modifying
    void deleteByCreatedAtBeforeAndIsSentIsTrue(LocalDateTime cutoffDate);
}