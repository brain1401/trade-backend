package com.hscoderadar.common.response;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 자동 ApiResponse 래핑을 건너뛰는 어노테이션
 * 
 * <p>
 * 특정 컨트롤러 메서드에서 ResponseWrapperAdvice의 자동 래핑을 비활성화하고 싶을 때 사용합니다.
 * 파일 다운로드, 스트리밍 응답, 또는 이미 완전한 응답 형태인 경우에 유용합니다.
 * 
 * <h3>사용 예시:</h3>
 * 
 * <pre>
 * {@code
 * &#64;GetMapping("/download/{fileId}")
 * @NoApiResponseWrap
 * public ResponseEntity<Resource> downloadFile(@PathVariable String fileId) {
 *     // 파일 리소스를 직접 반환 (ApiResponse로 래핑하지 않음)
 *     return ResponseEntity.ok()
 *         .contentType(MediaType.APPLICATION_OCTET_STREAM)
 *         .body(fileResource);
 * }
 * }
 * </pre>
 * 
 * @author Development Team
 * @since 1.0.0
 * @see ResponseWrapperAdvice
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface NoApiResponseWrap {
}