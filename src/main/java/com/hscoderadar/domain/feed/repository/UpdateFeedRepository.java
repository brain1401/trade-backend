package com.hscoderadar.domain.feed.repository;

import com.hscoderadar.domain.feed.entity.UpdateFeed;
import com.hscoderadar.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UpdateFeedRepository extends JpaRepository<UpdateFeed, Long> {

    /**
     * 특정 사용자의 모든 피드를 페이징하여 최신순으로 조회
     */
    Page<UpdateFeed> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    /**
     * 특정 사용자의 읽지 않은 피드 목록을 조회
     */
    List<UpdateFeed> findByUserAndIsReadFalseOrderByCreatedAtDesc(User user);
    
    /**
     * 사용자의 모든 피드를 읽음 처리
     */
    @Modifying
    @Query("UPDATE UpdateFeed f SET f.isRead = true WHERE f.user = :user AND f.isRead = false")
    int markAllAsReadForUser(@Param("user") User user);

     /**
     * ID와 사용자 ID로 피드를 조회하여 소유권을 확인하는 메서드
     */
    Optional<UpdateFeed> findByIdAndUser(Long id, User user);

    /**
     * 특정 피드를 읽음 상태로 변경하는 JPQL 쿼리
     * isRead 필드만 정확히 업데이트하여 불필요한 데이터 전송 및 타입 충돌 문제를 해결
     */
    @Modifying
    @Query("UPDATE UpdateFeed f SET f.isRead = true WHERE f.id = :feedId")
    void markAsReadByFeedId(@Param("feedId") Long feedId);
}