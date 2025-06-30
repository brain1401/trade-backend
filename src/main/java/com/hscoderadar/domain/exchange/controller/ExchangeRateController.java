package com.hscoderadar.domain.exchange.controller;

import com.hscoderadar.common.response.ApiResponseMessage;
import com.hscoderadar.domain.exchange.dto.ExchangeRateDto;
import com.hscoderadar.domain.exchange.service.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/exchange-rates")
@RequiredArgsConstructor
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    @GetMapping
    @ApiResponseMessage("환율 정보가 조회되었습니다.")
    public Mono<List<ExchangeRateDto>> getExchangeRates() {
        return exchangeRateService.getLatestExchangeRates();
    }

    /**
     * 특정 국가(통화 코드)의 환율 정보 조회 엔드포인트
     * @param currencyCode 국가를 식별하는 통화 코드 (예: USD, JPY)
     * @return 특정 국가의 환율 정보 Mono 객체
     */
    @GetMapping("/{currencyCode}")
    @ApiResponseMessage("특정 국가의 환율 정보가 조회되었습니다.")
    public Mono<List<ExchangeRateDto>> getExchangeRateByCurrency(@PathVariable String currencyCode) {
        return exchangeRateService.getExchangeRateByCurrency(currencyCode);
    }
}