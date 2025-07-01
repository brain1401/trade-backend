package com.hscoderadar.domain.chat.controller;

import com.hscoderadar.common.response.NoApiResponseWrap;
import com.hscoderadar.domain.chat.dto.request.TradeChatRequest;
import com.hscoderadar.domain.chat.dto.response.TradeChatStreamingResponse;
import com.hscoderadar.domain.chat.service.TradeChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * 무역 특화 통합 채팅 컨트롤러 v6.1 (SSE 표준 준수)
 * 
 * API 명세서 v6.1에 따른 구현:
 * - 단일 엔드포인트 POST /api/chat
 * - 회원/비회원 차별화 처리 (Authorization 헤더 기반)
 * - Spring WebFlux 표준 ServerSentEvent 사용
 * - SSE 표준에 맞는 스트리밍 응답
 * - 3단계 병렬 처리 (Claude 응답, 상세페이지 준비, 회원 기록 저장)
 * - LangChain4j 1.1.0-beta7 최신 패턴 적용
 */
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Trade Chat v6.1", description = "무역 특화 AI 채팅 서비스 - SSE 표준 준수")
public class TradeChatController {

  private final TradeChatService tradeChatService;

  /**
   * 무역 특화 통합 채팅 API v6.1 (SSE 표준)
   * 
   * **핵심 혁신사항:**
   * - Spring WebFlux ServerSentEvent 표준 준수
   * - 프론트엔드 SSE EventSource와 완벽 호환
   * - 회원/비회원 차별화: 회원만 첫 메시지 시 세션 생성하여 모든 대화를 pg_partman 파티션에 영구 저장
   * - 비회원 휘발성 채팅: 세션 생성, 데이터베이스 저장 등 일체의 저장 행위 없이 실시간 채팅만 제공
   * - 3단계 병렬 처리: [자연어 응답] + [상세페이지 준비] + [회원 기록 저장] 동시 실행
   * - SSE 메타데이터 북마크: HSCode 정보 포함시 SSE 메타데이터로 전달하여 프론트엔드에서 북마크 버튼 표시
   * - RAG 백엔드 처리: HSCode 벡터 검색 및 캐시 저장을 백엔드에서 내부적으로 처리
   * - LangChain4j 1.1.0-beta7 최신 스트리밍 패턴 적용
   */
  @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  @Operation(summary = "무역 특화 통합 채팅 v6.1 (SSE 표준)", description = """
      **단일 엔드포인트로 모든 무역 관련 AI 채팅을 처리합니다.**

      ## 🚀 v6.1 혁신 기능:
      - ✅ **SSE 표준 준수**: Spring WebFlux ServerSentEvent 사용으로 모든 브라우저 EventSource와 완벽 호환
      - ✅ **회원/비회원 차별화**: 회원만 첫 메시지 시 세션 생성하여 대화 기록 영구 저장, 비회원은 완전 휘발성
      - ✅ **3단계 병렬 처리**: [자연어 응답 스트리밍] + [상세페이지 정보 준비] + [회원 대화 기록 저장]
      - ✅ **SSE 메타데이터 북마크**: HSCode 정보 포함시 SSE 메타데이터로 전달하여 프론트엔드에서 북마크 버튼 표시
      - ✅ **RAG 백엔드 처리**: voyage-3-large + PostgreSQL+pgvector 기반 의미적 검색
      - ✅ **로딩 최적화**: 상세페이지 버튼 준비 전까지 로딩 스피너 표시
      - ✅ **LangChain4j 1.1.0-beta7**: 최신 스트리밍 패턴 및 성능 최적화

      ## 📡 SSE 이벤트 타입 (표준 준수):
      - **initial_metadata**: Claude 의도 분석 + 회원/비회원 상태 + RAG 활성화
      - **session_info**: 🆕 회원/비회원 차별화 정보
      - **thinking_***: 3단계 병렬 처리 진행 상황
      - **main_message_***: Claude 자연어 응답 스트리밍
      - **detail_page_***: 상세페이지 버튼 준비 완료
      - **member_***: 🆕 회원 전용 이벤트 (세션 생성, 기록 저장)

      ## 🔐 인증 방식:
      - **선택적 인증**: Authorization 헤더 제공 시 회원으로 처리, 미제공 시 비회원으로 처리
      - **JWT 토큰**: `Bearer {accessToken}` 형식 (Access Token 30분)

      ## 🎯 처리 흐름:
      1. **차별화된 질의**: 자연어 질문 → 인증 상태 확인 → 회원만 세션 생성 → 즉시 SSE 스트리밍 시작
      2. **3단계 병렬 처리**: [자연어 응답] + [상세페이지 준비] + [회원 기록 저장] 동시 실행
      3. **SSE 메타데이터**: 북마크 가능한 HSCode 정보 발견 시 프론트엔드로 메타데이터 전송

      ## 💻 프론트엔드 사용법:
      ```javascript
      const eventSource = new EventSource('/api/chat', {
        method: 'POST',
        headers: {
          'Authorization': 'Bearer your-jwt-token', // 선택적
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          message: 'HSCode 관련 질문',
          sessionId: 'session-id' // 회원만
        })
      });

      eventSource.onmessage = function(event) {
        const data = JSON.parse(event.data);
        console.log('Event:', event.type, 'Data:', data);
      };
      ```
      """)
  @ApiResponses(value = {
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "SSE 스트리밍 응답 성공", content = @Content(schema = @Schema(implementation = TradeChatStreamingResponse.class))),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 (메시지 길이, 무역 외 질문 등)"),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "무역 관련 질문이 아님 (무역 외 질의 차단)"),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "채팅 요청 한도 초과"),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "502", description = "Claude AI 분석 실패 또는 RAG 검색 실패"),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 내부 오류")
  })
  @Parameter(name = "Authorization", description = "선택적 JWT 토큰 (Bearer {accessToken}). 제공 시 회원으로 처리되어 대화 기록 저장", in = ParameterIn.HEADER, required = false, schema = @Schema(type = "string", example = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."))
  @NoApiResponseWrap
  public Flux<ServerSentEvent<TradeChatStreamingResponse>> chat(
      @Valid @RequestBody TradeChatRequest request,
      HttpServletRequest httpRequest) {

    // Authorization 헤더에서 인증 정보 추출
    String authHeader = httpRequest.getHeader("Authorization");
    boolean isAuthenticated = authHeader != null && authHeader.startsWith("Bearer ");
    String userId = null;

    if (isAuthenticated) {
      // JWT 토큰에서 사용자 ID 추출 (실제 구현에서는 JWT 서비스 사용)
      userId = extractUserIdFromToken(authHeader);
    }

    // 요청에 인증 정보 추가
    TradeChatRequest enhancedRequest = TradeChatRequest.builder()
        .message(request.message())
        .sessionId(request.sessionId())
        .modelName(request.modelName())
        .temperature(request.temperature())
        .maxTokens(request.maxTokens())
        .isAuthenticated(isAuthenticated)
        .userId(userId)
        .clientId(request.clientId())
        .context(extractRequestContext(httpRequest))
        .build();

    log.info("🚢 무역 특화 통합 채팅 API v6.1 호출 (SSE 표준) - 사용자: {}, 메시지 길이: {}, 회원 여부: {}, 세션: {}",
        userId != null ? userId : "비회원",
        request.message().length(),
        isAuthenticated,
        request.sessionId());

    // 즉시 SSE 스트리밍 시작 (Spring WebFlux 표준)
    return tradeChatService.processTradeChat(enhancedRequest);
  }

  /**
   * 채팅 세션 상태 확인 API (회원 전용)
   */
  @GetMapping("/session/{sessionId}/status")
  @Operation(summary = "채팅 세션 상태 확인", description = "특정 채팅 세션의 현재 상태를 확인합니다. 회원 전용 기능입니다.")
  @Parameter(name = "Authorization", description = "JWT 토큰 (Bearer {accessToken}) - 필수", in = ParameterIn.HEADER, required = true)
  public SessionStatusResponse getSessionStatus(
      @PathVariable String sessionId,
      HttpServletRequest httpRequest) {

    String authHeader = httpRequest.getHeader("Authorization");
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      throw new IllegalArgumentException("인증이 필요합니다");
    }

    String userId = extractUserIdFromToken(authHeader);
    log.info("📊 채팅 세션 상태 확인 - 세션 ID: {}, 사용자: {}", sessionId, userId);

    // TODO: 실제 세션 상태 조회 로직 구현
    return SessionStatusResponse.builder()
        .sessionId(sessionId)
        .status("ACTIVE")
        .messageCount(5)
        .isAuthenticated(true)
        .userId(userId)
        .lastActivity(java.time.LocalDateTime.now().minusMinutes(2))
        .createdAt(java.time.LocalDateTime.now().minusHours(1))
        .partitionYear(2024)
        .build();
  }

  // ==================== Private Helper Methods ====================

  /**
   * JWT 토큰에서 사용자 ID 추출
   */
  private String extractUserIdFromToken(String authHeader) {
    try {
      // TODO: 실제 JWT 서비스와 연동
      // JwtTokenProvider를 사용하여 토큰 검증 및 사용자 ID 추출
      String token = authHeader.substring(7); // "Bearer " 제거

      // 현재는 시뮬레이션 (실제 구현에서는 JWT 서비스 사용)
      return "user_" + Math.abs(token.hashCode() % 10000);

    } catch (Exception e) {
      log.warn("JWT 토큰 파싱 실패: {}", e.getMessage());
      return null;
    }
  }

  /**
   * 요청 컨텍스트 정보 추출
   */
  private java.util.Map<String, Object> extractRequestContext(HttpServletRequest httpRequest) {
    return java.util.Map.of(
        "userAgent", httpRequest.getHeader("User-Agent") != null ? httpRequest.getHeader("User-Agent") : "Unknown",
        "remoteAddr", httpRequest.getRemoteAddr(),
        "language", httpRequest.getHeader("Accept-Language") != null ? httpRequest.getHeader("Accept-Language") : "ko",
        "timestamp", java.time.LocalDateTime.now().toString());
  }

  /**
   * 채팅 세션 상태 응답 DTO v6.1
   */
  @lombok.Data
  @lombok.Builder
  @Schema(description = "채팅 세션 상태 응답 v6.1")
  public static class SessionStatusResponse {

    @Schema(description = "세션 ID", example = "chat_session_20240116_123456")
    private String sessionId;

    @Schema(description = "세션 상태", example = "ACTIVE", allowableValues = { "ACTIVE", "CLOSED", "EXPIRED" })
    private String status;

    @Schema(description = "메시지 수", example = "5")
    private Integer messageCount;

    @Schema(description = "인증 여부", example = "true")
    private Boolean isAuthenticated;

    @Schema(description = "사용자 ID", example = "user_1234")
    private String userId;

    @Schema(description = "마지막 활동 시간")
    private java.time.LocalDateTime lastActivity;

    @Schema(description = "세션 생성 시간")
    private java.time.LocalDateTime createdAt;

    @Schema(description = "파티션 연도 (pg_partman)", example = "2024")
    private Integer partitionYear;
  }
}