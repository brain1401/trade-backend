package com.tradegenie.platform.tradegenie_backend_api.repository;

import com.tradegenie.platform.tradegenie_backend_api.entity.ChangeDetectionLog;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ChangeDetectionLogRepository extends JpaRepository<ChangeDetectionLog, Long> {

  /**
   * 북마크별 최근 변동사항 조회
   */
  @EntityGraph(attributePaths = { "bookmark" })
  @Query("SELECT c FROM ChangeDetectionLog c WHERE c.bookmark.id = :bookmarkId " +
      "ORDER BY c.detectedAt DESC")
  List<ChangeDetectionLog> findByBookmarkIdOrderByDetectedAtDesc(@Param("bookmarkId") Long bookmarkId);

  /**
   * 특정 기간 내 변동사항 조회
   */
  @Query("SELECT c FROM ChangeDetectionLog c WHERE c.detectedAt >= :startDate " +
      "ORDER BY c.detectedAt DESC")
  List<ChangeDetectionLog> findRecentChanges(@Param("startDate") LocalDateTime startDate);

  /**
   * 변동 유형별 조회
   */
  @Query("SELECT c FROM ChangeDetectionLog c WHERE c.changeType = :changeType " +
      "ORDER BY c.detectedAt DESC")
  List<ChangeDetectionLog> findByChangeType(@Param("changeType") ChangeDetectionLog.ChangeType changeType);

  /**
   * 북마크와 변동 유형별 최근 변동사항 조회
   */
  @Query("SELECT c FROM ChangeDetectionLog c WHERE c.bookmark.id = :bookmarkId " +
      "AND c.changeType = :changeType ORDER BY c.detectedAt DESC")
  List<ChangeDetectionLog> findByBookmarkIdAndChangeType(
      @Param("bookmarkId") Long bookmarkId,
      @Param("changeType") ChangeDetectionLog.ChangeType changeType);

  /**
   * 일일 변동사항 통계
   */
  @Query("SELECT COUNT(c) FROM ChangeDetectionLog c WHERE " +
      "c.detectedAt >= :startOfDay AND c.detectedAt < :endOfDay")
  long countDailyChanges(@Param("startOfDay") LocalDateTime startOfDay,
      @Param("endOfDay") LocalDateTime endOfDay);

  /**
   * 사용자별 변동사항 조회 (북마크를 통해)
   */
  @EntityGraph(attributePaths = { "bookmark", "bookmark.user" })
  @Query("SELECT c FROM ChangeDetectionLog c WHERE c.bookmark.user.id = :userId ORDER BY c.detectedAt DESC")
  List<ChangeDetectionLog> findByUserId(@Param("userId") Long userId);

  /**
   * 변동 요약으로 검색
   */
  @Query("SELECT c FROM ChangeDetectionLog c WHERE c.changeSummary LIKE %:summary% ORDER BY c.detectedAt DESC")
  List<ChangeDetectionLog> findByChangeSummaryContaining(@Param("summary") String summary);

  /**
   * 특정 기간과 변동 유형별 조회
   */
  @Query("SELECT c FROM ChangeDetectionLog c WHERE c.detectedAt >= :startDate " +
      "AND c.detectedAt <= :endDate AND c.changeType = :changeType ORDER BY c.detectedAt DESC")
  List<ChangeDetectionLog> findByDateRangeAndChangeType(@Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate,
      @Param("changeType") ChangeDetectionLog.ChangeType changeType);

  /**
   * 소스 URL이 있는 변동사항 조회
   */
  @Query("SELECT c FROM ChangeDetectionLog c WHERE c.sourceUrls IS NOT NULL ORDER BY c.detectedAt DESC")
  List<ChangeDetectionLog> findWithSourceUrls();

  /**
   * 북마크별 변동 유형 통계
   */
  @Query("SELECT c.changeType, COUNT(c) FROM ChangeDetectionLog c WHERE c.bookmark.id = :bookmarkId GROUP BY c.changeType")
  List<Object[]> countByChangeTypeForBookmark(@Param("bookmarkId") Long bookmarkId);

  /**
   * 최근 N개 변동사항 조회
   */
  @Query("SELECT c FROM ChangeDetectionLog c ORDER BY c.detectedAt DESC LIMIT :limit")
  List<ChangeDetectionLog> findRecentChanges(@Param("limit") int limit);

  /**
   * 사용자별 최근 변동사항 조회 (제한된 개수)
   */
  @Query("SELECT c FROM ChangeDetectionLog c WHERE c.bookmark.user.id = :userId ORDER BY c.detectedAt DESC LIMIT :limit")
  List<ChangeDetectionLog> findRecentChangesByUserId(@Param("userId") Long userId, @Param("limit") int limit);

  /**
   * 북마크별 변동사항 삭제
   */
  @Modifying
  @Query("DELETE FROM ChangeDetectionLog c WHERE c.bookmark.id = :bookmarkId")
  void deleteByBookmarkId(@Param("bookmarkId") Long bookmarkId);

  /**
   * 특정 기간 이전 변동사항 삭제
   */
  @Modifying
  @Query("DELETE FROM ChangeDetectionLog c WHERE c.detectedAt < :cutoffDate")
  void deleteOldChanges(@Param("cutoffDate") LocalDateTime cutoffDate);

  /**
   * 사용자별 변동사항 삭제
   */
  @Modifying
  @Query("DELETE FROM ChangeDetectionLog c WHERE c.bookmark.user.id = :userId")
  void deleteByUserId(@Param("userId") Long userId);

  /**
   * 특정 변동 유형 삭제
   */
  @Modifying
  @Query("DELETE FROM ChangeDetectionLog c WHERE c.changeType = :changeType")
  void deleteByChangeType(@Param("changeType") ChangeDetectionLog.ChangeType changeType);

  /**
   * 변동 유형별 통계
   */
  @Query("SELECT c.changeType, COUNT(c) FROM ChangeDetectionLog c GROUP BY c.changeType")
  List<Object[]> countByChangeType();

  /**
   * 월별 변동사항 통계
   */
  @Query("SELECT YEAR(c.detectedAt), MONTH(c.detectedAt), COUNT(c) FROM ChangeDetectionLog c " +
      "WHERE c.detectedAt >= :startDate GROUP BY YEAR(c.detectedAt), MONTH(c.detectedAt) ORDER BY YEAR(c.detectedAt), MONTH(c.detectedAt)")
  List<Object[]> countMonthlyChanges(@Param("startDate") LocalDateTime startDate);

  /**
   * 사용자별 변동사항 수 조회
   */
  @Query("SELECT COUNT(c) FROM ChangeDetectionLog c WHERE c.bookmark.user.id = :userId")
  long countByUserId(@Param("userId") Long userId);

  /**
   * 특정 키워드가 포함된 변동사항 조회
   */
  @Query("SELECT c FROM ChangeDetectionLog c WHERE c.changeSummary LIKE %:keyword% ORDER BY c.detectedAt DESC")
  List<ChangeDetectionLog> findByKeyword(@Param("keyword") String keyword);

  /**
   * 북마크별 최신 변동사항 조회
   */
  @Query("SELECT c FROM ChangeDetectionLog c WHERE c.bookmark.id = :bookmarkId ORDER BY c.detectedAt DESC LIMIT 1")
  ChangeDetectionLog findLatestByBookmarkId(@Param("bookmarkId") Long bookmarkId);

  /**
   * 전체 변동사항 수 조회
   */
  @Query("SELECT COUNT(c) FROM ChangeDetectionLog c")
  long countAllChanges();

  /**
   * 변동 유형별 최근 변동사항 조회
   */
  @Query("SELECT c FROM ChangeDetectionLog c WHERE c.changeType = :changeType ORDER BY c.detectedAt DESC LIMIT :limit")
  List<ChangeDetectionLog> findRecentByChangeType(@Param("changeType") ChangeDetectionLog.ChangeType changeType,
      @Param("limit") int limit);
}