package com.hscoderadar.domain.statistics.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hscoderadar.domain.statistics.dto.ComprehensiveTradeSummary;
import com.hscoderadar.domain.statistics.dto.StatisticsRequest;
import com.hscoderadar.domain.statistics.service.StatisticsService;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/statistics")
@RequiredArgsConstructor
public class StatisticsController {
    
    private final StatisticsService statisticsService;

    @PostMapping
    public Mono<ComprehensiveTradeSummary> getTradeSummary(@RequestBody StatisticsRequest request) {
        return statisticsService.getComprehensiveSummary(request);
    }
    
}
