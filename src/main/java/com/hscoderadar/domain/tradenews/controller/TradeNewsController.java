package com.hscoderadar.domain.tradenews.controller;

import com.hscoderadar.domain.tradenews.dto.response.TradeNewsResponse;
import com.hscoderadar.domain.tradenews.service.TradeNewsService;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/news")
@RequiredArgsConstructor
public class TradeNewsController {

  private final TradeNewsService newsService;

  @GetMapping
  @Operation(summary = "최신 뉴스 조회", description = "최신 뉴스를 조회합니다.")
  public Page<TradeNewsResponse> getLatestNews(
      @RequestParam(value = "offset", defaultValue = "0") int offset,
      @RequestParam(value = "limit", defaultValue = "10") int limit) {

    return newsService.findNewsWithPagination(offset, limit);

  }
}