package com.tradegenie.platform.tradegenie_backend_api.dto.external;

import java.util.List;

/**
 * 관세청 무역통계 DTO
 */
public record TradeStatisticsDto(
    String hsCode,
    String productName,
    String period,
    List<CountryTradeDto> countryStatistics,
    TotalTradeDto totalTrade) {
  // 검증을 위한 compact canonical constructor
  public TradeStatisticsDto {
    if (hsCode == null || hsCode.trim().isEmpty()) {
      throw new IllegalArgumentException("HS Code는 필수입니다");
    }
    if (productName == null || productName.trim().isEmpty()) {
      throw new IllegalArgumentException("제품명은 필수입니다");
    }
    if (period == null || period.trim().isEmpty()) {
      throw new IllegalArgumentException("조회 기간은 필수입니다");
    }
  }
}