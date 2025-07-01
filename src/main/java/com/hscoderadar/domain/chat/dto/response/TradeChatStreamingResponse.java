package com.hscoderadar.domain.chat.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 무역 특화 SSE 스트리밍 응답 DTO v6.1 (리팩토링됨)
 * 
 * API 명세서 v6.1 기준:
 * - Server-Sent Events로 실시간 스트리밍 제공
 * - 회원/비회원 차별화 이벤트 처리
 * - 3단계 병렬 처리 상태 표시
 * - SSE 메타데이터 기반 동적 북마크 버튼 생성
 * - 상세페이지 정보 병렬 준비
 * - LangChain4j 1.1.0-beta7 최신 패턴 적용
 */
@Data
@Builder
@Schema(description = "무역 특화 SSE 스트리밍 응답 v6.1")
public class TradeChatStreamingResponse {

  @Schema(description = "응답 ID", example = "trade-chat-123456")
  private String responseId;

  @Schema(description = "SSE 이벤트 타입 v6.1", example = "main_message_data", allowableValues = {
      // 초기 메타데이터
      "initial_metadata", "session_info",
      // Thinking 단계 (v6.1 3단계 병렬 처리)
      "thinking_intent_analysis", "thinking_parallel_processing_start", "thinking_rag_search_planning",
      "thinking_rag_search_executing", "thinking_web_search_executing", "thinking_data_processing",
      "thinking_detail_page_preparation", "thinking_member_record_saving", "thinking_response_generation",
      // Main Message 단계
      "main_message_start", "main_message_data", "main_message_complete",
      // v6.1 상세페이지 버튼 단계 (병렬 처리)
      "detail_page_buttons_start", "detail_page_button_ready", "detail_page_buttons_complete",
      // v6.1 회원 전용 이벤트
      "member_session_created", "member_record_saved",
      // 기타
      "error"
  })
  private String eventType;

  @Schema(description = "부분 응답 내용", example = "아이폰 15 프로의 정확한 HS Code는...")
  private String partialContent;

  @Schema(description = "완전한 응답 내용 (main_message_complete 타입일 때만)")
  private String fullContent;

  @Schema(description = "Claude 사고과정 (thinking_* 타입일 때)")
  private String thinkingProcess;

  @Schema(description = "진행률 (0-100)", example = "45")
  private Integer progress;

  @Schema(description = "북마크 메타데이터 (main_message_complete 타입일 때)")
  private BookmarkMetadata bookmarkData;

  @Schema(description = "상세페이지 버튼 정보 (detail_page_button_ready 타입일 때)")
  private DetailPageButton detailPageButton;

  @Schema(description = "의도 분석 결과")
  private IntentAnalysis intentAnalysis;

  @Schema(description = "세션 정보 (회원 전용)")
  private SessionInfo sessionInfo;

  @Schema(description = "현재까지의 토큰 수", example = "45")
  private Integer currentTokenCount;

  @Schema(description = "스트리밍 순서", example = "15")
  private Integer sequenceNumber;

  @Schema(description = "완료 여부", example = "false")
  private Boolean isComplete;

  @Schema(description = "오류 메시지 (error 타입일 때)")
  private String errorMessage;

  @Schema(description = "타임스탬프")
  private LocalDateTime timestamp;

  @Schema(description = "사용자 식별자", example = "회원:user_1234 또는 비회원:client-12345")
  private String userIdentifier;

  @Schema(description = "세션 ID (회원만)", example = "chat_session_20240116_123456")
  private String sessionId;

  @Schema(description = "추가 메타데이터")
  private Map<String, Object> metadata;

  // ==================== v6.1 Inner Classes ====================

  /**
   * 북마크 메타데이터 (SSE 기반 동적 북마크 생성)
   */
  @Data
  @Builder
  @Schema(description = "북마크 메타데이터 v6.1")
  public static class BookmarkMetadata {

    @Schema(description = "북마크 생성 가능 여부", example = "true")
    private Boolean available;

    @Schema(description = "HS Code", example = "8517.12.00")
    private String hsCode;

    @Schema(description = "품목명", example = "스마트폰 및 기타 무선전화기")
    private String productName;

    @Schema(description = "분류 신뢰도 (0.0-1.0)", example = "0.95")
    private Double confidence;

    @Schema(description = "분류 근거", example = "셀룰러 네트워크용 무선전화기")
    private String classificationBasis;

    @Schema(description = "관세율 정보", example = "기본 8%, FTA 적용시 0%")
    private String tariffInfo;
  }

  /**
   * 상세페이지 버튼 정보 (병렬 처리)
   */
  @Data
  @Builder
  @Schema(description = "상세페이지 버튼 정보 v6.1")
  public static class DetailPageButton {

    @Schema(description = "버튼 타입", example = "HS_CODE", allowableValues = {
        "HS_CODE", "REGULATION", "STATISTICS" })
    private String buttonType;

    @Schema(description = "우선순위 (낮을수록 높은 우선순위)", example = "1")
    private Integer priority;

    @Schema(description = "버튼 URL", example = "/detail/hscode/8517.12.00")
    private String url;

    @Schema(description = "버튼 제목", example = "HS Code 상세정보")
    private String title;

    @Schema(description = "버튼 설명", example = "관세율, 규제정보 등")
    private String description;

    @Schema(description = "준비 상태", example = "true")
    private Boolean isReady;

    @Schema(description = "준비 시간 (밀리초)", example = "1500")
    private Long preparationTime;
  }

  /**
   * 의도 분석 결과
   */
  @Data
  @Builder
  @Schema(description = "의도 분석 결과 v6.1")
  public static class IntentAnalysis {

    @Schema(description = "분석된 의도", example = "HS_CODE_ANALYSIS", allowableValues = {
        "HS_CODE_ANALYSIS", "CARGO_TRACKING", "TRADE_REGULATION", "GENERAL_TRADE_INFO", "MARKET_ANALYSIS" })
    private String claudeIntent;

    @Schema(description = "추정 처리 시간 (초)", example = "15")
    private Integer estimatedTime;

    @Schema(description = "신뢰도 점수 (0.0-1.0)", example = "0.95")
    private Double confidenceScore;

    @Schema(description = "추출된 키워드")
    private List<String> extractedKeywords;

    @Schema(description = "무역 관련 여부", example = "true")
    private Boolean isTradeRelated;

    @Schema(description = "RAG 검색 활성화 여부", example = "true")
    private Boolean ragEnabled;

    @Schema(description = "병렬 처리 활성화 여부", example = "true")
    private Boolean parallelProcessing;
  }

  /**
   * 회원 세션 정보
   */
  @Data
  @Builder
  @Schema(description = "회원 세션 정보 v6.1")
  public static class SessionInfo {

    @Schema(description = "인증 여부", example = "true")
    private Boolean isAuthenticated;

    @Schema(description = "사용자 타입", example = "MEMBER", allowableValues = { "MEMBER", "GUEST" })
    private String userType;

    @Schema(description = "세션 ID", example = "chat_session_20240116_123456")
    private String sessionId;

    @Schema(description = "기록 저장 활성화 여부", example = "true")
    private Boolean recordingEnabled;

    @Schema(description = "안내 메시지", example = "회원님의 대화가 기록되어 나중에 다시 볼 수 있습니다")
    private String message;

    @Schema(description = "첫 메시지 여부", example = "true")
    private Boolean isFirstMessage;

    @Schema(description = "파티션 연도", example = "2024")
    private Integer partitionYear;
  }

  // ==================== Helper Methods ====================

  /**
   * 기본값 적용
   */
  public Integer getCurrentTokenCount() {
    return currentTokenCount != null ? currentTokenCount : 0;
  }

  public Integer getSequenceNumber() {
    return sequenceNumber != null ? sequenceNumber : 0;
  }

  public Boolean getIsComplete() {
    return isComplete != null ? isComplete : false;
  }

  public LocalDateTime getTimestamp() {
    return timestamp != null ? timestamp : LocalDateTime.now();
  }

  /**
   * 오류 응답 여부 확인
   */
  public boolean isError() {
    return "error".equals(eventType);
  }

  /**
   * 최종 응답 여부 확인
   */
  public boolean isFinalResponse() {
    return "main_message_complete".equals(eventType) || isError();
  }

  /**
   * 초기 메타데이터 이벤트 여부 확인
   */
  public boolean isInitialMetadata() {
    return "initial_metadata".equals(eventType) || "session_info".equals(eventType);
  }

  /**
   * Thinking 단계 이벤트 여부 확인
   */
  public boolean isThinkingEvent() {
    return eventType != null && eventType.startsWith("thinking_");
  }

  /**
   * Main Message 단계 이벤트 여부 확인
   */
  public boolean isMainMessageEvent() {
    return eventType != null && eventType.startsWith("main_message_");
  }

  /**
   * 상세페이지 버튼 이벤트 여부 확인
   */
  public boolean isDetailPageEvent() {
    return eventType != null && eventType.startsWith("detail_page_");
  }

  /**
   * 회원 전용 이벤트 여부 확인
   */
  public boolean isMemberEvent() {
    return eventType != null && eventType.startsWith("member_");
  }

  /**
   * 북마크 메타데이터 포함 여부 확인
   */
  public boolean hasBookmarkData() {
    return bookmarkData != null && Boolean.TRUE.equals(bookmarkData.getAvailable());
  }

  /**
   * 상세페이지 버튼 준비 완료 여부 확인
   */
  public boolean isDetailPageButtonReady() {
    return "detail_page_button_ready".equals(eventType) &&
        detailPageButton != null &&
        Boolean.TRUE.equals(detailPageButton.getIsReady());
  }

  /**
   * 회원 세션 생성 이벤트 여부 확인
   */
  public boolean isMemberSessionCreated() {
    return "member_session_created".equals(eventType);
  }

  /**
   * 회원 기록 저장 완료 이벤트 여부 확인
   */
  public boolean isMemberRecordSaved() {
    return "member_record_saved".equals(eventType);
  }
}