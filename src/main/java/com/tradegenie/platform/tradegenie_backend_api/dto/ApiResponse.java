package com.tradegenie.platform.tradegenie_backend_api.dto;

/**
 * 모든 API 응답의 표준 형식
 */
public record ApiResponse<T>(
    String result, // SUCCESS or ERROR
    String message, // 성공 또는 오류 메시지
    T data // 서비스에서 반환하는 데이터
) {
  // 검증을 위한 compact canonical constructor
  public ApiResponse {
    if (result == null || result.trim().isEmpty()) {
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