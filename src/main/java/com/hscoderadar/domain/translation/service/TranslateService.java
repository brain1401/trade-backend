package com.hscoderadar.domain.translation.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class TranslateService {

  private final WebClient webClient;

  @Value("${deepl.api.key}")
  private String apiKey;

  public TranslateService(WebClient.Builder webClientBuilder) {
    this.webClient = webClientBuilder.baseUrl("https://api-free.deepl.com").build();
  }

  @Cacheable("translations")
  public Mono<String> translate(String text, String targetLang) {
    if (text == null || text.isBlank()) {
      return Mono.just(text);
    }

    // 번역된 문자열(String)만 Mono에 담아 반환
    TranslateRequest requestBody = new TranslateRequest(new String[] { text }, targetLang);

    return webClient.post()
        .uri("/v2/translate")
        .header("Authorization", "DeepL-Auth-Key " + apiKey)
        .bodyValue(requestBody)
        .retrieve()
        .bodyToMono(DeepLResponse.class)
        .map(response -> {
          if (response != null && response.translations() != null
              && !response.translations().isEmpty()) {
            return response.translations().get(0).text();
          }
          return text;
        })
        .doOnError(error -> System.err.println("번역 API 오류: " + error.getMessage()))
        .onErrorReturn(text);
  }

  // --- DTO ---
  // DeepL API 요청 본문을 위한 DTO record
  private record TranslateRequest(
      String[] text,
      @JsonProperty("target_lang") String targetLang) {
  }

  // DeepL API 응답을 위한 DTO record
  private record DeepLResponse(
      List<Translation> translations) {
  }

  private record Translation(
      String text) {
  }
}