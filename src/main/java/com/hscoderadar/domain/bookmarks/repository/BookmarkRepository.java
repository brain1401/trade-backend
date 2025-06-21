package com.hscoderadar.domain.bookmarks.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.hscoderadar.domain.bookmarks.entity.Bookmark;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

  /**
   * 사용자별 활성 북마크 조회 (N+1 문제 방지)
   */
  @EntityGraph(attributePaths = { "user" })
  @Query("SELECT b FROM Bookmark b WHERE b.user.id = :userId AND b.isActive = true ORDER BY b.createdAt DESC")
  List<Bookmark> findActiveBookmarksByUserId(@Param("userId") Long userId);

  /**
   * 사용자별 모든 북마크 조회
   */
  @EntityGraph(attributePaths = { "user" })
  @Query("SELECT b FROM Bookmark b WHERE b.user.id = :userId ORDER BY b.createdAt DESC")
  List<Bookmark> findAllBookmarksByUserId(@Param("userId") Long userId);

  /**
   * HS Code로 북마크 조회
   */
  @Query("SELECT b FROM Bookmark b WHERE b.hsCode = :hsCode AND b.isActive = true")
  List<Bookmark> findActiveBookmarksByHsCode(@Param("hsCode") String hsCode);

  /**
   * 모니터링이 필요한 북마크 조회 (마지막 체크 시간 기준)
   */
  @Query("SELECT b FROM Bookmark b WHERE b.isActive = true AND " +
      "(b.lastCheckedAt IS NULL OR b.lastCheckedAt < :cutoffTime)")
  List<Bookmark> findBookmarksNeedingCheck(@Param("cutoffTime") LocalDateTime cutoffTime);

  /**
   * 사용자와 HS Code로 북마크 조회
   */
  @Query("SELECT b FROM Bookmark b WHERE b.user.id = :userId AND b.hsCode = :hsCode")
  Optional<Bookmark> findByUserIdAndHsCode(@Param("userId") Long userId, @Param("hsCode") String hsCode);

  /**
   * 활성 북마크 수 조회
   */
  @Query("SELECT COUNT(b) FROM Bookmark b WHERE b.isActive = true")
  long countActiveBookmarks();

  /**
   * 북마크 비활성화 (소프트 삭제)
   */
  @Modifying
  @Query("UPDATE Bookmark b SET b.isActive = false, b.updatedAt = CURRENT_TIMESTAMP WHERE b.id = :bookmarkId")
  void deactivateBookmark(@Param("bookmarkId") Long bookmarkId);

  /**
   * 북마크 활성화
   */
  @Modifying
  @Query("UPDATE Bookmark b SET b.isActive = true, b.updatedAt = CURRENT_TIMESTAMP WHERE b.id = :bookmarkId")
  void activateBookmark(@Param("bookmarkId") Long bookmarkId);

  /**
   * 마지막 체크 시간 업데이트
   */
  @Modifying
  @Query("UPDATE Bookmark b SET b.lastCheckedAt = :checkTime, b.updatedAt = CURRENT_TIMESTAMP WHERE b.id = :bookmarkId")
  void updateLastCheckedAt(@Param("bookmarkId") Long bookmarkId, @Param("checkTime") LocalDateTime checkTime);

  /**
   * 사용자별 북마크 삭제 (하드 삭제)
   */
  @Modifying
  @Query("DELETE FROM Bookmark b WHERE b.user.id = :userId")
  void deleteAllByUserId(@Param("userId") Long userId);

  /**
   * 특정 HS Code 북마크 삭제
   */
  @Modifying
  @Query("DELETE FROM Bookmark b WHERE b.hsCode = :hsCode")
  void deleteAllByHsCode(@Param("hsCode") String hsCode);

  /**
   * 사용자별 비활성 북마크 삭제
   */
  @Modifying
  @Query("DELETE FROM Bookmark b WHERE b.user.id = :userId AND b.isActive = false")
  void deleteInactiveBookmarksByUserId(@Param("userId") Long userId);

  /**
   * 모니터링 키워드 업데이트
   */
  @Modifying
  @Query("UPDATE Bookmark b SET b.monitoringKeywords = :keywords, b.updatedAt = CURRENT_TIMESTAMP WHERE b.id = :bookmarkId")
  void updateMonitoringKeywords(@Param("bookmarkId") Long bookmarkId, @Param("keywords") List<String> keywords);

  /**
   * 제품명 업데이트
   */
  @Modifying
  @Query("UPDATE Bookmark b SET b.productName = :productName, b.updatedAt = CURRENT_TIMESTAMP WHERE b.id = :bookmarkId")
  void updateProductName(@Param("bookmarkId") Long bookmarkId, @Param("productName") String productName);

  /**
   * 사용자별 활성 북마크 존재 여부 확인
   */
  @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM Bookmark b WHERE b.user.id = :userId AND b.isActive = true")
  boolean existsActiveBookmarkByUserId(@Param("userId") Long userId);

  /**
   * 사용자와 HS Code로 활성 북마크 존재 여부 확인
   */
  @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM Bookmark b WHERE b.user.id = :userId AND b.hsCode = :hsCode AND b.isActive = true")
  boolean existsActiveBookmarkByUserIdAndHsCode(@Param("userId") Long userId, @Param("hsCode") String hsCode);

  /**
   * 사용자별 북마크 수 조회
   */
  @Query("SELECT COUNT(b) FROM Bookmark b WHERE b.user.id = :userId AND b.isActive = true")
  long countActiveBookmarksByUserId(@Param("userId") Long userId);

  /**
   * 특정 기간 동안 체크되지 않은 북마크 조회
   */
  @Query("SELECT b FROM Bookmark b WHERE b.isActive = true AND " +
      "b.lastCheckedAt < :cutoffTime ORDER BY b.lastCheckedAt ASC")
  List<Bookmark> findStaleBookmarks(@Param("cutoffTime") LocalDateTime cutoffTime);

  /**
   * 벌크 비활성화 - 특정 사용자의 모든 북마크
   */
  @Modifying
  @Query("UPDATE Bookmark b SET b.isActive = false, b.updatedAt = CURRENT_TIMESTAMP WHERE b.user.id = :userId")
  void deactivateAllBookmarksByUserId(@Param("userId") Long userId);

  /**
   * 벌크 활성화 - 특정 사용자의 모든 북마크
   */
  @Modifying
  @Query("UPDATE Bookmark b SET b.isActive = true, b.updatedAt = CURRENT_TIMESTAMP WHERE b.user.id = :userId")
  void activateAllBookmarksByUserId(@Param("userId") Long userId);
}