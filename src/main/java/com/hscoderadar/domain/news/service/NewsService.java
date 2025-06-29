package com.hscoderadar.domain.news.service;

import com.hscoderadar.domain.news.dto.NewsDto;
import com.hscoderadar.domain.tradenews.entity.TradeNewsCache;
import com.hscoderadar.domain.tradenews.repository.TradeNewsCacheRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NewsService {

    private final TradeNewsCacheRepository tradeNewsCacheRepository;

    public List<NewsDto> getLatestNews() {
        
        List<TradeNewsCache> newsList = tradeNewsCacheRepository.findByIsActiveTrueAndExpiresAtAfterOrderByPublishedAtDesc(LocalDateTime.now());
        
        return newsList.stream()
                .map(NewsDto::from)
                .collect(Collectors.toList());
    }

}