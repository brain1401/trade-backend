package com.tradegenie.platform.tradegenie_backend_api.repository;

import com.tradegenie.platform.tradegenie_backend_api.entity.PushNotification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PushNotificationRepository extends JpaRepository<PushNotification, Long> {

  /**
   * 사용자별 알림 조회 (읽지 않은 알림 우선)
   */
  @EntityGraph(attributePaths = { "user", "bookmark" })
  @Query("SELECT p FROM PushNotification p WHERE p.user.id = :userId " +
      "ORDER BY p.isRead ASC, p.createdAt DESC")
  List<PushNotification> findByUserIdOrderByReadStatusAndCreatedAt(@Param("userId") Long userId);

  /**
   * 읽지 않은 알림 조회
   */
  @Query("SELECT p FROM PushNotification p WHERE p.user.id = :userId AND p.isRead = false " +
      "ORDER BY p.createdAt DESC")
  List<PushNotification> findUnreadNotificationsByUserId(@Param("userId") Long userId);

  /**
   * 전송되지 않은 알림 조회
   */
  @Query("SELECT p FROM PushNotification p WHERE p.isSent = false " +
      "ORDER BY p.createdAt ASC")
  List<PushNotification> findUnsentNotifications();

  /**
   * 북마크별 알림 조회
   */
  @Query("SELECT p FROM PushNotification p WHERE p.bookmark.id = :bookmarkId " +
      "ORDER BY p.createdAt DESC")
  List<PushNotification> findByBookmarkId(@Param("bookmarkId") Long bookmarkId);

  /**
   * 알림 읽음 처리
   */
  @Modifying
  @Query("UPDATE PushNotification p SET p.isRead = true WHERE p.id = :notificationId")
  void markAsRead(@Param("notificationId") Long notificationId);

  /**
   * 알림 전송 완료 처리
   */
  @Modifying
  @Query("UPDATE PushNotification p SET p.isSent = true, p.sentAt = :sentAt WHERE p.id = :notificationId")
  void markAsSent(@Param("notificationId") Long notificationId, @Param("sentAt") LocalDateTime sentAt);

  /**
   * 읽지 않은 알림 수 조회
   */
  @Query("SELECT COUNT(p) FROM PushNotification p WHERE p.user.id = :userId AND p.isRead = false")
  long countUnreadNotificationsByUserId(@Param("userId") Long userId);

  /**
   * 전송 성공률 통계
   */
  @Query("SELECT COUNT(p) FROM PushNotification p WHERE p.isSent = true")
  long countSentNotifications();

  /**
   * 사용자별 모든 알림 읽음 처리
   */
  @Modifying
  @Query("UPDATE PushNotification p SET p.isRead = true WHERE p.user.id = :userId AND p.isRead = false")
  void markAllAsReadByUserId(@Param("userId") Long userId);

  /**
   * 북마크별 알림 삭제
   */
  @Modifying
  @Query("DELETE FROM PushNotification p WHERE p.bookmark.id = :bookmarkId")
  void deleteByBookmarkId(@Param("bookmarkId") Long bookmarkId);

  /**
   * 사용자별 알림 삭제
   */
  @Modifying
  @Query("DELETE FROM PushNotification p WHERE p.user.id = :userId")
  void deleteByUserId(@Param("userId") Long userId);

  /**
   * 읽은 알림 삭제
   */
  @Modifying
  @Query("DELETE FROM PushNotification p WHERE p.isRead = true")
  void deleteReadNotifications();

  /**
   * 특정 기간 이전 알림 삭제
   */
  @Modifying
  @Query("DELETE FROM PushNotification p WHERE p.createdAt < :cutoffDate")
  void deleteOldNotifications(@Param("cutoffDate") LocalDateTime cutoffDate);

  /**
   * 알림 제목으로 검색
   */
  @Query("SELECT p FROM PushNotification p WHERE p.title LIKE %:title% ORDER BY p.createdAt DESC")
  List<PushNotification> findByTitleContaining(@Param("title") String title);

  /**
   * 알림 내용으로 검색
   */
  @Query("SELECT p FROM PushNotification p WHERE p.content LIKE %:content% ORDER BY p.createdAt DESC")
  List<PushNotification> findByContentContaining(@Param("content") String content);

  /**
   * 특정 기간 알림 조회
   */
  @Query("SELECT p FROM PushNotification p WHERE p.createdAt >= :startDate AND p.createdAt <= :endDate ORDER BY p.createdAt DESC")
  List<PushNotification> findNotificationsBetween(@Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate);

  /**
   * 전송 실패 알림 조회
   */
  @Query("SELECT p FROM PushNotification p WHERE p.isSent = false AND p.createdAt < :cutoffTime ORDER BY p.createdAt ASC")
  List<PushNotification> findFailedNotifications(@Param("cutoffTime") LocalDateTime cutoffTime);

  /**
   * 사용자별 최근 알림 조회 (제한된 개수)
   */
  @Query("SELECT p FROM PushNotification p WHERE p.user.id = :userId ORDER BY p.createdAt DESC LIMIT :limit")
  List<PushNotification> findRecentNotificationsByUserId(@Param("userId") Long userId, @Param("limit") int limit);

  /**
   * 북마크별 최근 알림 조회 (제한된 개수)
   */
  @Query("SELECT p FROM PushNotification p WHERE p.bookmark.id = :bookmarkId ORDER BY p.createdAt DESC LIMIT :limit")
  List<PushNotification> findRecentNotificationsByBookmarkId(@Param("bookmarkId") Long bookmarkId,
      @Param("limit") int limit);

  /**
   * 일일 알림 통계
   */
  @Query("SELECT COUNT(p) FROM PushNotification p WHERE DATE(p.createdAt) = DATE(:date)")
  long countDailyNotifications(@Param("date") LocalDateTime date);

  /**
   * 전송률 통계
   */
  @Query("SELECT COUNT(p) FROM PushNotification p WHERE p.isSent = false")
  long countUnsentNotifications();

  /**
   * 사용자별 읽음률 통계
   */
  @Query("SELECT COUNT(p) FROM PushNotification p WHERE p.user.id = :userId AND p.isRead = true")
  long countReadNotificationsByUserId(@Param("userId") Long userId);

  /**
   * 벌크 전송 완료 처리
   */
  @Modifying
  @Query("UPDATE PushNotification p SET p.isSent = true, p.sentAt = :sentAt WHERE p.id IN :notificationIds")
  void bulkMarkAsSent(@Param("notificationIds") List<Long> notificationIds,
      @Param("sentAt") LocalDateTime sentAt);

  /**
   * 알림 우선순위별 조회 (읽지 않은 것 우선)
   */
  @Query("SELECT p FROM PushNotification p WHERE p.user.id = :userId ORDER BY " +
      "CASE WHEN p.isRead = false THEN 0 ELSE 1 END, p.createdAt DESC")
  List<PushNotification> findByUserIdWithPriority(@Param("userId") Long userId);

  /**
   * 사용자별 알림 통계 조회
   */
  @Query("SELECT COUNT(p) FROM PushNotification p WHERE p.user.id = :userId")
  long countTotalNotificationsByUserId(@Param("userId") Long userId);
}