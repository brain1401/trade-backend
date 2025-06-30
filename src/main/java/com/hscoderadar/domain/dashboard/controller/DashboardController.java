package com.hscoderadar.domain.dashboard.controller;

import com.hscoderadar.common.response.ApiResponse;
import com.hscoderadar.common.response.ApiResponseMessage;
import com.hscoderadar.config.oauth.PrincipalDetails;
import com.hscoderadar.domain.dashboard.dto.DashboardDto;
import com.hscoderadar.domain.dashboard.dto.ProcessedCountResponse;
import com.hscoderadar.domain.dashboard.service.DashboardService;
import com.hscoderadar.domain.feed.dto.FeedDto;
import com.hscoderadar.domain.feed.service.FeedService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
  public DashboardDto.DashboardSummaryResponse getDashboardSummary(
      @AuthenticationPrincipal PrincipalDetails principalDetails) {
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

  @Operation(summary = "모든 피드 읽음으로 표시", description = "모든 읽지 않은 피드를 읽음으로 표시하고 처리된 개수를 반환합니다.")
  @ApiResponses(value = {
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "모든 피드를 성공적으로 읽음 처리했습니다.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProcessedCountResponse.class))),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다.")
  })
  @PutMapping("/feeds/read-all")
  public ResponseEntity<ApiResponse<ProcessedCountResponse>> markAllFeedsAsRead(
      @AuthenticationPrincipal PrincipalDetails principalDetails) {
    int count = feedService.markAllFeedsAsRead(principalDetails.getUser());
    ProcessedCountResponse responseData = new ProcessedCountResponse(count);

    ApiResponse<ProcessedCountResponse> apiResponse = ApiResponse.success("모든 피드를 읽음 처리했습니다.", responseData);

    return ResponseEntity.ok(apiResponse);
  }
}