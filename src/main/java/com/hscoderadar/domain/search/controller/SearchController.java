package com.hscoderadar.domain.search.controller;

import com.hscoderadar.common.response.ApiResponseMessage;
import com.hscoderadar.domain.search.dto.AnalyzeRequest;
import com.hscoderadar.domain.search.dto.AnalyzeResponse;
import com.hscoderadar.domain.search.service.SearchService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
@Slf4j
public class SearchController {

    private final SearchService searchService;

    /**
     * 사용자의 자연어 검색 질의를 받아 의도를 분석
     * (관련 유스케이스: TG_UC_006, 관련 기능 요구사항: FR-AD-003)
     *
     * @param request 사용자가 입력한 검색어(query)를 담은 DTO
     * @return 분석된 의도(intent)와 다음 행동 제안 등을 담은 DTO
     */
    @PostMapping("/analyze")
    @ApiResponseMessage("검색 의도 분석 완료")
    public AnalyzeResponse analyze(@RequestBody AnalyzeRequest request) {
        log.info("지능형 통합 검색 요청: query='{}'", request.getQuery());

        
        return searchService.analyze(request);
    }
}