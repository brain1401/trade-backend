package com.hscoderadar.domain.news.repository;

import com.hscoderadar.domain.news.entity.News;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NewsRepository extends JpaRepository<News, Long> {

    /**
     * 만료 시간이 지난 뉴스를 조회합니다.
     * (주기적인 데이터 정리 배치 작업에서 사용)
     */
    List<News> findByExpiresAtBefore(LocalDateTime now);

    /**
     * 만료 시간이 지난 뉴스를 일괄 삭제합니다.
     */
    @Modifying
    void deleteByExpiresAtBefore(LocalDateTime now);

    /**
     * 특정 HS Code와 관련된 뉴스를 최신순으로 조회합니다. (페이징 처리)
     * @param hsCode   조회할 HS Code
     * @param pageable 페이징 정보
     * @return HS Code 관련 뉴스 목록
     */
    List<News> findByHsCodeOrderByPublishedAtDesc(String hsCode, Pageable pageable);
}