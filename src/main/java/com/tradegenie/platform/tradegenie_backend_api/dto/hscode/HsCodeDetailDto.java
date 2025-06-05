package com.tradegenie.platform.tradegenie_backend_api.dto.hscode;

import com.tradegenie.platform.tradegenie_backend_api.dto.external.TradeStatisticsDto;
import java.util.List;

/**
 * HS Code 상세 정보 대시보드 DTO
 */
public record HsCodeDetailDto(
    String hsCode,
    String productName,
    String description,
    List<HsCodeNewsDto> news,
    List<HsCodeRegulationDto> regulations,
    TradeStatisticsDto tradeStatistics,
    ComtradeDataDto comtradeData) {
  // 검증을 위한 compact canonical constructor
  public HsCodeDetailDto {
    if (hsCode == null || hsCode.trim().isEmpty()) {
      throw new IllegalArgumentException("HS Code는 필수입니다");
    }
    if (productName == null || productName.trim().isEmpty()) {
      throw new IllegalArgumentException("제품명은 필수입니다");
    }
  }
}