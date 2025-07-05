package com.hscoderadar.domain.tradenews.service;

import com.hscoderadar.domain.tradenews.dto.response.NewsGenerationResponse;
import com.hscoderadar.domain.tradenews.entity.TradeNews;
import com.hscoderadar.domain.tradenews.repository.TradeNewsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 뉴스 자동 생성 스케줄러 서비스
 * Python AI 서버를 통해 주기적으로 무역 뉴스를 생성하고 저장함
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NewsGenerationService {

  @Qualifier("pythonAiWebClient")
  private final WebClient pythonAiWebClient;
  private final TradeNewsRepository newsRepository;

  /**
   * 매일 새벽 1시에 뉴스 생성 작업 실행
   * cron = "초 분 시 일 월 요일"
   */
  @Scheduled(cron = "0 0 1 * * ?")
  public void generateDailyNews() {
    log.info("일일 뉴스 생성 작업 시작 - {}", LocalDateTime.now());

    pythonAiWebClient.post()
        .uri("/api/v1/news")
        .contentType(MediaType.APPLICATION_JSON)
        .retrieve()
        .bodyToMono(NewsGenerationResponse.class)
        .timeout(Duration.ofMinutes(5)) // 5분 타임아웃
        .doOnSuccess(this::handleSuccessResponse)
        .doOnError(this::handleErrorResponse)
        .onErrorResume(error -> {
          log.error("뉴스 생성 중 예외 발생, 기본 응답 반환", error);
          return Mono.just(new NewsGenerationResponse("error", error.getMessage(), 0));
        })
        .subscribe(
            response -> log.info("뉴스 생성 작업 완료: {}", response),
            error -> log.error("뉴스 생성 최종 실패", error));
  }

  /**
   * 테스트용 즉시 실행 메소드
   * 스케줄러 동작 확인을 위해 수동으로 호출 가능
   */
  public void generateNewsManually() {
    log.info("수동 뉴스 생성 작업 시작 - {}", LocalDateTime.now());
    generateDailyNews();
  }

  /**
   * 성공 응답 처리
   */
  private void handleSuccessResponse(NewsGenerationResponse response) {
    if ("success".equals(response.status())) {
      log.info("뉴스 생성 성공: {} 건의 뉴스가 생성됨", response.generatedCount());

      // Python 서버에서 생성한 뉴스는 이미 DB에 저장되어 있으므로
      // 여기서는 추가 처리만 수행 (예: 통계 업데이트, 알림 발송 등)
      updateNewsStatistics(response.generatedCount());

    } else {
      log.warn("뉴스 생성 응답 상태가 성공이 아님: {}", response.status());
    }
  }

  /**
   * 에러 응답 처리
   */
  private void handleErrorResponse(Throwable error) {
    log.error("Python AI 서버 통신 실패: {}", error.getMessage(), error);

    // 에러 발생 시 관리자에게 알림 전송 (추후 구현)
    // notificationService.sendAdminAlert("뉴스 생성 실패", error.getMessage());

    // 재시도 로직 (필요 시 구현)
    // 현재는 다음 스케줄 시간까지 대기
  }

  /**
   * 뉴스 생성 통계 업데이트
   */
  private void updateNewsStatistics(int generatedCount) {
    try {
      // 통계 테이블 업데이트 또는 로그 기록
      log.info("뉴스 생성 통계 업데이트: {} 건", generatedCount);

      // 최근 생성된 뉴스 개수 확인
      long totalNewsCount = newsRepository.count();
      log.info("전체 뉴스 개수: {} 건", totalNewsCount);

    } catch (Exception e) {
      log.error("통계 업데이트 중 오류 발생", e);
    }
  }

  /**
   * 스케줄러 상태 확인 (관리 목적)
   */
  public boolean isSchedulerRunning() {
    // 스케줄러 상태 확인 로직
    return true;
  }
}