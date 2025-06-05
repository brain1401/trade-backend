package com.tradegenie.platform.tradegenie_backend_api.dto.hscode;

import java.math.BigDecimal;

/**
 * 글로벌 무역 파트너 DTO
 */
public record GlobalTradePartnerDto(
    String countryCode,
    String countryName,
    BigDecimal tradeValue,
    BigDecimal marketShare,
    Integer rank) {
  // 검증을 위한 compact canonical constructor
  public GlobalTradePartnerDto {
    if (countryCode == null || countryCode.trim().isEmpty()) {
      throw new IllegalArgumentException("국가 코드는 필수입니다");
    }
    if (countryName == null || countryName.trim().isEmpty()) {
      throw new IllegalArgumentException("국가명은 필수입니다");
    }
  }
}