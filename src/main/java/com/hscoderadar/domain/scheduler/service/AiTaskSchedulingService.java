package com.hscoderadar.domain.scheduler.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * AI 관련 작업을 주기적으로 실행하는 통합 스케줄러 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiTaskSchedulingService {

    @Qualifier("pythonAiWebClient")
    private final WebClient pythonAiWebClient;

    /**
     * Python의 /api/v1/news/ 엔드포인트를 호출
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void scheduleDailyNewsGeneration() {
        log.info("일일 뉴스 생성 스케줄러 시작 - {}", LocalDateTime.now());

        pythonAiWebClient.post()
                .uri("/api/v1/news/") // Python 뉴스 생성 API
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofMinutes(10)) // 뉴스 생성은 오래 걸릴 수 있으므로 타임아웃 10분
                .doOnSuccess(response -> log.info("뉴스 생성 작업 성공: {}", response))
                .doOnError(error -> log.error("뉴스 생성 작업 실패", error))
                .subscribe();
    }

    /**
     * 자정에 모니터링 실시
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void scheduleBookmarkMonitoring() {
        log.info("북마크 모니터링 스케줄러 시작 - {}", LocalDateTime.now());

        pythonAiWebClient.post()
                .uri("/api/v1/monitoring/run-monitoring")
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofMinutes(5)) // 모니터링 타임아웃 5분
                .doOnSuccess(response -> log.info("모니터링 작업 성공: {}", response))
                .doOnError(error -> log.error("모니터링 작업 실패", error))
                .subscribe();
    }
}