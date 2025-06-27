package com.hscoderadar.domain.sms.repository;

import com.hscoderadar.domain.sms.entity.SmsLog;
import com.hscoderadar.domain.users.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

/**
 * v4.0 SMS 발송 로그 Repository
 */
@Repository
public interface SmsLogRepository extends JpaRepository<SmsLog, Long> {

    /**
     * 사용자별 SMS 발송 이력 조회 (페이징)
     */
    Page<SmsLog> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    /**
     * 사용자별 메시지 타입별 SMS 발송 이력 조회
     */
    Page<SmsLog> findByUserAndMessageTypeOrderByCreatedAtDesc(User user, SmsLog.MessageType messageType,
            Pageable pageable);

    /**
     * 사용자별 발송 상태별 SMS 발송 이력 조회
     */
    Page<SmsLog> findByUserAndStatusOrderByCreatedAtDesc(User user, SmsLog.SmsStatus status, Pageable pageable);

    /**
     * 사용자의 특정 기간 내 SMS 발송 개수 조회
     */
    long countByUserAndCreatedAtBetween(User user, LocalDateTime start, LocalDateTime end);

    /**
     * 사용자의 특정 기간 내 SMS 발송 비용 합계
     */
    @Query("SELECT COALESCE(SUM(s.costKrw), 0) FROM SmsLog s WHERE s.user = :user AND s.createdAt BETWEEN :start AND :end")
    Integer sumCostByUserAndCreatedAtBetween(@Param("user") User user, @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /**
     * 사용자의 당일 SMS 발송 개수 조회 (일일 한도 체크용)
     */
    @Query("SELECT COUNT(s) FROM SmsLog s WHERE s.user = :user AND s.createdAt >= :startOfDay AND s.createdAt < :startOfNextDay")
    long countTodayByUser(@Param("user") User user, @Param("startOfDay") LocalDateTime startOfDay,
            @Param("startOfNextDay") LocalDateTime startOfNextDay);

    /**
     * 사용자의 전체 발송 성공률 계산
     */
    @Query("SELECT " +
            "CAST(COUNT(CASE WHEN s.status IN ('SENT', 'DELIVERED') THEN 1 END) AS double) / COUNT(*) * 100 " +
            "FROM SmsLog s WHERE s.user = :user")
    Double calculateDeliveryRateByUser(@Param("user") User user);

    /**
     * 외부 메시지 ID로 조회 (전달 확인 콜백용)
     */
    SmsLog findByExternalMessageId(String externalMessageId);
}