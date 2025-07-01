package com.hscoderadar.domain.chat.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Map;

/**
 * 무역 특화 통합 채팅 요청 DTO v6.1
 * 
 * API 명세서 v6.1 기준:
 * - 단일 엔드포인트로 모든 무역 관련 질의 처리
 * - 회원/비회원 차별화 (Authorization 헤더 기반)
 * - 회원: 세션 생성 및 pg_partman 파티션에 기록 저장
 * - 비회원: 완전 휘발성 채팅만 제공
 */
@Schema(description = "무역 특화 통합 채팅 요청 v6.1")
public record TradeChatRequest(

    @NotBlank(message = "메시지 입력 필수") @Size(min = 2, max = 100000, message = "메시지는 2자 이상 100,000자 이하 입력 필요") @Schema(description = "사용자 질문", example = "아이폰 15 프로를 수입할 때 HS Code와 관세율이 어떻게 되나요?", required = true) String message,

    @Schema(description = "기존 세션 ID (연속 대화인 경우, 회원만)", example = "chat_session_20240116_123456") String sessionId,

    @Schema(description = "클라이언트 식별자 (비회원 구분용)", example = "client-12345") String clientId,

    @Schema(description = "Claude 모델명", example = "claude-3-5-sonnet-20240620", defaultValue = "claude-3-5-sonnet-20240620") String modelName,

    @Schema(description = "온도값 (0.0-2.0)", example = "0.7", defaultValue = "0.7") Double temperature,

    @Schema(description = "최대 토큰 수", example = "4000", defaultValue = "4000") Integer maxTokens,

    // ==================== v6.1 회원/비회원 차별화 필드 ====================

    @Schema(description = "인증 여부 (Authorization 헤더 기반으로 자동 설정)", example = "true") Boolean isAuthenticated,

    @Schema(description = "사용자 ID (JWT 토큰에서 추출, 회원인 경우)", example = "user_1234") String userId,

    @Schema(description = "요청 컨텍스트 (IP, User-Agent 등)") Map<String, Object> context) {

  /**
   * 빌더 팩토리 메서드
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * 기본값 적용 팩토리 메서드
   */
  public TradeChatRequest withDefaults() {
    return new TradeChatRequest(
        message,
        sessionId,
        clientId,
        modelName != null && !modelName.isBlank() ? modelName : "claude-3-5-sonnet-20240620",
        temperature != null ? temperature : 0.7,
        maxTokens != null ? maxTokens : 4000,
        isAuthenticated != null ? isAuthenticated : false,
        userId,
        context);
  }

  /**
   * 회원 여부 확인
   */
  public boolean isAuthenticatedUser() {
    return Boolean.TRUE.equals(isAuthenticated) && userId != null && !userId.isBlank();
  }

  /**
   * 새 세션 여부 확인 (회원만 해당)
   */
  public boolean isNewSession() {
    return isAuthenticatedUser() && (sessionId == null || sessionId.isBlank());
  }

  /**
   * 연속 대화 여부 확인 (회원만 해당)
   */
  public boolean isContinuousChat() {
    return isAuthenticatedUser() && sessionId != null && !sessionId.isBlank();
  }

  /**
   * 비회원 식별자 확인
   */
  public boolean hasClientId() {
    return !isAuthenticatedUser() && clientId != null && !clientId.isBlank();
  }

  /**
   * 사용자 식별 문자열 반환 (로깅용)
   */
  public String getUserIdentifier() {
    if (isAuthenticatedUser()) {
      return "회원:" + userId;
    } else if (hasClientId()) {
      return "비회원:" + clientId;
    } else {
      return "익명";
    }
  }

  /**
   * 요청 컨텍스트에서 언어 정보 추출
   */
  public String getLanguage() {
    if (context != null && context.containsKey("language")) {
      return (String) context.get("language");
    }
    return "ko";
  }

  /**
   * 요청 컨텍스트에서 User-Agent 추출
   */
  public String getUserAgent() {
    if (context != null && context.containsKey("userAgent")) {
      return (String) context.get("userAgent");
    }
    return "Unknown";
  }

  /**
   * Builder 패턴 구현 (record와 호환)
   */
  public static class Builder {
    private String message;
    private String sessionId;
    private String clientId;
    private String modelName;
    private Double temperature;
    private Integer maxTokens;
    private Boolean isAuthenticated;
    private String userId;
    private Map<String, Object> context;

    public Builder message(String message) {
      this.message = message;
      return this;
    }

    public Builder sessionId(String sessionId) {
      this.sessionId = sessionId;
      return this;
    }

    public Builder clientId(String clientId) {
      this.clientId = clientId;
      return this;
    }

    public Builder modelName(String modelName) {
      this.modelName = modelName;
      return this;
    }

    public Builder temperature(Double temperature) {
      this.temperature = temperature;
      return this;
    }

    public Builder maxTokens(Integer maxTokens) {
      this.maxTokens = maxTokens;
      return this;
    }

    public Builder isAuthenticated(Boolean isAuthenticated) {
      this.isAuthenticated = isAuthenticated;
      return this;
    }

    public Builder userId(String userId) {
      this.userId = userId;
      return this;
    }

    public Builder context(Map<String, Object> context) {
      this.context = context;
      return this;
    }

    public TradeChatRequest build() {
      return new TradeChatRequest(
          message, sessionId, clientId, modelName, temperature, maxTokens,
          isAuthenticated, userId, context);
    }
  }
}