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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 전역 예외 처리를 담당하는 핸들러 클래스
 * 
 * <p>
 * 애플리케이션에서 발생하는 모든 예외를 일관된 {@link ApiResponse} 형태로 변환하여
 * 클라이언트에게 응답합니다. 예외별로 적절한 HTTP 상태 코드와 메시지를 제공합니다.
 * 
 * <h3>처리하는 예외 유형:</h3>
 * <ul>
 * <li><strong>Validation 예외</strong>: 요청 데이터 검증 실패</li>
 * <li><strong>Authentication 예외</strong>: 인증/인가 관련 오류</li>
 * <li><strong>Business Logic 예외</strong>: 도메인별 비즈니스 로직 오류</li>
 * <li><strong>External API 예외</strong>: 외부 API 호출 실패</li>
 * <li><strong>System 예외</strong>: 예상치 못한 시스템 오류</li>
 * </ul>
 * 
 * <h3>응답 형태:</h3>
 * 
 * <pre>{@code
 * {
 *   "success": "ERROR",
 *   "message": "구체적인 오류 메시지",
 *   "data": null
 * }
 * }</pre>
 * 
 * @author Development Team
 * @since 1.0.0
 * @see ApiResponse
 * @see com.hscoderadar.common.response.ResponseWrapperAdvice
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  /**
   * 오류 응답 엔티티를 생성하는 헬퍼 메서드
   * 
   * @param message 오류 메시지
   * @param status  HTTP 상태 코드
   * @return ResponseEntity로 래핑된 ApiResponse
   */
  public static ResponseEntity<ApiResponse<?>> errorResponseEntity(String message, HttpStatus status) {
    ApiResponse<?> response = ApiResponse.error(message);
    return new ResponseEntity<>(response, status);
  }

  /**
   * 잘못된 인수 예외 처리
   * 
   * @param ex IllegalArgumentException 인스턴스
   * @return 400 Bad Request 응답
   */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiResponse<?>> handleIllegalArgumentException(IllegalArgumentException ex) {
    log.error("잘못된 인수 예외: ", ex);
    return errorResponseEntity(ex.getMessage(), HttpStatus.BAD_REQUEST);
  }

  /**
   * 요청 데이터 검증 실패 예외 처리 (JSON 바디 검증)
   * 
   * @param ex MethodArgumentNotValidException 인스턴스
   * @return 400 Bad Request 응답 (상세한 검증 오류 메시지 포함)
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<?>> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
    log.error("메서드 인수 검증 실패 예외: ", ex);

    List<String> errors = new ArrayList<>();
    for (FieldError error : ex.getBindingResult().getFieldErrors()) {
      errors.add(error.getField() + ": " + error.getDefaultMessage());
    }

    String message = "요청 데이터 검증 실패: " + String.join(", ", errors);
    return errorResponseEntity(message, HttpStatus.BAD_REQUEST);
  }

  /**
   * 폼 데이터 바인딩 예외 처리
   * 
   * @param ex BindException 인스턴스
   * @return 400 Bad Request 응답
   */
  @ExceptionHandler(BindException.class)
  public ResponseEntity<ApiResponse<?>> handleBindException(BindException ex) {
    log.error("바인딩 예외: ", ex);

    List<String> errors = new ArrayList<>();
    for (FieldError error : ex.getBindingResult().getFieldErrors()) {
      errors.add(error.getField() + ": " + error.getDefaultMessage());
    }

    String message = "폼 데이터 바인딩 실패: " + String.join(", ", errors);
    return errorResponseEntity(message, HttpStatus.BAD_REQUEST);
  }

  /**
   * Bean Validation API 제약 조건 위반 예외 처리
   * 
   * @param ex ConstraintViolationException 인스턴스
   * @return 400 Bad Request 응답
   */
  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ApiResponse<?>> handleConstraintViolationException(ConstraintViolationException ex) {
    log.error("제약 조건 위반 예외: ", ex);

    List<String> errors = ex.getConstraintViolations()
        .stream()
        .map(ConstraintViolation::getMessage)
        .collect(Collectors.toList());

    String message = "데이터 제약 조건 위반: " + String.join(", ", errors);
    return errorResponseEntity(message, HttpStatus.BAD_REQUEST);
  }

  /**
   * 메서드 인수 타입 불일치 예외 처리
   * 
   * @param ex MethodArgumentTypeMismatchException 인스턴스
   * @return 400 Bad Request 응답
   */
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ApiResponse<?>> handleMethodArgumentTypeMismatchException(
      MethodArgumentTypeMismatchException ex) {
    log.error("메서드 인수 타입 불일치 예외: ", ex);

    Class<?> requiredType = ex.getRequiredType();
    String typeName = requiredType != null ? requiredType.getSimpleName() : "알 수 없음";

    String message = String.format("잘못된 파라미터 타입: %s는 %s 타입이어야 합니다",
        ex.getName(), typeName);
    return errorResponseEntity(message, HttpStatus.BAD_REQUEST);
  }

  /**
   * 핸들러를 찾을 수 없는 예외 처리 (404 Not Found)
   * 
   * @param ex NoHandlerFoundException 인스턴스
   * @return 404 Not Found 응답
   */
  @ExceptionHandler(NoHandlerFoundException.class)
  public ResponseEntity<ApiResponse<?>> handleNoHandlerFoundException(NoHandlerFoundException ex) {
    log.error("핸들러를 찾을 수 없는 예외: ", ex);

    String message = String.format("요청된 리소스를 찾을 수 없습니다: %s %s",
        ex.getHttpMethod(), ex.getRequestURL());
    return errorResponseEntity(message, HttpStatus.NOT_FOUND);
  }

  /**
   * HS Code 분석 관련 예외 처리
   * 
   * @param ex HsCodeAnalysisException 인스턴스
   * @return 500 Internal Server Error 응답
   */
  @ExceptionHandler(HsCodeAnalysisException.class)
  public ResponseEntity<ApiResponse<?>> handleHsCodeAnalysisException(HsCodeAnalysisException ex) {
    log.error("HS Code 분석 예외: ", ex);
    return errorResponseEntity("HS Code 분석 실패: " + ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
  }

  /**
   * 외부 API 호출 실패 예외 처리
   * 
   * @param ex ExternalApiException 인스턴스
   * @return 503 Service Unavailable 응답
   */
  @ExceptionHandler(ExternalApiException.class)
  public ResponseEntity<ApiResponse<?>> handleExternalApiException(ExternalApiException ex) {
    log.error("외부 API 예외: ", ex);
    return errorResponseEntity("외부 서비스 일시적 오류: " + ex.getMessage(), HttpStatus.SERVICE_UNAVAILABLE);
  }

  /**
   * 모니터링 관련 예외 처리
   * 
   * @param ex MonitoringException 인스턴스
   * @return 500 Internal Server Error 응답
   */
  @ExceptionHandler(MonitoringException.class)
  public ResponseEntity<ApiResponse<?>> handleMonitoringException(MonitoringException ex) {
    log.error("모니터링 예외: ", ex);
    return errorResponseEntity("모니터링 서비스 오류: " + ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
  }

  /**
   * 인증 실패 예외 처리 (v2.2 보안 정책 적용)
   * 
   * <p>
   * v2.2 보안 정책에 따라 구체적인 인증 실패 이유를 노출하지 않고
   * 일반화된 메시지로 응답하여 브루트 포스 공격 방지
   * 
   * @param ex BadCredentialsException 인스턴스
   * @return 401 Unauthorized 응답
   */
  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<ApiResponse<?>> handleBadCredentialsException(BadCredentialsException ex) {
    log.error("인증 실패 (v2.2 보안 정책 적용): ", ex);
    // v2.2 보안 정책: 구체적인 실패 이유 노출 금지
    return errorResponseEntity("인증 실패", HttpStatus.UNAUTHORIZED);
  }

  /**
   * 일반 인증 예외 처리 (v2.2 보안 정책 적용)
   * 
   * @param ex AuthenticationException 인스턴스
   * @return 401 Unauthorized 응답
   */
  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ApiResponse<?>> handleAuthenticationException(AuthenticationException ex) {
    log.error("인증 예외 (v2.2 보안 정책 적용): ", ex);
    // v2.2 보안 정책: 일반화된 인증 오류 메시지
    return errorResponseEntity("인증 오류", HttpStatus.UNAUTHORIZED);
  }

  /**
   * 접근 권한 거부 예외 처리 (v2.2 보안 정책 적용)
   * 
   * @param ex AccessDeniedException 인스턴스
   * @return 403 Forbidden 응답
   */
  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiResponse<?>> handleAccessDeniedException(AccessDeniedException ex) {
    log.error("접근 권한 거부 (v2.2 보안 정책 적용): ", ex);
    // v2.2 보안 정책: 권한 정보 노출 방지
    return errorResponseEntity("접근 권한 없음", HttpStatus.FORBIDDEN);
  }

  /**
   * 일반적인 런타임 예외 처리
   * 
   * @param ex RuntimeException 인스턴스
   * @return 500 Internal Server Error 응답
   */
  @ExceptionHandler(RuntimeException.class)
  public ResponseEntity<ApiResponse<?>> handleRuntimeException(RuntimeException ex) {
    log.error("런타임 예외: ", ex);
    return errorResponseEntity("서버 내부 오류가 발생했습니다: " + ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
  }

  /**
   * 예상치 못한 모든 예외에 대한 최종 처리
   * 
   * @param ex Exception 인스턴스
   * @return 500 Internal Server Error 응답
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<?>> handleGeneralException(Exception ex) {
    log.error("예상치 못한 예외: ", ex);
    return errorResponseEntity("예상치 못한 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR);
  }
}