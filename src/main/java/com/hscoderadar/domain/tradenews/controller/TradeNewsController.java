package com.hscoderadar.domain.tradenews.controller;

import com.hscoderadar.domain.tradenews.dto.response.TradeNewsResponse;
import com.hscoderadar.domain.tradenews.service.TradeNewsService;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/news")
@RequiredArgsConstructor
public class TradeNewsController {

  private final TradeNewsService newsService;

  @GetMapping
  @Operation(summary = "최신 뉴스 조회", description = "최신 뉴스를 조회합니다.")
  public List<TradeNewsResponse> getLatestNews() {
    return newsService.getLatestNews();
  }
}