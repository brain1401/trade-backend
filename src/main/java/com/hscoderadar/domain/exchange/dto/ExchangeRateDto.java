package com.hscoderadar.domain.exchange.dto;

import com.hscoderadar.domain.exchange.entity.ExchangeRatesCache;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Schema(description = "환율 정보 응답 DTO")
public class ExchangeRateDto {

    private final String currencyCode;
    private final String currencyName;
    private final BigDecimal exchangeRate;
    private final BigDecimal changeRate; // 뺄지 말지 얘기해봐야함
    private final LocalDateTime lastUpdated;

    @Builder
    private ExchangeRateDto(String currencyCode, String currencyName, BigDecimal exchangeRate, BigDecimal changeRate, LocalDateTime lastUpdated) {
        this.currencyCode = currencyCode;
        this.currencyName = currencyName;
        this.exchangeRate = exchangeRate;
        this.changeRate = changeRate;
        this.lastUpdated = lastUpdated;
    }
    /**
     * ExchangeRatesCache 엔티티를 DTO로 변환
     */
    public static ExchangeRateDto from(ExchangeRatesCache entity) {
        return ExchangeRateDto.builder()
                .currencyCode(entity.getCurrencyCode())
                .currencyName(entity.getCurrencyName())
                .exchangeRate(entity.getExchangeRate())
                .changeRate(entity.getChangeRate()) // DB에 저장된 값 사용
                .lastUpdated(entity.getFetchedAt())
                .build();
    }

    /**
     * [수정] CustomsExchangeRateResponse.Item을 DTO로 변환하는 메서드
     * @param item 관세청 API 응답의 개별 환율 아이템
     * @return ExchangeRateDto 객체
     */
    public static ExchangeRateDto from(CustomsExchangeRateResponse.Item item) {
        BigDecimal rate = new BigDecimal(item.getExchangeRate().replace(",", ""));
        LocalDateTime updatedTime = LocalDateTime.now(); // API 응답에는 시간이 없으므로 조회 시점으로 설정

        return ExchangeRateDto.builder()
                .currencyCode(item.getCurrencyCode())
                .currencyName(item.getCurrencyName())
                .exchangeRate(rate)
                .changeRate(BigDecimal.ZERO) // 새 API에는 변동률 정보가 없으므로 0으로 설정
                .lastUpdated(updatedTime)
                .build();
    }
}