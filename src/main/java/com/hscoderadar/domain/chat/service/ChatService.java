package com.hscoderadar.domain.chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hscoderadar.common.exception.ChatException;
import com.hscoderadar.common.exception.ErrorCode;
import com.hscoderadar.domain.chat.dto.request.ChatRequest;
import com.hscoderadar.domain.chat.dto.request.PythonChatRequest;
import com.hscoderadar.domain.chat.dto.response.*;
import com.hscoderadar.domain.chat.entity.ChatMessage;
import com.hscoderadar.domain.chat.entity.ChatSession;
import com.hscoderadar.domain.chat.repository.ChatMessageRepository;
import com.hscoderadar.domain.chat.repository.ChatSessionRepository;
import com.hscoderadar.domain.user.entity.User;
import com.hscoderadar.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI 채팅 서비스
 * Python AI 서버와의 통신 및 SSE 스트리밍 처리
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ChatService {

  @Qualifier("pythonAiWebClient")
  private final WebClient pythonAiWebClient;
  private final ChatSessionRepository sessionRepository;
  private final ChatMessageRepository messageRepository;
  private final UserRepository userRepository;
  private final ObjectMapper objectMapper;

  // 임시 세션 저장소 (비회원용)
  private final Map<String, ChatSession> tempSessions = new ConcurrentHashMap<>();

  /**
   * AI 채팅 스트리밍 처리
   */
  public SseEmitter streamChat(ChatRequest request, String userId) {
    // SSE Emitter 생성 (5분 타임아웃)
    SseEmitter emitter = new SseEmitter(300000L);

    // 백그라운드에서 스트리밍 처리
    CompletableFuture.runAsync(() -> {
      try {
        processChat(request, userId, emitter);
      } catch (Exception e) {
        log.error("채팅 처리 중 오류 발생", e);
        try {
          emitter.completeWithError(e);
        } catch (Exception ignored) {
        }
      }
    });

    return emitter;
  }

  /**
   * 채팅 처리 메인 로직
   */
  private void processChat(ChatRequest request, String userId, SseEmitter emitter) throws IOException {
    String requestId = UUID.randomUUID().toString();

    // 1. Initial Metadata 전송
    sendEvent(emitter, "initial_metadata", new InitialMetadataEvent(
        requestId,
        System.currentTimeMillis(),
        "Claude 4 Sonnet"));

    // 2. 세션 처리
    ChatSession session = getOrCreateSession(request.sessionUuid(), userId);
    boolean isNewSession = request.sessionUuid() == null;

    sendEvent(emitter, "session_info", new SessionInfoEvent(
        session.getSessionId(),
        isNewSession));

    // 3. Python AI 서버 요청 준비
    PythonChatRequest pythonRequest = new PythonChatRequest(
        userId,
        session.getSessionId(),
        request.message());

    // 4. 사용자 메시지 저장 (회원인 경우)
    ChatMessage userMessage = null;
    if (userId != null) {
      userMessage = ChatMessage.builder()
          .session(session)
          .userMessage(request.message())
          .build();
      session.addMessage(userMessage);
    }

    // 5. Thinking 시작 이벤트
    sendEvent(emitter, "thinking_start", new ThinkingStartEvent(
        System.currentTimeMillis()));

    // 6. Python 서버 스트리밍 호출
    StringBuilder aiResponseBuilder = new StringBuilder();
    final ChatMessage finalUserMessage = userMessage;

    pythonAiWebClient.post()
        .uri("/api/v1/chat/")
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.TEXT_EVENT_STREAM)
        .bodyValue(pythonRequest)
        .retrieve()
        .bodyToFlux(String.class)
        .doOnError(error -> {
          log.error("Python 서버 통신 오류", error);
          try {
            sendEvent(emitter, "error", Map.of("message", "AI 서버 통신 오류가 발생했습니다."));
            emitter.completeWithError(new ChatException(ErrorCode.CHAT_007));
          } catch (IOException e) {
            log.error("에러 이벤트 전송 실패", e);
          }
        })
        .subscribe(
            eventData -> handlePythonEvent(eventData, emitter, aiResponseBuilder),
            error -> {
              try {
                emitter.completeWithError(error);
              } catch (Exception ignored) {
              }
            },
            () -> {
              try {
                // 7. 스트리밍 완료 후 처리
                completeStreaming(emitter, session, finalUserMessage, aiResponseBuilder.toString(), userId);
              } catch (Exception e) {
                log.error("스트리밍 완료 처리 중 오류", e);
              }
            });
  }

  /**
   * Python 서버 이벤트 처리
   */
  private void handlePythonEvent(String eventData, SseEmitter emitter, StringBuilder responseBuilder) {
    try {
      // SSE 이벤트 파싱
      if (eventData.startsWith("data: ")) {
        String jsonData = eventData.substring(6);
        PythonSseEvent event = objectMapper.readValue(jsonData, PythonSseEvent.class);

        switch (event.type()) {
          case "token":
            // 토큰을 main_message_token으로 변환하여 전송
            sendEvent(emitter, "main_message_token", new MainMessageTokenEvent(event.data()));
            responseBuilder.append(event.data());
            break;

          case "session_id":
            // Python 서버에서 세션 ID를 받은 경우 (이미 처리했으므로 무시)
            break;

          case "finish":
            // 스트리밍 완료 (subscribe의 onComplete에서 처리)
            break;

          case "error":
            sendEvent(emitter, "error", Map.of("message", event.data()));
            break;

          default:
            log.warn("알 수 없는 이벤트 타입: {}", event.type());
        }
      }
    } catch (Exception e) {
      log.error("Python 이벤트 처리 중 오류", e);
    }
  }

  /**
   * 스트리밍 완료 처리
   */
  private void completeStreaming(SseEmitter emitter, ChatSession session,
      ChatMessage userMessage, String aiResponse, String userId) throws IOException {
    // AI 응답 저장 (회원인 경우)
    if (userId != null && userMessage != null) {
      userMessage.setAiResponse(aiResponse);
      messageRepository.save(userMessage);
      sessionRepository.save(session);
    }

    // 상세 페이지 버튼 준비 이벤트
    sendEvent(emitter, "detail_page_button_ready", new DetailPageButtonReadyEvent(
        session.getSessionId(),
        "상세 분석 보기",
        "/chat/detail/" + session.getSessionId()));

    // 스트리밍 완료
    emitter.complete();
  }

  /**
   * 세션 조회 또는 생성
   */
  private ChatSession getOrCreateSession(String sessionUuid, String userId) {
    if (sessionUuid != null) {
      // 기존 세션 조회
      if (userId != null) {
        return sessionRepository.findById(sessionUuid)
            .orElseThrow(() -> new ChatException(ErrorCode.CHAT_006));
      } else {
        // 비회원 임시 세션
        ChatSession tempSession = tempSessions.get(sessionUuid);
        if (tempSession == null) {
          throw new ChatException(ErrorCode.CHAT_006);
        }
        return tempSession;
      }
    } else {
      // 새 세션 생성
      String newSessionId = UUID.randomUUID().toString();
      ChatSession newSession = ChatSession.builder()
          .sessionId(newSessionId)
          .createdAt(LocalDateTime.now())
          .lastActivityAt(LocalDateTime.now())
          .build();

      if (userId != null) {
        // 회원 세션
        User user = userRepository.findByEmail(userId)
            .orElseThrow(() -> new ChatException(ErrorCode.USER_NOT_FOUND));
        newSession.setUser(user);
        return sessionRepository.save(newSession);
      } else {
        // 비회원 임시 세션
        tempSessions.put(newSessionId, newSession);
        return newSession;
      }
    }
  }

  /**
   * SSE 이벤트 전송 헬퍼
   */
  private void sendEvent(SseEmitter emitter, String eventName, Object data) throws IOException {
    emitter.send(SseEmitter.event()
        .name(eventName)
        .data(data, MediaType.APPLICATION_JSON));
  }
}