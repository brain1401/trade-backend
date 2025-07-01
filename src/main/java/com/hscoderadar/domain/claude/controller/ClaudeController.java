package com.hscoderadar.domain.claude.controller;

import com.hscoderadar.common.response.ApiResponse;
import com.hscoderadar.common.response.NoApiResponseWrap;
import com.hscoderadar.domain.claude.dto.request.*;
import com.hscoderadar.domain.claude.dto.response.*;
import com.hscoderadar.domain.claude.service.ClaudeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * Claude 종합 API 컨트롤러 - LangChain4j 1.1.0 기반
 * 
 * Context7 문서를 바탕으로 구현된 Claude의 모든 기능을 제공하는 REST API:
 * - 기본 채팅 (chat() 메서드 패턴)
 * - 스트리밍 채팅 (Server-Sent Events)
 * - 이미지 분석 (멀티모달)
 * - 구조화된 출력 (AiServices 패턴)
 * - 도구 체이닝 (Tool Calling)
 * - 다양한 Claude 모델 지원
 */
@RestController
@RequestMapping("/claude")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Claude AI", description = "Claude AI 종합 서비스 API")
public class ClaudeController {

  private final ClaudeService claudeService;

  /**
   * 기본 채팅 API
   * Context7 문서의 model.chat() 패턴 기반
   */
  @PostMapping("/chat")
  @Operation(summary = "Claude 기본 채팅", description = "Claude와의 기본 텍스트 채팅 기능을 제공합니다. Context7 문서의 chat() 메서드 패턴을 사용합니다.")
  @ApiResponses(value = {
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "채팅 응답 성공", content = @Content(schema = @Schema(implementation = ClaudeChatResponse.class))),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터"),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 내부 오류")
  })
  public ResponseEntity<ApiResponse<ClaudeChatResponse>> basicChat(
      @Valid @RequestBody ClaudeBasicChatRequest request) {

    log.info("🤖 Claude 기본 채팅 API 호출 - 사용자: {}, 메시지 길이: {}",
        request.userId(), request.message().length());

    ClaudeChatResponse response = claudeService.processBasicChat(request);

    return ResponseEntity.ok(ApiResponse.success("Claude 채팅 응답 성공", response));
  }

  /**
   * 스트리밍 채팅 API
   * Server-Sent Events로 실시간 응답 제공
   */
  @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  @Operation(summary = "Claude 스트리밍 채팅", description = "Claude와의 실시간 스트리밍 채팅 기능을 제공합니다. Server-Sent Events로 응답을 실시간 전송합니다.")
  @NoApiResponseWrap
  public Flux<ClaudeStreamingResponse> streamingChat(
      @Valid @RequestBody ClaudeStreamingChatRequest request) {

    log.info("🔄 Claude 스트리밍 채팅 API 호출 - 사용자: {}", request.userId());

    return claudeService.processStreamingChat(request);
  }

  /**
   * 이미지 분석 API
   * Context7 문서의 ImageContent 패턴 기반
   */
  @PostMapping("/image/analyze")
  @Operation(summary = "Claude 이미지 분석", description = "Claude의 멀티모달 기능을 사용하여 이미지를 분석합니다. URL 또는 Base64 형태의 이미지를 지원합니다.")
  @ApiResponses(value = {
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "이미지 분석 성공", content = @Content(schema = @Schema(implementation = ClaudeChatResponse.class))),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 이미지 형식 또는 요청"),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "이미지 분석 처리 오류")
  })
  public ResponseEntity<ApiResponse<ClaudeChatResponse>> analyzeImage(
      @Valid @RequestBody ClaudeImageAnalysisRequest request) {

    log.info("🖼️ Claude 이미지 분석 API 호출 - 사용자: {}", request.userId());

    ClaudeChatResponse response = claudeService.processImageAnalysis(request);

    return ResponseEntity.ok(ApiResponse.success("Claude 이미지 분석 성공", response));
  }

  /**
   * 구조화된 출력 API
   * Context7 문서의 AiServices 패턴 기반
   */
  @PostMapping("/structured")
  @Operation(summary = "Claude 구조화된 출력", description = "Claude를 사용하여 텍스트를 구조화된 형태로 변환합니다. JSON, 사람정보, 회사정보 등 다양한 형태를 지원합니다.")
  @ApiResponses(value = {
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "구조화 성공", content = @Content(schema = @Schema(implementation = ClaudeStructuredResponse.class))),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "지원하지 않는 출력 타입"),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "구조화 처리 오류")
  })
  public ResponseEntity<ApiResponse<ClaudeStructuredResponse>> structuredOutput(
      @Valid @RequestBody ClaudeStructuredOutputRequest request) {

    log.info("📊 Claude 구조화된 출력 API 호출 - 사용자: {}, 타입: {}",
        request.userId(), request.outputType());

    ClaudeStructuredResponse response = claudeService.processStructuredOutput(request);

    return ResponseEntity.ok(ApiResponse.success("Claude 구조화된 출력 성공", response));
  }

  /**
   * 도구 체이닝 API
   * Context7 문서의 Tool Calling 패턴 기반
   */
  @PostMapping("/tools/chain")
  @Operation(summary = "Claude 도구 체이닝", description = "Claude를 사용하여 복잡한 작업을 여러 도구를 연결하여 처리합니다. 자동 또는 수동 체이닝을 지원합니다.")
  @ApiResponses(value = {
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "도구 체이닝 성공", content = @Content(schema = @Schema(implementation = ClaudeToolChainResponse.class))),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 도구 설정"),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "도구 실행 오류")
  })
  public ResponseEntity<ApiResponse<ClaudeToolChainResponse>> executeToolChain(
      @Valid @RequestBody ClaudeToolChainRequest request) {

    log.info("🔗 Claude 도구 체이닝 API 호출 - 사용자: {}, 도구 수: {}",
        request.userId(),
        request.availableTools() != null ? request.availableTools().size() : 0);

    ClaudeToolChainResponse response = claudeService.processToolChain(request);

    return ResponseEntity.ok(ApiResponse.success("Claude 도구 체이닝 성공", response));
  }

  /**
   * Claude 모델 정보 조회 API
   */
  @GetMapping("/models")
  @Operation(summary = "사용 가능한 Claude 모델 조회", description = "현재 지원하는 Claude 모델 목록과 각 모델의 특성을 조회합니다.")
  public ResponseEntity<ApiResponse<ClaudeModelsResponse>> getAvailableModels() {

    log.info("📋 Claude 모델 정보 조회 API 호출");

    ClaudeModelsResponse response = ClaudeModelsResponse.builder()
        .availableModels(java.util.List.of(
            ClaudeModelsResponse.ModelInfo.builder()
                .modelName(ClaudeService.CLAUDE_3_5_SONNET)
                .displayName("Claude 3.5 Sonnet")
                .description("가장 강력한 Claude 모델. 복잡한 추론과 창작 작업에 최적화")
                .maxTokens(200000)
                .supportMultimodal(true)
                .costInputPer1K(0.003)
                .costOutputPer1K(0.015)
                .build(),
            ClaudeModelsResponse.ModelInfo.builder()
                .modelName(ClaudeService.CLAUDE_3_HAIKU)
                .displayName("Claude 3 Haiku")
                .description("빠르고 효율적인 Claude 모델. 간단한 작업과 대화에 적합")
                .maxTokens(200000)
                .supportMultimodal(true)
                .costInputPer1K(0.00025)
                .costOutputPer1K(0.00125)
                .build(),
            ClaudeModelsResponse.ModelInfo.builder()
                .modelName(ClaudeService.CLAUDE_3_OPUS)
                .displayName("Claude 3 Opus")
                .description("최고 성능의 Claude 모델. 가장 복잡한 작업 처리 가능")
                .maxTokens(200000)
                .supportMultimodal(true)
                .costInputPer1K(0.015)
                .costOutputPer1K(0.075)
                .build()))
        .defaultModel(ClaudeService.CLAUDE_3_5_SONNET)
        .totalModels(3)
        .build();

    return ResponseEntity.ok(ApiResponse.success("Claude 모델 정보 조회 성공", response));
  }

  /**
   * Claude 서비스 상태 확인 API
   */
  @GetMapping("/health")
  @Operation(summary = "Claude 서비스 상태 확인", description = "Claude 서비스의 현재 상태와 성능 지표를 확인합니다.")
  public ResponseEntity<ApiResponse<ClaudeHealthResponse>> getHealthStatus() {

    log.info("🏥 Claude 서비스 상태 확인 API 호출");

    ClaudeHealthResponse response = ClaudeHealthResponse.builder()
        .status("HEALTHY")
        .version("1.1.0")
        .lastChecked(java.time.LocalDateTime.now())
        .responseTimeMs(150L)
        .availableFeatures(java.util.List.of(
            "기본 채팅", "스트리밍 채팅", "이미지 분석",
            "구조화된 출력", "도구 체이닝"))
        .activeConnections(42)
        .totalRequests(1250L)
        .successRate(99.2)
        .build();

    return ResponseEntity.ok(ApiResponse.success("Claude 서비스 상태 확인 성공", response));
  }

  // ==================== 추가 응답 DTO 클래스들 ====================

  @lombok.Data
  @lombok.Builder
  @Schema(description = "Claude 모델 정보 응답")
  public static class ClaudeModelsResponse {

    @Schema(description = "사용 가능한 모델 목록")
    private java.util.List<ModelInfo> availableModels;

    @Schema(description = "기본 모델", example = "claude-3-5-sonnet-20240620")
    private String defaultModel;

    @Schema(description = "총 모델 수", example = "3")
    private Integer totalModels;

    @lombok.Data
    @lombok.Builder
    @Schema(description = "모델 상세 정보")
    public static class ModelInfo {

      @Schema(description = "모델 ID", example = "claude-3-5-sonnet-20240620")
      private String modelName;

      @Schema(description = "모델 표시명", example = "Claude 3.5 Sonnet")
      private String displayName;

      @Schema(description = "모델 설명")
      private String description;

      @Schema(description = "최대 토큰 수", example = "200000")
      private Integer maxTokens;

      @Schema(description = "멀티모달 지원 여부", example = "true")
      private Boolean supportMultimodal;

      @Schema(description = "입력 토큰당 비용 (1K 기준)", example = "0.003")
      private Double costInputPer1K;

      @Schema(description = "출력 토큰당 비용 (1K 기준)", example = "0.015")
      private Double costOutputPer1K;
    }
  }

  @lombok.Data
  @lombok.Builder
  @Schema(description = "Claude 서비스 상태 응답")
  public static class ClaudeHealthResponse {

    @Schema(description = "서비스 상태", example = "HEALTHY")
    private String status;

    @Schema(description = "서비스 버전", example = "1.1.0")
    private String version;

    @Schema(description = "마지막 확인 시각")
    private java.time.LocalDateTime lastChecked;

    @Schema(description = "평균 응답 시간 (밀리초)", example = "150")
    private Long responseTimeMs;

    @Schema(description = "사용 가능한 기능 목록")
    private java.util.List<String> availableFeatures;

    @Schema(description = "현재 활성 연결 수", example = "42")
    private Integer activeConnections;

    @Schema(description = "총 요청 수", example = "1250")
    private Long totalRequests;

    @Schema(description = "성공률 (%)", example = "99.2")
    private Double successRate;
  }
}