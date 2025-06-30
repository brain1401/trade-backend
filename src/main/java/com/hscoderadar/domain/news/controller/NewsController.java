package com.hscoderadar.domain.news.controller;

import com.hscoderadar.domain.news.dto.response.NewsResponse;
import com.hscoderadar.domain.news.service.NewsService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
public class NewsController {

  private final NewsService newsService;

  @GetMapping
  @Operation(summary = "최신 뉴스 조회", description = "최신 뉴스를 조회합니다.")
  public List<NewsResponse> getLatestNews() {
    return newsService.getLatestNews();
  }
}