package com.hscoderadar.domain.tradenews.dto.response;

import com.hscoderadar.domain.tradenews.entity.TradeNews;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "뉴스 정보 응답 DTO")
public record TradeNewsResponse(
    Long id,
    String title,
    String summary,
    String sourceName,
    String sourceUrl,
    LocalDateTime publishedAt,
    String category) {

  public static TradeNewsResponse from(TradeNews entity) {
    return new TradeNewsResponse(
        entity.getId(),
        entity.getTitle(),
        entity.getSummary(),
        entity.getSourceName(),
        entity.getSourceUrl(),
        entity.getPublishedAt(),
        entity.getCategory());
  }
}