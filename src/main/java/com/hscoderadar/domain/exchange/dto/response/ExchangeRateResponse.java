package com.hscoderadar.domain.exchange.dto.response;

import com.hscoderadar.domain.exchange.entity.ExchangeRatesCache;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "환율 정보 응답 DTO")
public record ExchangeRateResponse(
    String currencyCode,
    String currencyName,
    BigDecimal exchangeRate,
    BigDecimal changeRate,
    LocalDateTime lastUpdated) {

  public static ExchangeRateResponse from(ExchangeRatesCache entity) {
    return new ExchangeRateResponse(
        entity.getCurrencyCode(),
        entity.getCurrencyName(),
        entity.getExchangeRate(),
        entity.getChangeRate(),
        entity.getFetchedAt());
  }

  public static ExchangeRateResponse from(CustomsExchangeRateResponse.Item item) {
    BigDecimal rate = new BigDecimal(item.exchangeRate().replace(",", ""));
    LocalDateTime updatedTime = LocalDateTime.now();

    return new ExchangeRateResponse(
        item.currencyCode(),
        item.currencyName(),
        rate,
        BigDecimal.ZERO,
        updatedTime);
  }
}