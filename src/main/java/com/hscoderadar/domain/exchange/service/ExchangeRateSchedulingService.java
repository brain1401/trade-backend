package com.hscoderadar.domain.exchange.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExchangeRateSchedulingService {

    private final ExchangeRateService exchangeRateService;

    /**
     * 매일 새벽 1시에 환율 정보를 갱신하는 스케줄러.
     * cron 표현식: "0 0 0 * * *" (초 분 시 일 월 요일)
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void scheduleDailyExchangeRateFetch() {
        log.info("매일 환율 정보 갱신 스케줄을 시작합니다.");
        exchangeRateService.getLatestExchangeRates()
            .doOnSuccess(rates -> log.info("스케줄에 따라 {}개의 환율 정보가 성공적으로 갱신되었습니다.", rates.size()))
            .doOnError(error -> log.error("환율 정보 갱신 스케줄 중 오류가 발생했습니다.", error))
            .subscribe();
    }
}