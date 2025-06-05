package com.tradegenie.platform.tradegenie_backend_api.dto.hscode;

import java.util.List;

/**
 * UN COMTRADE 글로벌 무역 데이터 DTO
 */
public record ComtradeDataDto(
    String hsCode,
    String productName,
    String period,
    List<GlobalTradePartnerDto> topExporters,
    List<GlobalTradePartnerDto> topImporters,
    GlobalTradeSummaryDto summary) {
  // 검증을 위한 compact canonical constructor
  public ComtradeDataDto {
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
