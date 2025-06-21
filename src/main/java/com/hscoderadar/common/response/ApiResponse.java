package com.hscoderadar.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 모든 API 응답의 표준 형식
 * 
 * <p>
 * 이 레코드는 모든 API 응답에 대해 일관된 형식을 제공합니다.
 * 모든 필드는 null 값도 JSON 응답에 포함되어 클라이언트가
 * 항상 일관된 응답 구조를 받을 수 있도록 보장합니다.
 * 
 * @param <T> 응답 데이터의 타입
 * 
 * @example 성공 응답 (데이터 있음)
 * 
 *          <pre>
 * {
 *   "success": "SUCCESS",
 *   "message": "데이터 조회 성공",
 *   "data": {"id": 1, "name": "test"}
 * }
 *          </pre>
 * 
 * @example 성공 응답 (데이터 null)
 * 
 *          <pre>
 * {
 *   "success": "SUCCESS", 
 *   "message": "처리 완료",
 *   "data": null
 * }
 *          </pre>
 * 
 * @example 오류 응답
 * 
 *          <pre>
 * {
 *   "success": "ERROR",
 *   "message": "요청 처리 중 오류 발생",
 *   "data": null
 * }
 *          </pre>
 * 
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record ApiResponse<T>(
    String success, // SUCCESS or ERROR
    String message, // 성공 또는 오류 메시지
    @JsonInclude(JsonInclude.Include.ALWAYS) T data // 서비스에서 반환하는 데이터 (null 값도 포함)
) {
  // 검증을 위한 compact canonical constructor
  public ApiResponse {
    if (success == null || success.trim().isEmpty()) {
      throw new IllegalArgumentException("결과 상태는 필수입니다");
    }
  }

  // 성공 응답 생성 헬퍼 메서드
  public static <T> ApiResponse<T> success(String message, T data) {
    return new ApiResponse<>("SUCCESS", message, data);
  }

  // 오류 응답 생성 헬퍼 메서드
  public static <T> ApiResponse<T> error(String message) {
    return new ApiResponse<>("ERROR", message, null);
  }
}