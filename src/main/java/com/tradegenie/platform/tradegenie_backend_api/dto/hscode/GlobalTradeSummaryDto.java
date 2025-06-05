package com.tradegenie.platform.tradegenie_backend_api.dto.hscode;

import java.math.BigDecimal;

/**
 * 글로벌 무역 요약 DTO
 */
public record GlobalTradeSummaryDto(
    BigDecimal totalGlobalExport,
    BigDecimal totalGlobalImport,
    BigDecimal averageUnitPrice,
    Integer tradingCountryCount) {
  // 검증을 위한 compact canonical constructor
  public GlobalTradeSummaryDto {
    // 모든 필드가 nullable하므로 별도 검증 없음
  }
}