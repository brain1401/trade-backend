package com.hscoderadar.domain.tradenews.repository;

import com.hscoderadar.domain.tradenews.entity.TradeNews;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TradeNewsRepository extends JpaRepository<TradeNews, Long> {

    /**
     * 모든 뉴스를 발행일 기준 최신순 으로 정렬하여 조회
     */
    List<TradeNews> findAllByOrderByPublishedAtDesc();
}