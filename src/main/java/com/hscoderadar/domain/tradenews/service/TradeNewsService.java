package com.hscoderadar.domain.tradenews.service;

import com.hscoderadar.domain.tradenews.dto.response.TradeNewsResponse;
import com.hscoderadar.domain.tradenews.entity.TradeNews;
import com.hscoderadar.domain.tradenews.repository.TradeNewsRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TradeNewsService {

  private final TradeNewsRepository tradeNewsRepository;

  /**
     * 뉴스를 최신순으로 정렬하고, offset과 limit을 이용해 페이지네이션하여 조회
     * @param offset 데이터 조회 시작 위치
     * @param limit 조회할 데이터 개수
     * @return 페이지네이션이 적용된 뉴스 리스트
     */
    public List<TradeNewsResponse> findNewsWithPagination(int offset, int limit) {
        
        // PageRequest.of(페이지번호, 페이지크기, 정렬)
        Pageable pageable = PageRequest.of(
            offset / limit,               
            limit,                        
            Sort.by("publishedAt").descending()
        );

        Page<TradeNews> newsPage = tradeNewsRepository.findAll(pageable);

        return newsPage.getContent().stream()
                .map(TradeNewsResponse::from)
                .toList();
    }
}