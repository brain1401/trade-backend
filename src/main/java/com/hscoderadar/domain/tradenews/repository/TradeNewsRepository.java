package com.hscoderadar.domain.tradenews.repository;

import com.hscoderadar.domain.tradenews.entity.TradeNews;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TradeNewsRepository extends JpaRepository<TradeNews, Long> {

    // 활성화 상태이고 만료되지 않은 최신 뉴스를 가져오는 쿼리
    List<TradeNews> findByIsActiveTrueAndExpiresAtAfterOrderByPublishedAtDesc(LocalDateTime now);
}