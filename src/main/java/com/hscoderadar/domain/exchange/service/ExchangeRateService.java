package com.hscoderadar.domain.exchange.service;

import com.hscoderadar.domain.exchange.dto.CustomsExchangeRateResponse;
import com.hscoderadar.domain.exchange.dto.ExchangeRateDto;
import com.hscoderadar.domain.exchange.entity.ExchangeRatesCache;
import com.hscoderadar.domain.exchange.repository.ExchangeRatesCacheRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ExchangeRateService {

    private final ExchangeRatesCacheRepository exchangeRatesCacheRepository;
    private final WebClient webClient;

    @Value("${customs.api.key}")
    private String serviceKey;

    private final String apiUrl = "https://unipass.customs.go.kr:38010/ext/rest/trifFxrtInfoQry/retrieveTrifFxrtInfo";

    /**
     * 최신 환율 정보 목록을 조회
     * 캐시 확인 후, 유효한 캐시가 없으면 외부 API를 호출하여 최신 정보를 가져와 캐싱
     * 수입과 수출 환율을 모두 조회하여 한 번에 저장
     */
    @Transactional
    public Mono<List<ExchangeRateDto>> getLatestExchangeRates() {
        List<ExchangeRatesCache> cachedRates = exchangeRatesCacheRepository.findLatestActiveExchangeRates(LocalDateTime.now());

        if (!cachedRates.isEmpty()) {
            log.info("유효한 환율 캐시 {}건을 조회했습니다.", cachedRates.size());
            List<ExchangeRateDto> dtoList = cachedRates.stream()
                    .map(ExchangeRateDto::from)
                    .collect(Collectors.toList());
            return Mono.just(dtoList);
        }

        log.info("유효한 캐시가 없어 관세청 OPEN API를 호출합니다 (수입/수출 동시).");

        // 수입과 수출 API를 동시에 호출
        Mono<List<CustomsExchangeRateResponse.Item>> importRatesMono = fetchFromCustomsApi("2");
        Mono<List<CustomsExchangeRateResponse.Item>> exportRatesMono = fetchFromCustomsApi("1");

        return Mono.zip(importRatesMono, exportRatesMono)
                .flatMap(tuple -> {
                    List<CustomsExchangeRateResponse.Item> importItems = tuple.getT1();
                    List<CustomsExchangeRateResponse.Item> exportItems = tuple.getT2();

                    // 두 리스트를 하나로 합침
                    List<CustomsExchangeRateResponse.Item> allItems = Stream.concat(importItems.stream(), exportItems.stream())
                            .collect(Collectors.toList());

                    if (allItems.isEmpty()) {
                        return Mono.just(Collections.emptyList());
                    }
                    
                    // 합쳐진 리스트를 saveAll로 한번에 저장
                    return saveAllAndMapToDto(allItems);
                });
    }

    /**
     * 특정 통화의 최신 환율 정보를 조회
     */
    @Transactional
    public Mono<ExchangeRateDto> getExchangeRateByCurrency(String currencyCode) {
        final String searchCode = currencyCode.toUpperCase();
        Optional<ExchangeRatesCache> cachedRate = exchangeRatesCacheRepository.findTopByCurrencyCodeAndIsActiveTrueAndExpiresAtAfterOrderByFetchedAtDesc(searchCode, LocalDateTime.now());

        if (cachedRate.isPresent()) {
            log.info("캐시에서 {} 환율 정보를 찾았습니다.", searchCode);
            return Mono.just(ExchangeRateDto.from(cachedRate.get()));
        }

        log.info("캐시에 {} 정보가 없어 API 호출 후 필터링합니다.", searchCode);
        return getLatestExchangeRates().flatMap(rateList ->
                rateList.stream()
                        .filter(dto -> dto.getCurrencyCode().equalsIgnoreCase(searchCode))
                        .findFirst()
                        .map(Mono::just)
                        .orElse(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "해당 국가의 환율 정보를 찾을 수 없습니다: " + searchCode)))
        );
    }

    /**
     * 관세청 API를 호출하여 XML 데이터를 Mono<List<Item>> 형태로 반환
     */
    private Mono<List<CustomsExchangeRateResponse.Item>> fetchFromCustomsApi(String importExportType) {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        URI uri = UriComponentsBuilder.fromUriString(apiUrl)
                .queryParam("crkyCn", serviceKey)
                .queryParam("qryYymmDd", today)
                .queryParam("imexTp", importExportType)
                .build(true)
                .toUri();

        log.info("관세청 API 요청 URI: {}", uri);

        return webClient.get()
                .uri(uri)
                .accept(MediaType.APPLICATION_XML)
                .retrieve()
                .bodyToMono(CustomsExchangeRateResponse.class)
                .map(response -> {
                    if (response == null || response.getItemList() == null) {
                        log.warn("API 응답에서 유효한 데이터를 찾을 수 없습니다.");
                        return Collections.<CustomsExchangeRateResponse.Item>emptyList();
                    }
                    log.info("API로부터 {} 타입 환율 정보 {}개를 수신했습니다.", "1".equals(importExportType) ? "수출" : "수입", response.getItemList().size());
                    return response.getItemList();
                })
                .doOnError(e -> log.error("관세청 API 호출 또는 파싱 중 오류 발생", e))
                .onErrorReturn(Collections.emptyList());
    }

    /**
     * API 응답 Item을 ExchangeRatesCache 엔티티로 변환
     */
    private ExchangeRatesCache mapToEntity(CustomsExchangeRateResponse.Item item) {
        String rateTypeName = "1".equals(item.getRateType()) ? "수출" : "수입";
        return ExchangeRatesCache.builder()
                .currencyCode(item.getCurrencyCode())
                .currencyName(item.getCurrencyName() + " (" + rateTypeName + ")")
                .exchangeRate(new BigDecimal(item.getExchangeRate().replace(",", "")))
                .sourceApi("관세청 OPEN API")
                .expiresAt(LocalDate.parse(item.getNotifiedDate(), DateTimeFormatter.ofPattern("yyyyMMdd")).atStartOfDay().plusDays(1))
                .build();
    }

    /**
     * 여러 Item을 한번에 저장하고 DTO 리스트로 변환하여 반환
     */
    @Transactional
    public Mono<List<ExchangeRateDto>> saveAllAndMapToDto(List<CustomsExchangeRateResponse.Item> items) {
        return Mono.fromCallable(() -> {
            List<ExchangeRatesCache> entities = items.stream()
                    .map(this::mapToEntity)
                    .collect(Collectors.toList());
            
            List<ExchangeRatesCache> savedEntities = exchangeRatesCacheRepository.saveAll(entities);
            
            log.info("DB에 {}건의 환율 정보를 한번에 저장 완료", savedEntities.size());
            
            return savedEntities.stream()
                    .map(ExchangeRateDto::from)
                    .collect(Collectors.toList());
        }).subscribeOn(Schedulers.boundedElastic()); // DB I/O 작업을 별도 스레드에서 실행
    }
}