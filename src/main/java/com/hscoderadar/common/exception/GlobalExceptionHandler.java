package com.hscoderadar.common.exception;

import com.hscoderadar.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * API 명세서 v2.4 기준 전역 예외 처리 핸들러
 * 
 * 애플리케이션에서 발생하는 모든 예외를 일관된 {@link ApiResponse} 형태로 변환하여
 * 클라이언트에게 정확한 HTTP 상태 코드와 에러 코드를 제공
 * 
 * <h3>보안 정책:</h3>
 * <ul>
 * <li>사용자 열거 공격 방지: 모든 인증 실패를 AUTH_001로 통일</li>
 * <li>내부 시스템 정보 노출 방지</li>
 * <li>일관된 에러 응답 형태 제공</li>
 * </ul>
 * 
 * @author HsCodeRadar Team
 * @since 2.4.0
 * @see ErrorCode
 * @see ApiResponse
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  /**
   * 에러 응답 엔티티 생성 헬퍼 메서드
   */
  private static ResponseEntity<ApiResponse<?>> createErrorResponse(ErrorCode errorCode) {
    log.error("API 에러 발생: {} - {}", errorCode.name(), errorCode.getMessage());
    ApiResponse<?> response = ApiResponse.error(errorCode.getMessage());
    return new ResponseEntity<>(response, errorCode.getHttpStatus());
  }

  private static ResponseEntity<ApiResponse<?>> createErrorResponse(String message, HttpStatus status) {
    log.error("일반 에러 발생: {} - {}", status, message);
    ApiResponse<?> response = ApiResponse.error(message);
    return new ResponseEntity<>(response, status);
  }

  // ===== 인증 관련 예외 처리 =====

  /**
   * 사용자 정의 인증 예외 처리
   * 사용자 열거 공격 방지를 위해 통합된 예외 처리
   */
  @ExceptionHandler(AuthException.class)
  public ResponseEntity<ApiResponse<?>> handleAuthException(AuthException ex) {
    return createErrorResponse(ex.getErrorCode());
  }

  /**
   * Spring Security 인증 실패 예외 처리
   * 모든 인증 실패를 AUTH_001로 통일 (사용자 열거 공격 방지)
   */
  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<ApiResponse<?>> handleBadCredentialsException(BadCredentialsException ex) {
    log.warn("인증 실패 시도: {}", ex.getMessage());
    return createErrorResponse(ErrorCode.AUTH_001);
  }

  /**
   * Spring Security 일반 인증 예외 처리
   */
  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ApiResponse<?>> handleAuthenticationException(AuthenticationException ex) {
    log.warn("인증 예외: {}", ex.getMessage());
    return createErrorResponse(ErrorCode.AUTH_004);
  }

  /**
   * Spring Security 접근 권한 예외 처리
   */
  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiResponse<?>> handleAccessDeniedException(AccessDeniedException ex) {
    log.warn("접근 권한 없음: {}", ex.getMessage());
    return createErrorResponse(ErrorCode.AUTH_005);
  }

  // ===== Rate Limiting 예외 처리 =====

  /**
   * Rate Limiting 예외 처리
   */
  @ExceptionHandler(RateLimitException.class)
  public ResponseEntity<ApiResponse<?>> handleRateLimitException(RateLimitException ex) {
    return createErrorResponse(ex.getErrorCode());
  }

  // ===== 사용자 관련 예외 처리 =====

  /**
   * 이메일 중복 등 사용자 관련 IllegalArgumentException 처리
   */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiResponse<?>> handleIllegalArgumentException(IllegalArgumentException ex) {
    String message = ex.getMessage();

    // 이메일 중복 체크
    if (message != null && message.contains("이미 사용 중인 이메일")) {
      return createErrorResponse(ErrorCode.USER_001);
    }

    // 비밀번호 정책 위반
    if (message != null && message.contains("비밀번호")) {
      return createErrorResponse(ErrorCode.USER_004);
    }

    // 사용자 정보 없음
    if (message != null && (message.contains("사용자를 찾을 수 없습니다") ||
        message.contains("인증 정보가 없습니다"))) {
      return createErrorResponse(ErrorCode.AUTH_004);
    }

    // 기본 잘못된 요청 처리
    return createErrorResponse(ErrorCode.USER_002);
  }

  // ===== Validation 예외 처리 =====

  /**
   * JSON 요청 데이터 검증 실패 예외 처리
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<?>> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
    List<String> errors = new ArrayList<>();
    for (FieldError error : ex.getBindingResult().getFieldErrors()) {
      errors.add(error.getField() + ": " + error.getDefaultMessage());
    }

    String detailMessage = "입력 데이터 검증 실패: " + String.join(", ", errors);
    log.warn("요청 데이터 검증 실패: {}", detailMessage);

    return createErrorResponse(ErrorCode.USER_002);
  }

  /**
   * 폼 데이터 바인딩 예외 처리
   */
  @ExceptionHandler(BindException.class)
  public ResponseEntity<ApiResponse<?>> handleBindException(BindException ex) {
    List<String> errors = new ArrayList<>();
    for (FieldError error : ex.getBindingResult().getFieldErrors()) {
      errors.add(error.getField() + ": " + error.getDefaultMessage());
    }

    log.warn("폼 데이터 바인딩 실패: {}", String.join(", ", errors));
    return createErrorResponse(ErrorCode.USER_002);
  }

  /**
   * Bean Validation API 제약 조건 위반 예외 처리
   */
  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ApiResponse<?>> handleConstraintViolationException(ConstraintViolationException ex) {
    List<String> errors = ex.getConstraintViolations()
        .stream()
        .map(ConstraintViolation::getMessage)
        .collect(Collectors.toList());

    log.warn("제약 조건 위반: {}", String.join(", ", errors));
    return createErrorResponse(ErrorCode.USER_002);
  }

  /**
   * 메서드 인수 타입 불일치 예외 처리
   */
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ApiResponse<?>> handleMethodArgumentTypeMismatchException(
      MethodArgumentTypeMismatchException ex) {

    log.warn("메서드 인수 타입 불일치: {}", ex.getMessage());
    return createErrorResponse(ErrorCode.COMMON_001);
  }

  // ===== 시스템 예외 처리 =====

  /**
   * 404 Not Found 예외 처리
   */
  @ExceptionHandler(NoHandlerFoundException.class)
  public ResponseEntity<ApiResponse<?>> handleNoHandlerFoundException(NoHandlerFoundException ex) {
    log.warn("요청 경로를 찾을 수 없음: {} {}", ex.getHttpMethod(), ex.getRequestURL());

    String message = String.format("요청된 리소스를 찾을 수 없습니다: %s %s",
        ex.getHttpMethod(), ex.getRequestURL());
    return createErrorResponse(message, HttpStatus.NOT_FOUND);
  }

  /**
   * 파일 업로드 크기 초과 예외 처리
   */
  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<ApiResponse<?>> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex) {
    log.warn("파일 업로드 크기 초과: {}", ex.getMessage());
    return createErrorResponse(ErrorCode.COMMON_003);
  }

  // ===== 기존 도메인별 예외 처리 =====

  /**
   * HS Code 분석 관련 예외 처리
   */
  @ExceptionHandler(HsCodeAnalysisException.class)
  public ResponseEntity<ApiResponse<?>> handleHsCodeAnalysisException(HsCodeAnalysisException ex) {
    log.error("HS Code 분석 예외: {}", ex.getMessage());
    return createErrorResponse(ErrorCode.SEARCH_003);
  }

  /**
   * 외부 API 호출 실패 예외 처리
   */
  @ExceptionHandler(ExternalApiException.class)
  public ResponseEntity<ApiResponse<?>> handleExternalApiException(ExternalApiException ex) {
    log.error("외부 API 예외: {}", ex.getMessage());

    // 타임아웃인지 연결 실패인지 구분
    if (ex.getMessage() != null && ex.getMessage().contains("timeout")) {
      return createErrorResponse(ErrorCode.EXTERNAL_002);
    }

    return createErrorResponse(ErrorCode.EXTERNAL_001);
  }

  /**
   * 모니터링 관련 예외 처리
   */
  @ExceptionHandler(MonitoringException.class)
  public ResponseEntity<ApiResponse<?>> handleMonitoringException(MonitoringException ex) {
    log.error("모니터링 예외: {}", ex.getMessage());
    return createErrorResponse(ErrorCode.SYSTEM_001);
  }

  // ===== 최종 예외 처리 =====

  /**
   * RuntimeException 처리 (예상치 못한 런타임 오류)
   */
  @ExceptionHandler(RuntimeException.class)
  public ResponseEntity<ApiResponse<?>> handleRuntimeException(RuntimeException ex) {
    log.error("예상치 못한 런타임 예외: ", ex);
    return createErrorResponse(ErrorCode.COMMON_002);
  }

  /**
   * 최종 예외 처리 (모든 예외의 마지막 처리)
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<?>> handleGeneralException(Exception ex) {
    log.error("예상치 못한 예외: ", ex);
    return createErrorResponse(ErrorCode.COMMON_002);
  }
}