package com.hscoderadar.controller;

import com.hscoderadar.common.response.ApiResponseMessage;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 애플리케이션의 기본 홈 및 상태 확인 API를 제공하는 컨트롤러
 * 
 * <p>
 * 이 컨트롤러는 서버 상태 확인과 기본 홈 페이지 응답을 담당합니다.
 * 모든 응답은 자동으로 {@link com.hscoderadar.common.response.ApiResponse}로 래핑됩니다.
 * 
 * @author Development Team
 * @since 1.0.0
 * @see com.hscoderadar.common.response.ResponseWrapperAdvice
 */
@RestController
public class HomeController {

  /**
   * 홈 페이지 기본 응답을 반환합니다
   * 
   * <p>
   * 애플리케이션의 기본 홈 페이지 메시지를 반환하며,
   * 클라이언트가 서버에 정상적으로 연결되었는지 확인할 수 있습니다.
   * 
   * @return 홈 페이지 환영 메시지
   * 
   *         <h3>API 호출 예시:</h3>
   * 
   *         <pre>
   * GET /api/
   * 응답: {
   *   "success": "SUCCESS",
   *   "message": "홈 페이지 로드 성공",
   *   "data": "Hello, HS Code Radar"
   * }
   *         </pre>
   */
  @GetMapping("/")
  @ApiResponseMessage("홈 페이지 로드 성공")
  public String home() {
    return "Hello, HS Code Radar";
  }

  /**
   * 서버 상태를 확인하는 헬스 체크 엔드포인트
   * 
   * <p>
   * 서버가 정상적으로 작동 중인지 확인할 수 있는 상태 체크 API입니다.
   * 로드 밸런서나 모니터링 시스템에서 사용할 수 있습니다.
   * 
   * @return 서버 상태 메시지
   * 
   *         <h3>API 호출 예시:</h3>
   * 
   *         <pre>
   * GET /api/status
   * 응답: {
   *   "success": "SUCCESS",
   *   "message": "서버 상태 확인 완료",
   *   "data": "서버가 정상적으로 작동 중입니다."
   * }
   *         </pre>
   */
  @GetMapping("/status")
  @ApiResponseMessage("서버 상태 확인 완료")
  public String status() {
    return "서버가 정상적으로 작동 중입니다.";
  }
}