package com.hscoderadar.domain.customs.service;

import com.hscoderadar.domain.customs.dto.CargoClearanceProgressResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import java.net.URI;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomsService {

  private final WebClient webClient;

  @Value("${customs.api.key2}")
  private String apiKey;

  private static final String API_URL = "https://unipass.customs.go.kr:38010/ext/rest/cargCsclPrgsInfoQry/retrieveCargCsclPrgsInfo";

  /**
   * 화물관리번호로 통관 진행 정보를 조회
   * 
   * @param cargoManagementNumber 조회할 화물관리번호
   * @return 통관 진행 정보 Mono 객체
   */
  public Mono<CargoClearanceProgressResponse> getCargoClearanceProgress(String cargoManagementNumber) {
    if (cargoManagementNumber == null || cargoManagementNumber.isBlank()) {
      return Mono.error(new IllegalArgumentException("화물관리번호는 필수입니다."));
    }

    URI uri = UriComponentsBuilder.fromPath(API_URL)
        .queryParam("crkyCn", apiKey)
        .queryParam("cargMtNo", cargoManagementNumber)
        .build(true)
        .toUri();

    log.info("관세청 화물통관정보조회 API 요청: {}", uri);

    return webClient.get()
        .uri(uri)
        .accept(MediaType.APPLICATION_XML)
        .retrieve()
        .bodyToMono(CargoClearanceProgressResponse.class)
        .doOnSuccess(response -> log.info("API 응답 성공: 화물번호={}, 진행상태={}",
            cargoManagementNumber,
            response.getBaseInfo() != null
                ? response.getBaseInfo().getClearanceProgressStatus()
                : "N/A"))
        .doOnError(error -> log.error("API 호출 실패: 화물번호={}, 오류={}",
            cargoManagementNumber, error.getMessage()));
  }
}