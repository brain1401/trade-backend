package com.hscoderadar.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 애플리케이션 상태 확인 컨트롤러
 * 시스템 및 외부 서비스 연결 상태 모니터링
 */
@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
@Slf4j
public class HealthController {

  @Qualifier("pythonAiWebClient")
  private final WebClient pythonAiWebClient;

  @Value("${ai.python.server.url}")
  private String pythonServerUrl;

  /**
   * 기본 헬스체크
   */
  @GetMapping
  public ResponseEntity<Map<String, Object>> health() {
    Map<String, Object> response = new HashMap<>();
    response.put("status", "UP");
    response.put("timestamp", System.currentTimeMillis());
    response.put("application", "HSCodeRadar");
    response.put("version", "6.1");

    return ResponseEntity.ok(response);
  }

  /**
   * 상세 시스템 상태 확인 (Python AI 서버 포함)
   */
  @GetMapping("/detailed")
  public ResponseEntity<Map<String, Object>> detailedHealth() {
    Map<String, Object> response = new HashMap<>();
    Map<String, Object> services = new HashMap<>();

    // Python AI 서버 연결 확인
    boolean pythonServerHealthy = checkPythonServerHealth();
    services.put("python-ai-server", Map.of(
        "status", pythonServerHealthy ? "UP" : "DOWN",
        "url", pythonServerUrl,
        "description", "Python AI 서버 연결 상태"));

    // 전체 상태 결정
    boolean overallHealthy = pythonServerHealthy;

    response.put("status", overallHealthy ? "UP" : "DOWN");
    response.put("timestamp", System.currentTimeMillis());
    response.put("services", services);

    return ResponseEntity.ok(response);
  }

  /**
   * Python AI 서버 연결 상태 확인
   */
  private boolean checkPythonServerHealth() {
    try {
      String response = pythonAiWebClient
          .get()
          .uri("/health") // Python 서버에 헬스체크 엔드포인트가 있다고 가정
          .retrieve()
          .bodyToMono(String.class)
          .timeout(Duration.ofSeconds(5))
          .block();

      log.debug("Python 서버 헬스체크 응답: {}", response);
      return true;

    } catch (Exception e) {
      log.warn("Python 서버 헬스체크 실패: {} - {}", pythonServerUrl, e.getMessage());
      return false;
    }
  }
}
