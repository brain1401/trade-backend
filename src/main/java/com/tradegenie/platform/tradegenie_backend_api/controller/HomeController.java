package com.tradegenie.platform.tradegenie_backend_api.controller;

import com.tradegenie.platform.tradegenie_backend_api.dto.ApiResponse;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api")
public class HomeController {
  @GetMapping("/")
  public ResponseEntity<ApiResponse<String>> home() {
    try {
      String data = "Hello, TestTst";
      ApiResponse<String> response = ApiResponse.success("홈 페이지 로드 성공", data);

      return ResponseEntity.ok(response);
    } catch (Exception e) {
      throw new RuntimeException("홈 페이지 로드 중 오류 발생");
    }
  }

  @GetMapping("/status")
  public ResponseEntity<ApiResponse<String>> status() {
    try {
      String statusMessage = "서버가 정상적으로 작동 중입니다.";
      ApiResponse<String> response = ApiResponse.success("서버 상태 확인 완료", statusMessage);
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      throw new RuntimeException("서버 상태 확인 중 오류 발생");
    }
  }
}
