package com.hscoderadar.domain.news.controller;

import com.hscoderadar.common.response.ApiResponseMessage;
import com.hscoderadar.domain.news.dto.NewsDto;
import com.hscoderadar.domain.news.service.NewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/news")
@RequiredArgsConstructor
public class NewsController {

    private final NewsService newsService;

    @GetMapping
    @ApiResponseMessage("최신 무역 뉴스를 성공적으로 조회했습니다.")
    public List<NewsDto> getLatestNews() {
        return newsService.getLatestNews();
    }
}