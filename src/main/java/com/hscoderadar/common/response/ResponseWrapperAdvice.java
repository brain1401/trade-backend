package com.hscoderadar.common.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import java.lang.reflect.Method;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 모든 컨트롤러 응답을 자동으로 ApiResponse로 감싸는 응답 어드바이스
 * 
 * <p>
 * 이 클래스는 Spring의 {@link ResponseBodyAdvice}를 구현하여 컨트롤러에서 반환하는
 * 모든 응답을 자동으로 {@link ApiResponse} 형태로 감싸서 개발자가 매번 ResponseEntity를
 * 만들 필요가 없도록 합니다.
 * 
 * <h3>동작 원리:</h3>
 * <ol>
 * <li>컨트롤러 메서드 실행 완료 후 {@link #supports} 메서드로 래핑 여부 결정</li>
 * <li>래핑이 필요한 경우 {@link #beforeBodyWrite} 메서드에서 실제 래핑 수행</li>
 * <li>{@link ApiResponseMessage} 어노테이션 확인하여 커스텀 메시지 적용</li>
 * <li>최종적으로 표준 {@link ApiResponse} 형태로 클라이언트에 응답</li>
 * <li>JSON Content-Type 자동 설정 및 순환 참조 방지</li>
 * </ol>
 * 
 * <h3>지원하는 기능:</h3>
 * <ul>
 * <li><strong>자동 래핑</strong>: 일반 데이터를 SUCCESS 상태로 자동 감싸기</li>
 * <li><strong>커스텀 메시지</strong>: {@link ApiResponseMessage} 어노테이션으로 메시지 지정</li>
 * <li><strong>선택적 제외</strong>: {@link NoApiResponseWrap} 어노테이션으로 래핑 건너뛰기</li>
 * <li><strong>기존 호환성</strong>: 이미 ApiResponse인 경우 그대로 반환</li>
 * <li><strong>예외 처리 분리</strong>: GlobalExceptionHandler에서 처리되는 오류 응답 제외</li>
 * <li><strong>Content-Type 보장</strong>: JSON 응답의 올바른 Content-Type 설정</li>
 * <li><strong>순환 참조 방지</strong>: ResponseEntity 내부의 ApiResponse 감지</li>
 * </ul>
 * 
 * <h3>사용 예시:</h3>
 * 
 * <pre>
 * {@code
 * // 기본 자동 래핑
 * &#64;GetMapping("/users")
 * public List<User> getUsers() {
 *   return userService.getAllUsers();
 * }
 * // 응답: {"success":"SUCCESS", "message":"요청이 성공적으로 처리되었습니다", "data":[...]}
 * 
 * // 커스텀 메시지
 * &#64;GetMapping("/users")
 * &#64;ApiResponseMessage("사용자 목록 조회 완료")
 * public List<User> getUsers() {
 *   return userService.getAllUsers();
 * }
 * // 응답: {"success":"SUCCESS", "message":"사용자 목록 조회 완료", "data":[...]}
 * 
 * // 래핑 제외
 * &#64;GetMapping("/download")
 * @NoApiResponseWrap
 * public ResponseEntity<Resource> downloadFile() {
 *   return ResponseEntity.ok().body(resource);
 * }
 * // 응답: 원본 ResponseEntity 그대로 반환
 * }
 * </pre>
 * 
 * @author Development Team
 * @since 1.0.0
 * @see ApiResponse
 * @see ApiResponseMessage
 * @see NoApiResponseWrap
 * @see com.hscoderadar.common.exception.GlobalExceptionHandler
 */
@RestControllerAdvice
@Slf4j
public class ResponseWrapperAdvice implements ResponseBodyAdvice<Object> {

  @Autowired
  private ObjectMapper objectMapper;

  /**
   * 응답을 래핑할지 결정하는 메서드
   * 
   * <p>
   * 다음 조건에서 래핑을 수행합니다:
   * <ul>
   * <li>{@link NoApiResponseWrap} 어노테이션이 없는 경우</li>
   * <li>반환 타입이 {@link ApiResponse}가 아닌 경우</li>
   * <li>반환 타입이 {@code ResponseEntity<ApiResponse<?>>}가 아닌 경우</li>
   * </ul>
   * 
   * <p>
   * 다음 조건에서는 래핑하지 않습니다:
   * <ul>
   * <li>{@link NoApiResponseWrap} 어노테이션이 메서드에 있는 경우</li>
   * <li>이미 {@link ApiResponse} 타입인 경우</li>
   * <li>예외 처리 응답 (GlobalExceptionHandler가 처리)</li>
   * <li>{@code ResponseEntity<ApiResponse<?>>} 타입인 경우</li>
   * </ul>
   * 
   * @param returnType    컨트롤러 메서드의 반환 타입 정보
   * @param converterType 사용할 HTTP 메시지 컨버터 타입
   * @return 래핑 여부 (true: 래핑함, false: 래핑하지 않음)
   * 
   * @since 1.0.0
   */
  @Override
  public boolean supports(@NonNull MethodParameter returnType,
      @NonNull Class<? extends HttpMessageConverter<?>> converterType) {

    // 메서드에 NoApiResponseWrap 어노테이션이 있으면 래핑하지 않음
    if (returnType.hasMethodAnnotation(NoApiResponseWrap.class)) {
      Method method = returnType.getMethod();
      log.debug("Skipping response wrapping due to @NoApiResponseWrap annotation on method: {}",
          method != null ? method.getName() : "unknown");
      return false;
    }

    // 컨트롤러 클래스에 NoApiResponseWrap 어노테이션이 있으면 래핑하지 않음
    if (returnType.getDeclaringClass().isAnnotationPresent(NoApiResponseWrap.class)) {
      log.debug("Skipping response wrapping due to @NoApiResponseWrap annotation on class: {}",
          returnType.getDeclaringClass().getSimpleName());
      return false;
    }

    // 이미 ApiResponse 타입이면 래핑하지 않음
    if (ApiResponse.class.isAssignableFrom(returnType.getParameterType())) {
      log.debug("Skipping response wrapping for ApiResponse type: {}", returnType.getParameterType());
      return false;
    }

    // ResponseEntity<ApiResponse<?>> 타입이면 래핑하지 않음 (GlobalExceptionHandler에서 사용)
    if (ResponseEntity.class.isAssignableFrom(returnType.getParameterType())) {
      // ResponseEntity의 제네릭 타입을 확인
      if (returnType.getGenericParameterType().getTypeName().contains("ApiResponse")) {
        log.debug("Skipping response wrapping for ResponseEntity<ApiResponse<?>> type");
        return false;
      }
    }

    log.debug("Response wrapping will be applied for return type: {}", returnType.getParameterType());
    return true;
  }

  /**
   * 실제 응답 래핑을 수행하는 메서드
   * 
   * <p>
   * 이 메서드는 컨트롤러에서 반환된 데이터를 다음 단계에 따라 처리합니다:
   * <ol>
   * <li>이미 {@link ApiResponse} 타입인지 확인 (그대로 반환)</li>
   * <li>{@code ResponseEntity<ApiResponse<?>>} 타입인지 확인 (그대로 반환)</li>
   * <li>{@link ApiResponseMessage} 어노테이션에서 커스텀 메시지 추출</li>
   * <li>기본 메시지 또는 커스텀 메시지로 SUCCESS 응답 생성</li>
   * <li>JSON Content-Type 설정</li>
   * </ol>
   * 
   * <p>
   * <strong>주의사항:</strong> 이 메서드는 {@link #supports} 메서드에서 true를 반환한 경우에만 호출됩니다.
   * 
   * @param body                  컨트롤러에서 반환한 원본 데이터 (null 가능)
   * @param returnType            컨트롤러 메서드의 반환 타입 정보
   * @param selectedContentType   선택된 콘텐츠 타입
   * @param selectedConverterType 선택된 HTTP 메시지 컨버터 타입
   * @param request               HTTP 요청 객체
   * @param response              HTTP 응답 객체
   * @return {@link ApiResponse}로 래핑된 응답 데이터, 또는 이미 ApiResponse인 경우 원본 반환
   * 
   * @example 기본 자동 래핑
   * 
   *          <pre>
   *          // Controller: return "Hello World";
   *          // 실제 응답: {"success": "SUCCESS", "message": "요청이 성공적으로 처리되었습니다", "data":
   *          // "Hello World"}
   *          </pre>
   * 
   * @example 커스텀 메시지 사용
   * 
   *          <pre>
   *          // Controller with @ApiResponseMessage("사용자 생성 완료"): return user;
   *          // 실제 응답: {"success": "SUCCESS", "message": "사용자 생성 완료", "data": {...}}
   *          </pre>
   * 
   * @example null 데이터 처리
   * 
   *          <pre>
   *          // Controller: return null;
   *          // 실제 응답: {"success": "SUCCESS", "message": "요청이 성공적으로 처리되었습니다", "data":
   *          // null}
   *          </pre>
   * 
   * @since 1.0.0
   */
  @Override
  public Object beforeBodyWrite(
      @Nullable Object body,
      @NonNull MethodParameter returnType,
      @NonNull MediaType selectedContentType,
      @NonNull Class<? extends HttpMessageConverter<?>> selectedConverterType,
      @NonNull ServerHttpRequest request,
      @NonNull ServerHttpResponse response) {

    // 이중 래핑 방지: 이미 ApiResponse인 경우 그대로 반환
    if (body instanceof ApiResponse) {
      log.debug("Body is already ApiResponse type, returning as-is");
      setJsonContentTypeIfNeeded(response, selectedContentType);
      return body;
    }

    // ResponseEntity<ApiResponse<?>> 처리 (GlobalExceptionHandler에서 오는 응답)
    if (body instanceof ResponseEntity) {
      ResponseEntity<?> responseEntity = (ResponseEntity<?>) body;
      if (responseEntity.getBody() instanceof ApiResponse) {
        log.debug("Body is ResponseEntity<ApiResponse<?>>, returning as-is");
        return body;
      }
    }

    // 커스텀 메시지 확인
    String message = "요청이 성공적으로 처리되었습니다";
    ApiResponseMessage annotation = returnType.getMethodAnnotation(ApiResponseMessage.class);
    if (annotation != null && !annotation.value().trim().isEmpty()) {
      message = annotation.value().trim();
      log.debug("Using custom message from @ApiResponseMessage: {}", message);
    }

    // JSON Content-Type 설정
    setJsonContentTypeIfNeeded(response, selectedContentType);

    // ApiResponse로 래핑하여 반환
    ApiResponse<Object> wrappedResponse = ApiResponse.success(message, body);
    log.debug("Response successfully wrapped with ApiResponse. Message: {}", message);

    // String 타입의 경우 특별 처리 (ClassCastException 방지)
    if (body instanceof String || selectedConverterType.equals(StringHttpMessageConverter.class)) {
      try {
        // JSON으로 직렬화하여 반환
        String jsonResponse = objectMapper.writeValueAsString(wrappedResponse);
        log.debug("String response converted to JSON: {}", jsonResponse);
        return jsonResponse;
      } catch (Exception e) {
        log.error("Failed to serialize ApiResponse to JSON string", e);
        return wrappedResponse;
      }
    }

    return wrappedResponse;
  }

  /**
   * JSON 응답을 위한 Content-Type을 설정합니다
   * 
   * @param response            HTTP 응답 객체
   * @param selectedContentType 선택된 콘텐츠 타입
   */
  private void setJsonContentTypeIfNeeded(ServerHttpResponse response, MediaType selectedContentType) {
    // Content-Type이 설정되지 않았거나 application/json이 아닌 경우 설정
    if (selectedContentType == null || !MediaType.APPLICATION_JSON.isCompatibleWith(selectedContentType)) {
      response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
      log.debug("Content-Type set to application/json for API response");
    }
  }
}