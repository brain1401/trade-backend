package com.hscoderadar.domain.search.service;

import com.hscoderadar.domain.search.dto.AnalyzeRequest;
import com.hscoderadar.domain.search.dto.AnalyzeResponse;

/**
 * 검색 관련 비즈니스 로직을 처리하는 서비스 인터페이스.
 */
public interface SearchService {

    /**
     * 사용자의 자연어 검색 질의를 받아 의도를 분석함.
     *
     * @param request 사용자가 입력한 검색어(query)를 담은 DTO
     * @return 분석된 의도(intent)와 다음 행동 제안 등을 담은 AnalyzeResponse DTO
     */
    AnalyzeResponse analyze(AnalyzeRequest request);
}