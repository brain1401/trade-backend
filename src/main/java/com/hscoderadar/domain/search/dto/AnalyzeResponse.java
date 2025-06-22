package com.hscoderadar.domain.search.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // 로그인 안했을 때 personalizedSuggestions 등이 null이면 응답에서 제외
public class AnalyzeResponse {
    private String intent;
    private double confidence;
    private String suggestedAction;
    private String[] extractedKeywords;
    private String nextStepUrl;
    private String[] personalizedSuggestions; // 로그인 시에만 제공될 수 있음
}