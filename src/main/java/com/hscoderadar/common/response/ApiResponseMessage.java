package com.hscoderadar.common.response;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * API 응답 메시지를 커스터마이징하기 위한 어노테이션
 *
 * <p>컨트롤러 메서드에 이 어노테이션을 적용하면 기본 성공 메시지 대신 지정된 메시지를 사용하여 ApiResponse를 생성합니다.
 *
 * <h3>사용 예시:</h3>
 *
 * <pre>{@code
 * @GetMapping("/users")
 * @ApiResponseMessage("사용자 목록을 성공적으로 조회했습니다")
 * public List<User> getUsers() {
 *   return userService.getAllUsers();
 * }
 * }</pre>
 *
 * @author Development Team
 * @since 1.0.0
 * @see ResponseWrapperAdvice
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiResponseMessage {

  /**
   * 성공 응답 시 사용할 메시지
   *
   * <p>이 값은 필수이며 빈 문자열이나 null이 될 수 없습니다.
   *
   * @return 응답 메시지 (빈 문자열 불가)
   */
  String value();
}
