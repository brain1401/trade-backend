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

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI 채팅 서비스
 * Python AI 서버와의 통신 및 SSE 스트리밍 처리
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

  @Qualifier("pythonAiWebClient")
  private final WebClient pythonAiWebClient;
  private final ChatSessionRepository sessionRepository;
  private final ChatMessageRepository messageRepository;
  private final UserRepository userRepository;
  private final ObjectMapper objectMapper;

  // 임시 세션 저장소 (비회원용)
  private final Map<UUID, ChatSession> tempSessions = new ConcurrentHashMap<>();

  /**
   * AI 채팅 스트리밍 처리
   */
  public SseEmitter streamChat(ChatRequest request, String userId) {
    // 클라이언트로 보낼 SSE Emitter 생성 (5분 타임아웃)
    SseEmitter emitter = new SseEmitter(300_000L);

    UUID sessionUuid;
    try {
      if (request.sessionUuid() != null && !request.sessionUuid().isEmpty()) {
        sessionUuid = UUID.fromString(request.sessionUuid());
      } else {
        sessionUuid = UUID.randomUUID();
        log.info("새로운 채팅 세션을 시작합니다. Session UUID: {}", sessionUuid);
      }
    } catch (IllegalArgumentException e) {
      log.warn("잘못된 형식의 Session UUID 입니다: {}. 새로운 UUID를 생성합니다.", request.sessionUuid());
      sessionUuid = UUID.randomUUID();
    }

    // Python 서버로 보낼 요청 객체 생성
    PythonChatRequest pythonRequest = new PythonChatRequest(
        userId,
        request.sessionUuid(), // 세션 ID 전달
        request.message());

    // WebClient를 사용하여 Python AI 서버의 SSE 스트림을 구독
    pythonAiWebClient.post()
        .uri("/api/v1/chat/")
        .accept(MediaType.TEXT_EVENT_STREAM)
        .bodyValue(pythonRequest)
        .retrieve()
        .bodyToFlux(String.class) // 응답을 Flux<String> 스트림으로 받음
        .doOnNext(eventData -> {
          try {
            // 받은 데이터를 그대로 클라이언트로 전송
            emitter.send(SseEmitter.event().data(eventData));
          } catch (IOException e) {
            log.error("클라이언트로 SSE 데이터 전송 실패", e);
          }
        })
        .doOnComplete(emitter::complete) // 스트림이 완료되면 클라이언트 연결도 완료
        .doOnError(emitter::completeWithError) // 에러 발생 시 클라이언트 연결도 에러로 종료
        .subscribe(); // 비동기 구독 시작

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

    // 2. 세션 처리 (String을 UUID로 변환)
    UUID sessionUuid = null;
    if (request.sessionUuid() != null) {
      try {
        sessionUuid = UUID.fromString(request.sessionUuid());
      } catch (IllegalArgumentException e) {
        log.error("잘못된 UUID 형식: {}", request.sessionUuid());
        throw new ChatException(ErrorCode.CHAT_006);
      }
    }

    ChatSession session = getOrCreateSession(sessionUuid, userId);
    boolean isNewSession = sessionUuid == null;

    sendEvent(emitter, "session_info", new SessionInfoEvent(
        session.getSessionUuid().toString(),
        isNewSession));

    // 3. Python AI 서버 요청 준비
    PythonChatRequest pythonRequest = new PythonChatRequest(
        userId,
        session.getSessionUuid().toString(),
        request.message());

    // 4. 사용자 메시지 저장 (회원인 경우)
    ChatMessage userMessage = null;
    if (userId != null) {
      userMessage = ChatMessage.builder()
          .sessionUuid(session.getSessionUuid())
          .sessionCreatedAt(session.getCreatedAt())
          .messageType("USER")
          .content(request.message())
          .build();
      messageRepository.save(userMessage);
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
        .timeout(Duration.ofSeconds(120)) // Spring Boot 3.5+ 권장 스트리밍 타임아웃
        .onErrorResume(error -> {
          log.error("Python 서버 통신 오류 상세", error);

          // SSL/HTTP 파싱 에러 구체적 처리
          if (error instanceof java.net.ConnectException) {
            log.error("Python 서버 연결 실패: {}", error.getMessage());
            try {
              sendEvent(emitter, "error", new ErrorEvent("AI 서버에 연결할 수 없습니다. 서버 상태를 확인해주세요."));
            } catch (IOException e) {
              log.error("연결 에러 이벤트 전송 실패", e);
            }
          } else if (error.getMessage() != null && error.getMessage().contains("SSL")) {
            log.error("SSL 관련 오류: {}", error.getMessage());
            try {
              sendEvent(emitter, "error", new ErrorEvent("보안 연결 오류가 발생했습니다."));
            } catch (IOException e) {
              log.error("SSL 에러 이벤트 전송 실패", e);
            }
          } else {
            try {
              sendEvent(emitter, "error", new ErrorEvent("AI 서버 통신 오류가 발생했습니다."));
            } catch (IOException e) {
              log.error("일반 에러 이벤트 전송 실패", e);
            }
          }

          return Flux.empty(); // 빈 Flux 반환으로 스트림 종료
        })
        .doOnError(error -> {
          log.error("Python 서버 통신 오류", error);
          try {
            emitter.completeWithError(new ChatException(ErrorCode.CHAT_007));
          } catch (Exception ignored) {
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

          case "session_uuid":
            // Python 서버에서 세션 ID를 받은 경우 (이미 처리했으므로 무시)
            break;

          case "finish":
            // 스트리밍 완료 (subscribe의 onComplete에서 처리)
            break;

          case "error":
            sendEvent(emitter, "error", new ErrorEvent(event.data()));
            break;

          default:
            log.warn("알 수 없는 이벤트 타입: {}", event.type());
        }
      }
    } catch (Exception e) {
      log.error("Python 이벤트 처리 중 오류 - 데이터: {}", eventData, e);
      try {
        sendEvent(emitter, "error", new ErrorEvent("이벤트 처리 중 오류가 발생했습니다."));
      } catch (IOException ioException) {
        log.error("에러 이벤트 전송 실패", ioException);
      }
    }
  }

  /**
   * 스트리밍 완료 처리
   */
  @Transactional
  private void completeStreaming(SseEmitter emitter, ChatSession session,
      ChatMessage userMessage, String aiResponse, String userId) throws IOException {
    // AI 응답 저장 (회원인 경우)
    if (userId != null && !aiResponse.isEmpty()) {
      ChatMessage aiMessage = ChatMessage.builder()
          .sessionUuid(session.getSessionUuid())
          .sessionCreatedAt(session.getCreatedAt())
          .messageType("AI")
          .content(aiResponse)
          .aiModel("Claude 4 Sonnet")
          .build();
      messageRepository.save(aiMessage);
      sessionRepository.save(session);
    }

    // 상세 페이지 버튼 준비 이벤트
    sendEvent(emitter, "detail_page_button_ready", new DetailPageButtonReadyEvent(
        session.getSessionUuid().toString(),
        "상세 분석 보기",
        "/chat/detail/" + session.getSessionUuid()));

    // 스트리밍 완료
    emitter.complete();
  }

  /**
   * 세션 조회 또는 생성
   */
  @Transactional
  private ChatSession getOrCreateSession(UUID sessionUuid, String userId) {
    if (sessionUuid != null) {
      // 기존 세션 조회
      if (userId != null) {
        return sessionRepository.findBySessionUuid(sessionUuid)
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
      UUID newSessionId = UUID.randomUUID();
      LocalDateTime now = LocalDateTime.now();

      ChatSession newSession = ChatSession.builder()
          .sessionUuid(newSessionId)
          .createdAt(now)
          .updatedAt(now)
          .messageCount(0)
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