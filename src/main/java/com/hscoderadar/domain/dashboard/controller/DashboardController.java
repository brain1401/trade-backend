package com.hscoderadar.domain.dashboard.controller;

import com.hscoderadar.common.response.ApiResponse;
import com.hscoderadar.common.response.ApiResponseMessage;
import com.hscoderadar.config.oauth.PrincipalDetails;
import com.hscoderadar.domain.dashboard.dto.DashboardDto;
import com.hscoderadar.domain.dashboard.service.DashboardService;
import com.hscoderadar.domain.feed.dto.FeedDto;
import com.hscoderadar.domain.feed.service.FeedService;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;

import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final FeedService feedService;

    @GetMapping("/summary")
    @ApiResponseMessage("대시보드 요약 정보가 성공적으로 조회되었습니다.")
    public DashboardDto.DashboardSummaryResponse getDashboardSummary(@AuthenticationPrincipal PrincipalDetails principalDetails) {
        return dashboardService.getDashboardSummary(principalDetails.getUser());
    }

    @GetMapping("/feeds")
    @ApiResponseMessage("업데이트 피드가 성공적으로 조회되었습니다.")
    public Page<FeedDto.FeedResponse> getFeeds(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @Parameter(hidden = true) Pageable pageable) {
        return feedService.getFeeds(principalDetails.getUser(), pageable);
    }

    @PutMapping("/feeds/{feedId}/read")
    @ApiResponseMessage("피드를 읽음 처리했습니다.")
    public ResponseEntity<Void> markFeedAsRead(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @PathVariable Long feedId) {
        feedService.markFeedAsRead(principalDetails.getUser(), feedId);
        return ResponseEntity.ok().build();
    }
    
    /**
     * 모든 피드를 읽음 처리하고, 처리된 개수를 ApiResponse에 담아 ResponseEntity로 직접 반환
     * 이를 통해 ResponseWrapperAdvice를 우회하고 MessageConverter 충돌 문제를 근본적으로 해결
     */
    @PutMapping("/feeds/read-all")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> markAllFeedsAsRead(@AuthenticationPrincipal PrincipalDetails principalDetails) {
        int count = feedService.markAllFeedsAsRead(principalDetails.getUser());
        Map<String, Integer> responseData = Map.of("processedCount", count);
        
        // ApiResponse를 직접 생성
        ApiResponse<Map<String, Integer>> apiResponse = ApiResponse.success("모든 피드를 읽음 처리했습니다.", responseData);
        
        // 최종 응답 형태인 ResponseEntity로 감싸서 반환
        return ResponseEntity.ok(apiResponse);
    }
}