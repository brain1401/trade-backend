package com.hscoderadar.domain.statistics.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.hscoderadar.domain.statistics.dto.ComprehensiveTradeSummary;
import com.hscoderadar.domain.statistics.dto.ComtradeData;
import com.hscoderadar.domain.statistics.dto.ComtradeResponse;
import com.hscoderadar.domain.statistics.dto.StatisticsRequest;
import com.hscoderadar.domain.statistics.dto.TopTradeItem;

import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

@Service
public class StatisticsService {
    
    private final WebClient webClient;

    @Value("${comtrade.api.key}")
    private String apiKey;
    
    public StatisticsService(@Qualifier("comtradeWebClient") WebClient webClient) {
        this.webClient = webClient;
    }
    
    // 중간 처리 결과를 담기 위한 내부 record
    private record ProcessedData(double totalValue, List<TopTradeItem> topCategories, List<TopTradeItem> topProducts) {}
    
    public Mono<ComprehensiveTradeSummary> getComprehensiveSummary(StatisticsRequest request) {
        Mono<ComtradeResponse> exportMono = fetchTradeData(request, "X");
        Mono<ComtradeResponse> importMono = fetchTradeData(request, "M");

        // 1. 수출 데이터 요청
        return exportMono.flatMap(exportResponse -> {
            // 2. 수출 요청이 성공하면, 그 결과를 가지고 수입 데이터 요청 Mono를 반환
            return importMono.map(importResponse ->
                // 3. 두 응답(수출, 수입)을 Tuples.of를 사용해 하나로 묶어 다음 체인으로 전달
                Tuples.of(exportResponse, importResponse)
            );
        }).map(tuple -> {
            // 4. 순차적으로 받아온 두 응답을 처리
            ComtradeResponse exportResponse = tuple.getT1();
            ComtradeResponse importResponse = tuple.getT2();

            ProcessedData exportData = processApiResponse(exportResponse);
            ProcessedData importData = processApiResponse(importResponse);

            return new ComprehensiveTradeSummary(
                    exportData.totalValue(),
                    importData.totalValue(),
                    exportData.topCategories(),
                    exportData.topProducts(),
                    importData.topCategories(),
                    importData.topProducts()
            );
        });
    }
    
    /**
     * 하나의 Comtrade 응답에서 요구사항에 맞게 모든 데이터를 추출하는 메소드
     */
    private ProcessedData processApiResponse(ComtradeResponse response) {
        if (response == null || response.data() == null || response.data().isEmpty()) {
            return new ProcessedData(0.0, Collections.emptyList(), Collections.emptyList());
        }

        List<ComtradeData> allData = response.data();
        // 총 수출, 총 수입 구하기
        double officialTotalValue = allData.stream()
                .filter(d -> d.cmdDesc() != null && "All Commodities".equalsIgnoreCase(d.cmdDesc().trim()))
                .mapToDouble(ComtradeData::primaryValue)
                .sum();

        // 카테고리 Top 5 계산
        Map<String, Double> categoryValueMap = allData.stream()
                .filter(d -> d.aggrLevel() != null && d.aggrLevel() == 2)
                .filter(d -> d.cmdDesc() != null && !d.cmdDesc().isBlank())
                .collect(Collectors.groupingBy(
                        ComtradeData::cmdDesc,
                        Collectors.summingDouble(ComtradeData::primaryValue)
                ));

        List<TopTradeItem> topCategories = categoryValueMap.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(5)
                .map(entry -> new TopTradeItem(entry.getKey(), entry.getValue()))
                .toList();

        // 주요 상세 품목 Top 5 계산
        Map<String, Double> productValueMap = allData.stream()
                .filter(d -> d.aggrLevel() != null && d.aggrLevel() == 6)
                .filter(d -> d.cmdDesc() != null && !d.cmdDesc().isBlank())
                .collect(Collectors.groupingBy(
                        ComtradeData::cmdDesc,
                        Collectors.summingDouble(ComtradeData::primaryValue)
                ));

        List<TopTradeItem> topProducts = productValueMap.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(5)
                .map(entry -> new TopTradeItem(entry.getKey(), entry.getValue()))
                .toList();

        return new ProcessedData(officialTotalValue, topCategories, topProducts);
    }

    /**
     * flowCode를 파라미터로 받아 API를 호출하는 재사용 가능한 메소드
     */
    private Mono<ComtradeResponse> fetchTradeData(StatisticsRequest request, String flowCode) {
        String period = IntStream.rangeClosed(Integer.parseInt(request.StartPeriod()), Integer.parseInt(request.EndPeriod()))
                .mapToObj(String::valueOf)
                .collect(Collectors.joining(","));

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/data/v1/get/C/A/HS")
                        .queryParam("reporterCode", request.reporterCode())
                        .queryParam("partnerCode", request.partnerCode())
                        .queryParam("flowCode", flowCode) // "X" 또는 "M"
                        .queryParam("period", period)
                        .queryParam("includeDesc", true)
                        .build())
                .header("Ocp-Apim-Subscription-Key", apiKey)
                .retrieve()
                .bodyToMono(ComtradeResponse.class)
                .doOnEach(signal -> {
                if (signal.isOnNext()) {
                    // 성공적으로 데이터를 받아 DTO로 변환했을 때
                    System.out.println("[SUCCESS] 데이터 도착");
                } else if (signal.isOnError()) {
                    // 에러가 발생했을 때 (가장 유력)
                    System.err.println("[ERROR] 스트림 실패 : " + signal.getThrowable());
                } else if (signal.isOnComplete() && !signal.hasValue()) {
                    // 데이터 없이 정상적으로 완료되었을 때
                    System.out.println("[EMPTY] 노 데이터");
                }
            })
                .onErrorReturn(new ComtradeResponse(Collections.emptyList())); // 에러 시 빈 데이터 반환
    }
}
