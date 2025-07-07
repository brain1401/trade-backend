package com.hscoderadar.domain.chat.controller;

import com.hscoderadar.common.response.NoApiResponseWrap;
import com.hscoderadar.domain.chat.dto.request.ChatRequest;
import com.hscoderadar.domain.chat.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * AI 채팅 컨트롤러
 * 파이썬 AI 서버로 요청을 프록시하고 응답을 클라이언트에 전달
 */
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
@Validated
@Tag(name = "Chat", description = "AI 채팅 API")
@Slf4j
public class ChatController {

  private final ChatService chatService;

  /**
   * AI 채팅 엔드포인트
   * 파이썬 서버의 응답을 그대로 클라이언트에 전달
   * 파이썬 서버가 의도 분류 후 JSON 또는 SSE 응답을 결정함
   */
  @PostMapping
  @NoApiResponseWrap
  @Operation(summary = "AI 채팅", description = "파이썬 AI 서버로 요청을 프록시하여 JSON 응답 또는 SSE 스트리밍 응답을 제공")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "성공", content = {
          @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(description = "JSON 응답 (특수 의도)")),
          @Content(mediaType = MediaType.TEXT_EVENT_STREAM_VALUE, schema = @Schema(type = "string", description = "SSE 스트림 (일반 채팅)"))
      })
  })
  public Mono<Object> handleChat(
      @Valid @RequestBody ChatRequest request,
      @AuthenticationPrincipal UserDetails userDetails) {

    String userId = userDetails != null ? userDetails.getUsername() : null;
    log.info("채팅 요청 수신 - 사용자: {}, 메시지 길이: {}", userId, request.message().length());

    // 파이썬 서버로 요청을 프록시하고 응답을 그대로 전달
    return chatService.proxyToPythonServer(request, userId);
  }
}