package com.hscoderadar.domain.chat.controller;

import com.hscoderadar.common.response.NoApiResponseWrap;
import com.hscoderadar.domain.chat.dto.request.ChatRequest;
import com.hscoderadar.domain.chat.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI 채팅 컨트롤러
 * Server-Sent Events(SSE)를 통한 실시간 AI 응답 스트리밍 제공
 */
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
@Validated
@Tag(name = "Chat", description = "AI 채팅 API")
public class ChatController {

  private final ChatService chatService;

  /**
   * AI 채팅 스트리밍 엔드포인트
   * SSE를 통해 실시간으로 AI 응답을 스트리밍함
   */
  @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  @NoApiResponseWrap // SSE는 ApiResponse로 감싸지 않음
  @Operation(summary = "AI 채팅 스트리밍", description = "사용자 메시지를 받아 AI 응답을 SSE로 스트리밍합니다. " +
      "인증된 사용자는 대화 내역이 저장되며, 비회원은 임시 세션으로 처리됩니다.")
  public SseEmitter streamChat(
      @Valid @RequestBody ChatRequest request,
      @AuthenticationPrincipal UserDetails userDetails) {
    String userId = userDetails != null ? userDetails.getUsername() : null;
    return chatService.streamChat(request, userId);
  }

  /**
   * 채팅 세션 히스토리 조회 (회원 전용)
   */
  @GetMapping("/sessions")
  @Operation(summary = "채팅 세션 목록 조회", description = "인증된 사용자의 채팅 세션 목록을 조회합니다.")
  public Object getUserSessions(@AuthenticationPrincipal UserDetails userDetails) {
    // TODO: 추후 구현
    return null;
  }

  /**
   * 특정 세션의 대화 내역 조회 (회원 전용)
   */
  @GetMapping("/sessions/{sessionId}")
  @Operation(summary = "채팅 세션 상세 조회", description = "특정 채팅 세션의 대화 내역을 조회합니다.")
  public Object getSessionMessages(
      @PathVariable String sessionId,
      @AuthenticationPrincipal UserDetails userDetails) {
    // TODO: 추후 구현
    return null;
  }
}