package com.hscoderadar.domain.feeds.repository;

import com.hscoderadar.domain.feeds.entity.UpdateFeed;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UpdateFeedRepository extends JpaRepository<UpdateFeed, Long> {

    /**
     * 사용자별 피드 목록 조회
     */
    List<UpdateFeed> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    /**
     * 읽지 않은 피드 개수 조회
     */
    long countByUserIdAndIsReadIsFalse(Long userId);
}