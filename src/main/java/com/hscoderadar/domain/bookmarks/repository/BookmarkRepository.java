package com.hscoderadar.domain.bookmarks.repository;

import com.hscoderadar.domain.bookmarks.entity.Bookmark;
import com.hscoderadar.domain.bookmarks.entity.Bookmark.BookmarkType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    /**
     * 사용자별 활성 북마크 조회
     */
    @EntityGraph(attributePaths = {"user"})
    List<Bookmark> findByUserIdAndMonitoringEnabledIsTrueOrderByCreatedAtDesc(Long userId);

    /**
     * 사용자의 모든 북마크 조회
     */
    @EntityGraph(attributePaths = {"user"})
    List<Bookmark> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * 특정 값과 타입으로 활성 북마크들 조회
     */
    List<Bookmark> findByTargetValueAndTypeAndMonitoringEnabledIsTrue(String targetValue, BookmarkType type);

    /**
     *  모니터링 배치 작업을 위한 북마크들 조회
     */
    List<Bookmark> findAllByMonitoringEnabledIsTrue();

    /**
     * 사용자와 특정 값/타입으로 북마크 조회
     */
    Optional<Bookmark> findByUserIdAndTargetValueAndType(Long userId, String targetValue, BookmarkType type);

    /**
     * 모니터링 상태 업데이트
     */
    @Modifying
    @Query("UPDATE Bookmark b SET b.monitoringEnabled = :status, b.updatedAt = CURRENT_TIMESTAMP WHERE b.id = :bookmarkId")
    void updateMonitoringStatus(@Param("bookmarkId") Long bookmarkId, @Param("status") boolean status);

    /**
     * 사용자별 북마크 삭제
     */
    @Modifying
    @Query("DELETE FROM Bookmark b WHERE b.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);

}