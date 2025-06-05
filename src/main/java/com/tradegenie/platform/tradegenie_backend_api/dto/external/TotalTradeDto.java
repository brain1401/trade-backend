package com.tradegenie.platform.tradegenie_backend_api.dto.external;

import java.math.BigDecimal;

/**
 * 전체 무역통계 DTO
 */
public record TotalTradeDto(
    BigDecimal totalExportAmount,
    BigDecimal totalImportAmount,
    BigDecimal totalExportWeight,
    BigDecimal totalImportWeight,
    Integer exportCountryCount,
    Integer importCountryCount) {
  // 검증을 위한 compact canonical constructor
  public TotalTradeDto {
    // 모든 필드가 nullable하므로 별도 검증 없음
  }
}