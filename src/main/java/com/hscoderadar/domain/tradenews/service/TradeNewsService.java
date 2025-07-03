package com.hscoderadar.domain.tradenews.service;

import com.hscoderadar.domain.tradenews.dto.response.TradeNewsResponse;
import com.hscoderadar.domain.tradenews.entity.TradeNews;
import com.hscoderadar.domain.tradenews.repository.TradeNewsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TradeNewsService {
  private final TradeNewsRepository tradeNewsCacheRepository;

  public List<TradeNewsResponse> getLatestNews() {
    List<TradeNews> newsList = tradeNewsCacheRepository.findByIsActiveTrueAndExpiresAtAfterOrderByPublishedAtDesc(
        LocalDateTime.now());

    return newsList.stream()
        .map(TradeNewsResponse::from)
        .toList();
  }
}