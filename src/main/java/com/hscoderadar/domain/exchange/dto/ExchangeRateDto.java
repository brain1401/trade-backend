package com.hscoderadar.domain.exchange.dto;

import com.hscoderadar.domain.exchange.entity.ExchangeRatesCache;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "환율 정보 응답 DTO")
public record ExchangeRateDto(
    String currencyCode,
    String currencyName,
    BigDecimal exchangeRate,
    BigDecimal changeRate, // 뺄지 말지 얘기해봐야함
    LocalDateTime lastUpdated) {

  /** ExchangeRatesCache 엔티티를 DTO로 변환. */
  public static ExchangeRateDto from(ExchangeRatesCache entity) {
    return new ExchangeRateDto(
        entity.getCurrencyCode(),
        entity.getCurrencyName(),
        entity.getExchangeRate(),
        entity.getChangeRate(), // DB에 저장된 값 사용
        entity.getFetchedAt());
  }

  /**
   * [수정] CustomsExchangeRateResponse.Item을 DTO로 변환하는 메서드
   *
   * @param item 관세청 API 응답의 개별 환율 아이템
   * @return ExchangeRateDto 객체
   */
  public static ExchangeRateDto from(CustomsExchangeRateResponse.Item item) {
    BigDecimal rate = new BigDecimal(item.exchangeRate().replace(",", ""));
    LocalDateTime updatedTime = LocalDateTime.now(); // API 응답에는 시간이 없으므로 조회 시점으로 설정

    return new ExchangeRateDto(
        item.currencyCode(),
        item.currencyName(),
        rate,
        BigDecimal.ZERO, // 새 API에는 변동률 정보가 없으므로 0으로 설정
        updatedTime);
  }
}