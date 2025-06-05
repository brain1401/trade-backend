package com.tradegenie.platform.tradegenie_backend_api.dto.external;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 관세청 환율 정보 DTO
 */
public record ExchangeRateDto(
    String currencyCode,
    String currencyName,
    BigDecimal baseRate,
    BigDecimal buyRate,
    BigDecimal sellRate,
    BigDecimal changeRate,
    LocalDate announcementDate) {
  // 검증을 위한 compact canonical constructor
  public ExchangeRateDto {
    if (currencyCode == null || currencyCode.trim().isEmpty()) {
      throw new IllegalArgumentException("통화 코드는 필수입니다");
    }
    if (currencyName == null || currencyName.trim().isEmpty()) {
      throw new IllegalArgumentException("통화명은 필수입니다");
    }
    if (baseRate == null || baseRate.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("기준 환율은 0보다 큰 값이어야 합니다");
    }
  }
}