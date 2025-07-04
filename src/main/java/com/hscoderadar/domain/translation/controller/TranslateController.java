package com.hscoderadar.domain.translation.controller;

import com.hscoderadar.domain.translation.dto.TranslateResponse;
import com.hscoderadar.domain.translation.service.TranslateService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/translate")
public class TranslateController {

    private final TranslateService translateService;

    public TranslateController(TranslateService translateService) {
        this.translateService = translateService;
    }

    @PostMapping
    public Mono<TranslateResponse> translateText(@RequestBody Map<String, String> payload) {
        String text = payload.get("text");

        return translateService.translate(text, "KO")
                .map(TranslateResponse::new);
    }
}