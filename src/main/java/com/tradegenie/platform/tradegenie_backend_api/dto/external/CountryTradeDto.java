package com.tradegenie.platform.tradegenie_backend_api.dto.external;

import java.math.BigDecimal;

/**
 * 국가별 무역통계 DTO
 */
public record CountryTradeDto(
    String countryCode,
    String countryName,
    BigDecimal exportAmount,
    BigDecimal importAmount,
    BigDecimal exportWeight,
    BigDecimal importWeight,
    Integer exportRank,
    Integer importRank) {
  // 검증을 위한 compact canonical constructor
  public CountryTradeDto {
    if (countryCode == null || countryCode.trim().isEmpty()) {
      throw new IllegalArgumentException("국가 코드는 필수입니다");
    }
    if (countryName == null || countryName.trim().isEmpty()) {
      throw new IllegalArgumentException("국가명은 필수입니다");
    }
  }
}