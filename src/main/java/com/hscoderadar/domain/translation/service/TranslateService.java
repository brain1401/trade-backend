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
                    if (response != null && response.getTranslations() != null
                            && !response.getTranslations().isEmpty()) {
                        return response.getTranslations().get(0).getText();
                    }
                    return text;
                })
                .doOnError(error -> System.err.println("번역 API 오류: " + error.getMessage()))
                .onErrorReturn(text);
    }

    // --- DTO ---
    // DeepL API 요청 본문을 위한 DTO 클래스
    private static class TranslateRequest {
        private final String[] text;
        @JsonProperty("target_lang") // JSON 필드명과 Java 필드명을 매핑
        private final String targetLang;

        public TranslateRequest(String[] text, String targetLang) {
            this.text = text;
            this.targetLang = targetLang;
        }

        public String[] getText() {
            return text;
        }

        public String getTargetLang() {
            return targetLang;
        }
    }

    // DeepL API 응답을 위한 DTO 클래스들
    private static class DeepLResponse {
        private List<Translation> translations;

        public List<Translation> getTranslations() {
            return translations;
        }

        public void setTranslations(List<Translation> translations) {
            this.translations = translations;
        }
    }

    private static class Translation {
        private String text;

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }
}