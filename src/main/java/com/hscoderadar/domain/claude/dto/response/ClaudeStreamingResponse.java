package com.hscoderadar.domain.claude.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * Claude 스트리밍 응답 DTO
 * 
 * Claude API의 실시간 스트리밍 응답 정보
 */
@Schema(description = "Claude 스트리밍 응답")
public record ClaudeStreamingResponse(

    @Schema(description = "응답 ID", example = "stream_123456") String responseId,

    @Schema(description = "스트리밍 타입", example = "PARTIAL", allowableValues = {
        "START", "PARTIAL", "COMPLETE", "ERROR" }) String type,

    @Schema(description = "부분 응답 내용", example = "안녕하") String partialContent,

    @Schema(description = "완전한 응답 내용 (COMPLETE 타입일 때만)") String fullContent,

    @Schema(description = "현재까지의 토큰 수", example = "45") Integer currentTokenCount,

    @Schema(description = "스트리밍 순서", example = "15") Integer sequenceNumber,

    @Schema(description = "완료 여부", example = "false") Boolean isComplete,

    @Schema(description = "오류 메시지 (ERROR 타입일 때)") String errorMessage,

    @Schema(description = "타임스탬프") LocalDateTime timestamp,

    @Schema(description = "사용자 ID", example = "user123") String userId,

    @Schema(description = "추정 완료 시간 (밀리초)", example = "2000") Long estimatedCompletionTimeMs){

  /**
   * 기본값 적용 생성자
   */
  public ClaudeStreamingResponse {
    if (currentTokenCount == null) {
      currentTokenCount = 0;
    }
    if (sequenceNumber == null) {
      sequenceNumber = 0;
    }
    if (isComplete == null) {
      isComplete = false;
    }
    if (timestamp == null) {
      timestamp = LocalDateTime.now();
    }
  }

  /**
   * 오류 응답 여부 확인
   */
  public boolean isError() {
    return "ERROR".equals(type);
  }

  /**
   * 최종 응답 여부 확인
   */
  public boolean isFinalResponse() {
    return "COMPLETE".equals(type) || isError();
  }
}