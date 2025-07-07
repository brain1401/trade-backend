package com.hscoderadar.domain.chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hscoderadar.common.exception.ChatException;
import com.hscoderadar.common.exception.ErrorCode;
import com.hscoderadar.domain.chat.dto.request.ChatRequest;
import com.hscoderadar.domain.chat.dto.request.PythonChatRequest;
import com.hscoderadar.domain.chat.entity.ChatSession;
import com.hscoderadar.domain.chat.repository.ChatSessionRepository;
import com.hscoderadar.domain.user.entity.User;
import com.hscoderadar.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI 채팅 서비스
 * 파이썬 AI 서버와의 통신 및 프록시 처리
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

  @Qualifier("pythonAiWebClient")
  private final WebClient pythonAiWebClient;
  private final ChatSessionRepository sessionRepository;
  private final UserRepository userRepository;
  private final ObjectMapper objectMapper;

  // 임시 세션 저장소 (비회원용)
  private final Map<UUID, ChatSession> tempSessions = new ConcurrentHashMap<>();

  /**
   * 파이썬 서버로 요청을 프록시하고 응답을 그대로 전달
   * 파이썬 서버가 의도 분류 후 JSON 또는 SSE 응답을 결정
   */
  public Mono<Object> proxyToPythonServer(ChatRequest request, String userId) {
    UUID sessionUuid = getOrCreateSessionUuid(request, userId);
    Long actualUserId = getUserId(userId);

    // 파이썬 서버용 요청 객체 생성
    PythonChatRequest pythonRequest = new PythonChatRequest(
        actualUserId,
        sessionUuid.toString(),
        request.message());

    log.info("파이썬 서버로 프록시 요청 전송 - 세션: {}, 사용자: {}", sessionUuid, userId);

    return pythonAiWebClient.post()
        .uri("/api/v1/chat")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(pythonRequest)
        .exchangeToMono(response -> {
          String contentType = response.headers().contentType()
              .map(MediaType::toString)
              .orElse(MediaType.APPLICATION_JSON_VALUE);

          log.info("파이썬 서버 응답 Content-Type: {}", contentType);

          // Content-Type에 따라 응답 처리 방식 결정
          if (contentType.contains(MediaType.TEXT_EVENT_STREAM_VALUE)) {
            // SSE 응답인 경우 SseEmitter로 처리
            return handleSseResponse(response, sessionUuid, userId);
          } else {
            // JSON 응답인 경우 ResponseEntity로 처리
            return handleJsonResponse(response);
          }
        })
        .timeout(Duration.ofSeconds(60))
        .doOnError(error -> log.error("파이썬 서버 프록시 중 에러 발생", error))
        .onErrorResume(error -> {
          log.error("파이썬 서버 통신 실패, 폴백 처리", error);
          return createErrorResponse(error.getMessage());
        });
  }

  /**
   * SSE 응답 처리 - SseEmitter 직접 반환
   */
  private Mono<Object> handleSseResponse(
      org.springframework.web.reactive.function.client.ClientResponse response,
      UUID sessionUuid, String userId) {

    SseEmitter emitter = new SseEmitter(300_000L);

    // 연결 끊김 감지 콜백 설정
    setupSseCallbacks(emitter, userId);

    // 파이썬 서버의 SSE 스트림을 구독하여 클라이언트로 전달
    response.bodyToFlux(String.class)
        .doOnNext(eventData -> {
          try {
            if (isEmitterActive(emitter)) {
              emitter.send(SseEmitter.event().data(eventData));
            }
          } catch (IOException e) {
            handleSseError(e, emitter);
          }
        })
        .doOnComplete(() -> completeSseEmitter(emitter))
        .doOnError(error -> completeSseEmitterWithError(emitter, error))
        .subscribe();

    return Mono.just(emitter);
  }

  /**
   * JSON 응답 처리 - ResponseEntity 반환
   */
  private Mono<Object> handleJsonResponse(
      org.springframework.web.reactive.function.client.ClientResponse response) {

    return response.bodyToMono(String.class)
        .map(body -> {
          try {
            // JSON 문자열을 Object로 파싱하여 전달
            Object jsonObject = objectMapper.readValue(body, Object.class);
            return ResponseEntity.status(response.statusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .headers(copyHeaders(response.headers().asHttpHeaders()))
                .body(jsonObject);
          } catch (Exception e) {
            log.error("JSON 응답 파싱 실패", e);
            return ResponseEntity.status(response.statusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("error", "응답 파싱 실패", "message", e.getMessage()));
          }
        });
  }

  /**
   * 세션 UUID 조회 또는 생성
   */
  private UUID getOrCreateSessionUuid(ChatRequest request, String userId) {
    String sessionUuidStr = request.sessionUuid();

    if (sessionUuidStr != null && !sessionUuidStr.isEmpty()) {
      try {
        UUID existingUuid = UUID.fromString(sessionUuidStr);
        log.info("기존 세션 사용: {}", existingUuid);
        return existingUuid;
      } catch (IllegalArgumentException e) {
        log.warn("잘못된 세션 UUID 형식: {}", sessionUuidStr);
      }
    }

    // 새 세션 생성
    UUID newSessionUuid = UUID.randomUUID();
    createNewSession(newSessionUuid, userId);
    log.info("새 세션 생성: {}", newSessionUuid);
    return newSessionUuid;
  }

  /**
   * 사용자 ID 조회
   */
  private Long getUserId(String userId) {
    if (userId == null) {
      return null;
    }

    try {
      User user = userRepository.findByEmail(userId)
          .orElseThrow(() -> new ChatException(ErrorCode.USER_NOT_FOUND));
      return user.getId();
    } catch (ChatException e) {
      log.warn("사용자를 찾을 수 없음: {}", userId);
      return null;
    }
  }

  /**
   * 새 세션 생성
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  private void createNewSession(UUID sessionUuid, String userId) {
    OffsetDateTime now = OffsetDateTime.now();

    ChatSession newSession = ChatSession.builder()
        .sessionUuid(sessionUuid)
        .createdAt(now)
        .updatedAt(now)
        .messageCount(0)
        .build();

    if (userId != null) {
      try {
        User user = userRepository.findByEmail(userId)
            .orElseThrow(() -> new ChatException(ErrorCode.USER_NOT_FOUND));
        newSession.setUser(user);
        sessionRepository.save(newSession);
        log.info("회원용 세션 DB 저장 완료: {}", sessionUuid);
      } catch (ChatException e) {
        log.warn("회원 세션 생성 실패, 임시 세션으로 처리: {}", userId);
        tempSessions.put(sessionUuid, newSession);
      }
    } else {
      tempSessions.put(sessionUuid, newSession);
      log.info("비회원용 임시 세션 생성: {}", sessionUuid);
    }
  }

  /**
   * SSE Emitter 콜백 설정
   */
  private void setupSseCallbacks(SseEmitter emitter, String userId) {
    emitter.onCompletion(() -> log.debug("SSE 연결 정상 완료 - 사용자: {}", userId));

    emitter.onTimeout(() -> log.debug("SSE 연결 타임아웃 - 사용자: {}", userId));

    emitter.onError(throwable -> {
      if (isClientDisconnectionError(throwable)) {
        log.debug("클라이언트 연결 끊김 - 사용자: {}", userId);
      } else {
        log.error("SSE 연결 중 예상치 못한 에러 - 사용자: {}", userId, throwable);
      }
    });
  }

  /**
   * 헤더 복사 (필요한 헤더만)
   */
  private HttpHeaders copyHeaders(HttpHeaders sourceHeaders) {
    HttpHeaders targetHeaders = new HttpHeaders();

    // CORS 관련 헤더 복사
    sourceHeaders.entrySet().stream()
        .filter(entry -> entry.getKey().startsWith("Access-Control-"))
        .forEach(entry -> targetHeaders.addAll(entry.getKey(), entry.getValue()));

    return targetHeaders;
  }

  /**
   * 에러 응답 생성
   */
  private Mono<Object> createErrorResponse(String errorMessage) {
    Map<String, Object> errorResponse = Map.of(
        "error", "PROXY_ERROR",
        "message", "파이썬 서버 통신 실패",
        "details", errorMessage);

    return Mono.just(ResponseEntity.status(500)
        .contentType(MediaType.APPLICATION_JSON)
        .body(errorResponse));
  }

  /**
   * SSE 에러 처리
   */
  private void handleSseError(IOException e, SseEmitter emitter) {
    if (isClientDisconnectionError(e)) {
      log.debug("클라이언트 연결 끊김으로 인한 SSE 전송 실패");
    } else {
      log.error("SSE 데이터 전송 실패", e);
    }
  }

  /**
   * SSE Emitter 완료 처리
   */
  private void completeSseEmitter(SseEmitter emitter) {
    try {
      if (isEmitterActive(emitter)) {
        emitter.complete();
        log.debug("SSE 스트림 정상 완료");
      }
    } catch (Exception e) {
      log.debug("SSE 완료 처리 중 에러 (정상적인 상황일 수 있음)", e);
    }
  }

  /**
   * SSE Emitter 에러 완료 처리
   */
  private void completeSseEmitterWithError(SseEmitter emitter, Throwable error) {
    try {
      if (isEmitterActive(emitter)) {
        if (isClientDisconnectionError(error)) {
          emitter.complete();
        } else {
          emitter.completeWithError(error);
        }
      }
    } catch (Exception e) {
      log.debug("SSE 에러 완료 처리 중 에러", e);
    }
  }

  /**
   * 클라이언트 연결 끊김 에러 확인
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
   * SseEmitter 활성 상태 확인
   */
  private boolean isEmitterActive(SseEmitter emitter) {
    try {
      emitter.send(SseEmitter.event().comment("heartbeat"));
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  // 기존 호환성을 위한 메서드 (deprecated)
  @Deprecated
  public SseEmitter streamChat(ChatRequest request, String userId) {
    log.warn("streamChat 메서드는 deprecated되었습니다. proxyToPythonServer 사용을 권장합니다.");

    // 임시 구현: 에러 메시지를 반환하는 SSE
    SseEmitter emitter = new SseEmitter(5000L);
    try {
      emitter.send(SseEmitter.event()
          .data("이 메서드는 더 이상 지원되지 않습니다. 새로운 API를 사용해주세요."));
      emitter.complete();
    } catch (IOException e) {
      emitter.completeWithError(e);
    }
    return emitter;
  }
}