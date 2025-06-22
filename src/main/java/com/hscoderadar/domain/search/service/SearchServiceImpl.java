package com.hscoderadar.domain.search.service;

import com.hscoderadar.domain.search.dto.AnalyzeRequest;
import com.hscoderadar.domain.search.dto.AnalyzeResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchServiceImpl implements SearchService {

    @Override
    public AnalyzeResponse analyze(AnalyzeRequest request) {
        String query = request.getQuery();

        // 1. 화물관리번호 형식인지 확인 (15~19자리의 숫자)
        if (isCargoNumber(query)) {
            log.info("의도 분석 결과: 화물 추적(CARGO_TRACKING)");
            return new AnalyzeResponse(
                "CARGO_TRACKING",
                0.99, // 규칙 기반이므로 높은 신뢰도 부여
                "CARGO_TRACKING",
                new String[]{query},
                "/api/search/cargo/" + query, // 다음 단계 URL을 바로 생성
                null
            );
        }

        // 2. 그 외에는 HS Code 분석으로 판단
        log.info("의도 분석 결과: HS Code 분석(HS_CODE_ANALYSIS)");
        return new AnalyzeResponse(
            "HS_CODE_ANALYSIS",
            0.85, // AI가 아니므로 적당한 신뢰도 부여
            "HS_CODE_ANALYSIS",
            extractKeywords(query), // 간단한 키워드 추출
            "/api/search/hscode/start",
            null
        );
    }

    /**
     * 입력된 문자열이 화물관리번호 형식인지 판별함.
     * @param query 검사할 문자열
     * @return 화물관리번호 형식이면 true, 아니면 false
     */
    private boolean isCargoNumber(String query) {
        if (query == null || query.trim().isEmpty()) {
            return false;
        }
        // 정규식을 사용하여 15자리에서 19자리 사이의 숫자로만 구성되었는지 확인
        return query.matches("^\\d{15,19}$");
    }

    /**
     * 간단한 키워드 추출 로직 (공백으로 분리).
     * @param query 원본 검색어
     * @return 공백으로 분리된 키워드 배열
     */
    private String[] extractKeywords(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new String[0];
        }
        return query.trim().split("\\s+");
    }
}