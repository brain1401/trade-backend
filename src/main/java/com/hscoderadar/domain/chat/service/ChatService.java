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

    // 연결 끊김 감지를 위한 완료 콜백 설정
    emitter.onCompletion(() -> {
      log.debug("SSE 연결이 정상적으로 완료됨. User: {}", userId);
    });

    emitter.onTimeout(() -> {
      log.debug("SSE 연결 타임아웃 발생. User: {}", userId);
    });

    emitter.onError(throwable -> {
      if (isClientDisconnectionError(throwable)) {
        log.debug("클라이언트 연결이 끊어짐. User: {}", userId);
      } else {
        log.error("SSE 연결 중 예상치 못한 에러 발생. User: {}", userId, throwable);
      }
    });

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

    // 실제 User ID 조회 (회원인 경우)
    Long actualUserId = null;
    if (userId != null) {
      try {
        User user = userRepository.findByEmail(userId)
            .orElseThrow(() -> new ChatException(ErrorCode.USER_NOT_FOUND));
        actualUserId = user.getId();
      } catch (ChatException e) {
        log.error("사용자를 찾을 수 없습니다: {}", userId);
        try {
          emitter.completeWithError(e);
        } catch (Exception ignored) {
        }
        return emitter;
      }
    }

    // Python 서버로 보낼 요청 객체 생성
    PythonChatRequest pythonRequest = new PythonChatRequest(
        actualUserId,
        sessionUuid.toString(),
        request.message());

    // WebClient를 사용하여 Python AI 서버의 SSE 스트림을 구독
    pythonAiWebClient.post()
        .uri("/api/v1/chat")
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.TEXT_EVENT_STREAM)
        .bodyValue(pythonRequest)
        .retrieve()
        .bodyToFlux(String.class)
        .doOnNext(eventData -> {
          try {
            // SSE 연결 상태 확인
            if (!isEmitterActive(emitter)) {
              log.debug("SSE 연결이 이미 종료되었습니다. 데이터 전송을 중단합니다.");
              return;
            }

            // 받은 데이터를 그대로 클라이언트로 전송
            emitter.send(SseEmitter.event().data(eventData));
          } catch (IOException e) {
            if (isClientDisconnectionError(e)) {
              log.debug("클라이언트 연결 끊김으로 인한 전송 실패. 스트림을 종료합니다.");
            } else {
              log.error("클라이언트로 SSE 데이터 전송 실패", e);
            }
          } catch (Exception e) {
            log.error("SSE 이벤트 처리 중 예상치 못한 에러", e);
          }
        })
        .doOnComplete(() -> {
          try {
            if (isEmitterActive(emitter)) {
              emitter.complete();
              log.debug("Python AI 서버 스트림 완료. SSE 연결 종료.");
            }
          } catch (Exception e) {
            log.debug("SSE 연결 종료 시 에러 (정상적인 상황일 수 있음)", e);
          }
        })
        .doOnError(error -> {
          try {
            if (isEmitterActive(emitter)) {
              if (isClientDisconnectionError(error)) {
                log.debug("Python AI 서버 연결 중 클라이언트 연결 끊김");
                emitter.complete();
              } else {
                log.error("Python AI 서버 통신 중 에러 발생", error);
                emitter.completeWithError(error);
              }
            }
          } catch (Exception e) {
            log.debug("에러 처리 중 SSE 연결 종료 실패 (정상적인 상황일 수 있음)", e);
          }
        })
        .onErrorResume(error -> {
          // 에러 발생 시 빈 Flux 반환하여 스트림 종료
          log.debug("WebClient 스트림 에러 복구 처리", error);
          return Flux.empty();
        })
        .subscribe();

    return emitter;
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

  /**
   * 클라이언트 연결 끊김 에러인지 확인
   */
  private boolean isClientDisconnectionError(Throwable throwable) {
    if (throwable instanceof IOException) {
      String message = throwable.getMessage();
      return message != null && (message.contains("현재 연결은 사용자의 호스트 시스템의 소프트웨어의 의해 중단되었습니다") ||
          message.contains("Connection reset by peer") ||
          message.contains("Broken pipe") ||
          message.contains("Connection aborted") ||
          message.contains("Software caused connection abort"));
    }
    return false;
  }

  /**
   * SseEmitter가 활성 상태인지 확인
   */
  private boolean isEmitterActive(SseEmitter emitter) {
    try {
      // 빈 주석 이벤트로 연결 상태 확인
      emitter.send(SseEmitter.event().comment("heartbeat"));
      return true;
    } catch (Exception e) {
      return false;
    }
  }
}