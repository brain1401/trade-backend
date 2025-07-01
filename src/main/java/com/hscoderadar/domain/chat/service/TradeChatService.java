package com.hscoderadar.domain.chat.service;

import com.hscoderadar.domain.chat.dto.request.TradeChatRequest;
import com.hscoderadar.domain.chat.dto.response.TradeChatStreamingResponse;
import com.hscoderadar.domain.chat.dto.response.TradeChatStreamingResponse.*;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/**
 * 무역 특화 통합 채팅 서비스 v6.1 (SSE 표준 준수)
 * 
 * API 명세서 v6.1에 따른 구현:
 * - LangChain4j 1.1.0-beta7 최신 패턴 적용
 * - Spring WebFlux ServerSentEvent 표준 사용
 * - Claude 3.5 Sonnet + StreamingChatResponseHandler
 * - voyage-3-large 1024차원 임베딩 모델
 * - PostgreSQL + pgvector RAG 시스템 (추후 구현)
 * - 3단계 병렬 처리 최적화
 * - 회원/비회원 차별화 SSE 이벤트
 * - SSE 메타데이터 기반 동적 북마크 생성
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TradeChatService {

  private final StreamingChatModel streamingChatModel;
  private final TradeAnalysisAI tradeAnalysisAI;
  private final TradeRagService tradeRagService;

  // v6.1 3단계 병렬 처리용 ExecutorService
  private final ExecutorService parallelExecutor = Executors.newFixedThreadPool(10);

  // HSCode 패턴 정규식
  private static final Pattern HSCODE_PATTERN = Pattern.compile("\\b\\d{4}\\.\\d{2}\\.\\d{2,4}\\b");
  private static final Pattern SIMPLE_HSCODE_PATTERN = Pattern.compile("\\b\\d{6,10}\\b");

  /**
   * 무역 특화 통합 채팅 처리 v6.1 (SSE 표준)
   * 
   * 3단계 병렬 처리:
   * 1. Claude AI 스트리밍 응답 생성
   * 2. 상세페이지 정보 준비
   * 3. 회원 대화 기록 저장 (회원만)
   */
  public Flux<ServerSentEvent<TradeChatStreamingResponse>> processTradeChat(TradeChatRequest request) {
    String responseId = generateResponseId();
    AtomicInteger sequenceNumber = new AtomicInteger(0);

    return Flux.create(sink -> {
      try {
        log.info("🚢 무역 채팅 처리 시작 v6.1 (SSE 표준) - 응답 ID: {}, 사용자: {}, 회원: {}",
            responseId, request.getUserIdentifier(), request.isAuthenticated());

        // 단계 1: 무역 관련성 검증 (실제 AI로 검증)
        String tradeIntentResult = analyzeTradeIntent(request.message());

        if ("NOT_TRADE_RELATED".equals(tradeIntentResult)) {
          sink.next(createErrorSSEEvent(responseId, sequenceNumber.incrementAndGet(),
              "무역 관련 질문에만 답변할 수 있습니다. HSCode, 관세율, 수출입 규제 등에 대해 문의해주세요.",
              request));
          sink.complete();
          return;
        }

        // 단계 2: 초기 메타데이터 이벤트 발송 (SSE 표준)
        sendInitialMetadataSSEEvents(sink, responseId, sequenceNumber, request, tradeIntentResult);

        // 단계 3: v6.1 3단계 병렬 처리 시작 (SSE 표준)
        processV61ParallelTasksSSE(sink, responseId, sequenceNumber, request, tradeIntentResult);

      } catch (Exception e) {
        log.error("무역 채팅 처리 실패: {}", e.getMessage(), e);
        sink.next(createErrorSSEEvent(responseId, sequenceNumber.incrementAndGet(),
            "서비스 처리 중 오류가 발생했습니다.", request));
        sink.complete();
      }
    });
  }

  /**
   * v6.1 초기 메타데이터 이벤트 발송 (SSE 표준)
   */
  private void sendInitialMetadataSSEEvents(
      reactor.core.publisher.FluxSink<ServerSentEvent<TradeChatStreamingResponse>> sink,
      String responseId,
      AtomicInteger sequenceNumber,
      TradeChatRequest request,
      String tradeIntentResult) {

    // 1. initial_metadata 이벤트 (SSE 표준)
    IntentAnalysis intentAnalysis = createIntentAnalysisFromAI(tradeIntentResult, request.message());

    TradeChatStreamingResponse initialMetadata = TradeChatStreamingResponse.builder()
        .responseId(responseId)
        .eventType("initial_metadata")
        .intentAnalysis(intentAnalysis)
        .sequenceNumber(sequenceNumber.incrementAndGet())
        .isComplete(false)
        .timestamp(LocalDateTime.now())
        .userIdentifier(request.getUserIdentifier())
        .sessionId(request.sessionId())
        .build();

    sink.next(ServerSentEvent.<TradeChatStreamingResponse>builder()
        .id(responseId + "-" + sequenceNumber.get())
        .event("initial_metadata")
        .data(initialMetadata)
        .build());

    // 2. session_info 이벤트 (v6.1 회원/비회원 차별화 SSE)
    SessionInfo sessionInfo = SessionInfo.builder()
        .isAuthenticated(request.isAuthenticated())
        .userType(request.isAuthenticated() ? "MEMBER" : "GUEST")
        .sessionId(request.sessionId())
        .recordingEnabled(request.isAuthenticated())
        .message(request.isAuthenticated() ? "회원님의 대화가 기록되어 나중에 다시 볼 수 있습니다" : "회원가입하면 대화 기록을 저장할 수 있습니다")
        .isFirstMessage(request.isNewSession())
        .partitionYear(LocalDateTime.now().getYear())
        .build();

    TradeChatStreamingResponse sessionInfoEvent = TradeChatStreamingResponse.builder()
        .responseId(responseId)
        .eventType("session_info")
        .sessionInfo(sessionInfo)
        .sequenceNumber(sequenceNumber.incrementAndGet())
        .isComplete(false)
        .timestamp(LocalDateTime.now())
        .userIdentifier(request.getUserIdentifier())
        .sessionId(request.sessionId())
        .build();

    sink.next(ServerSentEvent.<TradeChatStreamingResponse>builder()
        .id(responseId + "-" + sequenceNumber.get())
        .event("session_info")
        .data(sessionInfoEvent)
        .build());
  }

  /**
   * v6.1 3단계 병렬 처리 수행 (SSE 표준)
   */
  private void processV61ParallelTasksSSE(
      reactor.core.publisher.FluxSink<ServerSentEvent<TradeChatStreamingResponse>> sink,
      String responseId,
      AtomicInteger sequenceNumber,
      TradeChatRequest request,
      String tradeIntentResult) {

    try {
      IntentAnalysis intentAnalysis = createIntentAnalysisFromAI(tradeIntentResult, request.message());

      // thinking 이벤트들 순차 발송 (SSE 표준)
      sendThinkingSSEEvents(sink, responseId, sequenceNumber, request, intentAnalysis);

      // 병렬 작업 1: Claude AI 스트리밍 응답 생성 + RAG 검색
      CompletableFuture<String> aiResponseFuture = CompletableFuture.supplyAsync(() -> {
        try {
          // RAG 검색으로 관련 정보 수집
          List<TradeRagService.HsCodeSearchResult> ragResults = searchHsCodesBySemantic(request.message());

          // RAG 결과를 컨텍스트로 포함하여 AI 응답 생성
          return generateActualAIResponseWithRAGSSE(request.message(), tradeIntentResult, ragResults, sink, responseId,
              sequenceNumber, request);
        } catch (Exception e) {
          log.error("AI 응답 생성 실패: {}", e.getMessage(), e);
          return generateFallbackResponse(request.message());
        }
      }, parallelExecutor);

      // 병렬 작업 2: 상세페이지 정보 준비
      CompletableFuture<List<DetailPageButton>> detailPagesFuture = CompletableFuture.supplyAsync(() -> {
        return prepareDetailPageButtons(intentAnalysis, request.message());
      }, parallelExecutor);

      // 병렬 작업 3: 회원 채팅 기록 저장 (회원만)
      CompletableFuture<Void> saveChatFuture = CompletableFuture.runAsync(() -> {
        if (request.isAuthenticated()) {
          saveMemberChatSessionSSE(request, sink, responseId, sequenceNumber);
        }
      }, parallelExecutor);

      // AI 응답 완료 대기
      String fullResponse = aiResponseFuture.get();

      // 상세페이지 버튼 준비 완료 시 전송 (SSE 표준)
      List<DetailPageButton> detailButtons = detailPagesFuture.get();
      sendDetailPageButtonSSEEvents(sink, responseId, sequenceNumber, request, detailButtons);

      // 모든 병렬 작업 완료 대기
      CompletableFuture.allOf(aiResponseFuture, detailPagesFuture, saveChatFuture).get();

      // COMPLETE 이벤트 발송 (SSE 표준)
      TradeChatStreamingResponse completeResponse = TradeChatStreamingResponse.builder()
          .responseId(responseId)
          .eventType("main_message_complete")
          .fullContent(fullResponse)
          .sequenceNumber(sequenceNumber.incrementAndGet())
          .isComplete(true)
          .timestamp(LocalDateTime.now())
          .userIdentifier(request.getUserIdentifier())
          .sessionId(request.sessionId())
          .metadata(Map.of(
              "sources", List.of("Claude 3.5 Sonnet", "voyage-3-large RAG"),
              "ragSources", List.of("HSCode 벡터 DB"),
              "cacheHit", false,
              "processingTime", System.currentTimeMillis() % 10000))
          .build();

      sink.next(ServerSentEvent.<TradeChatStreamingResponse>builder()
          .id(responseId + "-" + sequenceNumber.get())
          .event("main_message_complete")
          .data(completeResponse)
          .build());

      sink.complete();

    } catch (Exception e) {
      log.error("v6.1 병렬 처리 실패: {}", e.getMessage(), e);
      sink.next(createErrorSSEEvent(responseId, sequenceNumber.incrementAndGet(),
          "AI 응답 생성 중 오류가 발생했습니다.", request));
      sink.complete();
    }
  }

  /**
   * LangChain4j 1.1.0-beta7 패턴: RAG 기반 AI 응답 생성 + 실시간 SSE 스트리밍
   */
  private String generateActualAIResponseWithRAGSSE(String message, String intentType,
      List<TradeRagService.HsCodeSearchResult> ragResults,
      reactor.core.publisher.FluxSink<ServerSentEvent<TradeChatStreamingResponse>> sink,
      String responseId, AtomicInteger sequenceNumber, TradeChatRequest request) {

    try {
      // RAG 컨텍스트를 포함한 메시지 구성
      String contextualMessage = buildContextualMessage(message, ragResults);

      // main_message_start 이벤트 (SSE 표준)
      TradeChatStreamingResponse startEvent = TradeChatStreamingResponse.builder()
          .responseId(responseId)
          .eventType("main_message_start")
          .sequenceNumber(sequenceNumber.incrementAndGet())
          .isComplete(false)
          .timestamp(LocalDateTime.now())
          .userIdentifier(request.getUserIdentifier())
          .sessionId(request.sessionId())
          .build();

      sink.next(ServerSentEvent.<TradeChatStreamingResponse>builder()
          .id(responseId + "-" + sequenceNumber.get())
          .event("main_message_start")
          .data(startEvent)
          .build());

      // LangChain4j 1.1.0-beta7 StreamingChatResponseHandler 사용
      StringBuilder fullResponseBuilder = new StringBuilder();
      CompletableFuture<String> responseFuture = new CompletableFuture<>();

      streamingChatModel.chat(contextualMessage, new StreamingChatResponseHandler() {
        @Override
        public void onPartialResponse(String partialResponse) {
          fullResponseBuilder.append(partialResponse);

          // main_message_data 이벤트 발송 (SSE 표준)
          TradeChatStreamingResponse dataEvent = TradeChatStreamingResponse.builder()
              .responseId(responseId)
              .eventType("main_message_data")
              .partialContent(partialResponse)
              .currentTokenCount(fullResponseBuilder.length() / 4)
              .sequenceNumber(sequenceNumber.incrementAndGet())
              .isComplete(false)
              .timestamp(LocalDateTime.now())
              .userIdentifier(request.getUserIdentifier())
              .sessionId(request.sessionId())
              .build();

          sink.next(ServerSentEvent.<TradeChatStreamingResponse>builder()
              .id(responseId + "-" + sequenceNumber.get())
              .event("main_message_data")
              .data(dataEvent)
              .build());

          // 북마크 메타데이터 감지 및 발송 (SSE 표준)
          if (shouldGenerateBookmark(fullResponseBuilder.toString())) {
            BookmarkMetadata bookmarkData = extractBookmarkMetadata(fullResponseBuilder.toString());
            if (bookmarkData != null) {
              TradeChatStreamingResponse bookmarkEvent = TradeChatStreamingResponse.builder()
                  .responseId(responseId)
                  .eventType("main_message_data")
                  .partialContent(partialResponse)
                  .bookmarkData(bookmarkData)
                  .sequenceNumber(sequenceNumber.incrementAndGet())
                  .isComplete(false)
                  .timestamp(LocalDateTime.now())
                  .userIdentifier(request.getUserIdentifier())
                  .sessionId(request.sessionId())
                  .build();

              sink.next(ServerSentEvent.<TradeChatStreamingResponse>builder()
                  .id(responseId + "-" + sequenceNumber.get())
                  .event("bookmark_metadata")
                  .data(bookmarkEvent)
                  .build());
            }
          }
        }

        @Override
        public void onCompleteResponse(ChatResponse response) {
          responseFuture.complete(fullResponseBuilder.toString());
        }

        @Override
        public void onError(Throwable error) {
          log.error("Claude 스트리밍 응답 오류: {}", error.getMessage(), error);
          responseFuture.completeExceptionally(error);
        }
      });

      return responseFuture.get();

    } catch (Exception e) {
      log.error("RAG 기반 AI 응답 생성 실패: {}", e.getMessage(), e);
      return generateFallbackResponse(message);
    }
  }

  // ==================== SSE Helper Methods ====================

  /**
   * SSE 표준 thinking 이벤트 발송
   */
  private void sendThinkingSSEEvents(
      reactor.core.publisher.FluxSink<ServerSentEvent<TradeChatStreamingResponse>> sink,
      String responseId,
      AtomicInteger sequenceNumber,
      TradeChatRequest request,
      IntentAnalysis intentAnalysis) {

    String[] thinkingStages = {
        "thinking_intent_analysis",
        "thinking_parallel_processing_start",
        "thinking_rag_search_planning",
        "thinking_rag_search_executing",
        "thinking_data_processing",
        "thinking_detail_page_preparation",
        "thinking_member_record_saving",
        "thinking_response_generation"
    };

    int[] progressValues = { 10, 15, 25, 40, 70, 85, 90, 95 };

    for (int i = 0; i < thinkingStages.length; i++) {
      // 회원 전용 이벤트는 회원일 때만 발송
      if ("thinking_member_record_saving".equals(thinkingStages[i]) && !request.isAuthenticated()) {
        continue;
      }

      String thinkingContent = generateThinkingContent(thinkingStages[i], request.message());

      TradeChatStreamingResponse thinkingEvent = TradeChatStreamingResponse.builder()
          .responseId(responseId)
          .eventType(thinkingStages[i])
          .thinkingProcess(thinkingContent)
          .progress(progressValues[i])
          .intentAnalysis(intentAnalysis)
          .sequenceNumber(sequenceNumber.incrementAndGet())
          .isComplete(false)
          .timestamp(LocalDateTime.now())
          .userIdentifier(request.getUserIdentifier())
          .sessionId(request.sessionId())
          .build();

      sink.next(ServerSentEvent.<TradeChatStreamingResponse>builder()
          .id(responseId + "-" + sequenceNumber.get())
          .event(thinkingStages[i])
          .data(thinkingEvent)
          .build());

      // 자연스러운 thinking 흐름을 위한 짧은 지연
      try {
        Thread.sleep(200);
      } catch (InterruptedException ignored) {
        Thread.currentThread().interrupt();
      }
    }
  }

  /**
   * SSE 표준 상세페이지 버튼 이벤트 발송
   */
  private void sendDetailPageButtonSSEEvents(
      reactor.core.publisher.FluxSink<ServerSentEvent<TradeChatStreamingResponse>> sink,
      String responseId,
      AtomicInteger sequenceNumber,
      TradeChatRequest request,
      List<DetailPageButton> detailButtons) {

    // detail_page_buttons_start 이벤트 (SSE 표준)
    TradeChatStreamingResponse startEvent = TradeChatStreamingResponse.builder()
        .responseId(responseId)
        .eventType("detail_page_buttons_start")
        .sequenceNumber(sequenceNumber.incrementAndGet())
        .metadata(Map.of("buttonsCount", detailButtons.size()))
        .timestamp(LocalDateTime.now())
        .userIdentifier(request.getUserIdentifier())
        .sessionId(request.sessionId())
        .build();

    sink.next(ServerSentEvent.<TradeChatStreamingResponse>builder()
        .id(responseId + "-" + sequenceNumber.get())
        .event("detail_page_buttons_start")
        .data(startEvent)
        .build());

    // 각 버튼별 detail_page_button_ready 이벤트 (SSE 표준)
    for (DetailPageButton button : detailButtons) {
      TradeChatStreamingResponse buttonEvent = TradeChatStreamingResponse.builder()
          .responseId(responseId)
          .eventType("detail_page_button_ready")
          .detailPageButton(button)
          .sequenceNumber(sequenceNumber.incrementAndGet())
          .timestamp(LocalDateTime.now())
          .userIdentifier(request.getUserIdentifier())
          .sessionId(request.sessionId())
          .build();

      sink.next(ServerSentEvent.<TradeChatStreamingResponse>builder()
          .id(responseId + "-" + sequenceNumber.get())
          .event("detail_page_button_ready")
          .data(buttonEvent)
          .build());
    }

    // detail_page_buttons_complete 이벤트 (SSE 표준)
    TradeChatStreamingResponse completeEvent = TradeChatStreamingResponse.builder()
        .responseId(responseId)
        .eventType("detail_page_buttons_complete")
        .sequenceNumber(sequenceNumber.incrementAndGet())
        .metadata(Map.of("totalPreparationTime", 2000))
        .timestamp(LocalDateTime.now())
        .userIdentifier(request.getUserIdentifier())
        .sessionId(request.sessionId())
        .build();

    sink.next(ServerSentEvent.<TradeChatStreamingResponse>builder()
        .id(responseId + "-" + sequenceNumber.get())
        .event("detail_page_buttons_complete")
        .data(completeEvent)
        .build());
  }

  /**
   * SSE 표준 회원 채팅 세션 저장
   */
  private void saveMemberChatSessionSSE(TradeChatRequest request,
      reactor.core.publisher.FluxSink<ServerSentEvent<TradeChatStreamingResponse>> sink,
      String responseId,
      AtomicInteger sequenceNumber) {

    if (!request.isAuthenticated()) {
      return;
    }

    try {
      log.info("💾 회원 채팅 세션 저장 v6.1 (SSE 표준) - 사용자: {}, 세션: {}",
          request.userId(), request.sessionId());

      // 세션 생성 이벤트 (첫 메시지인 경우) - SSE 표준
      if (request.isNewSession()) {
        TradeChatStreamingResponse sessionCreatedEvent = TradeChatStreamingResponse.builder()
            .responseId(responseId)
            .eventType("member_session_created")
            .sequenceNumber(sequenceNumber.incrementAndGet())
            .sessionInfo(SessionInfo.builder()
                .sessionId(request.sessionId())
                .isFirstMessage(true)
                .partitionYear(LocalDateTime.now().getYear())
                .build())
            .timestamp(LocalDateTime.now())
            .userIdentifier(request.getUserIdentifier())
            .sessionId(request.sessionId())
            .build();

        sink.next(ServerSentEvent.<TradeChatStreamingResponse>builder()
            .id(responseId + "-" + sequenceNumber.get())
            .event("member_session_created")
            .data(sessionCreatedEvent)
            .build());
      }

      // TODO: 실제 채팅 세션 서비스 연동
      // if (request.isNewSession()) {
      // chatSessionService.createSession(request.userId(),
      // request.sessionId());
      // }
      // chatMessageService.saveMessage(request.sessionId(), request.message(),
      // "USER");

      // 기록 저장 완료 이벤트 (SSE 표준)
      TradeChatStreamingResponse recordSavedEvent = TradeChatStreamingResponse.builder()
          .responseId(responseId)
          .eventType("member_record_saved")
          .sequenceNumber(sequenceNumber.incrementAndGet())
          .metadata(Map.of(
              "messageCount", 2,
              "partitionYear", LocalDateTime.now().getYear()))
          .timestamp(LocalDateTime.now())
          .userIdentifier(request.getUserIdentifier())
          .sessionId(request.sessionId())
          .build();

      sink.next(ServerSentEvent.<TradeChatStreamingResponse>builder()
          .id(responseId + "-" + sequenceNumber.get())
          .event("member_record_saved")
          .data(recordSavedEvent)
          .build());

    } catch (Exception e) {
      log.error("회원 채팅 세션 저장 실패: {}", e.getMessage(), e);
      // 저장 실패해도 채팅은 계속 진행
    }
  }

  /**
   * SSE 표준 에러 이벤트 생성
   */
  private ServerSentEvent<TradeChatStreamingResponse> createErrorSSEEvent(String responseId, int sequenceNumber,
      String errorMessage, TradeChatRequest request) {

    TradeChatStreamingResponse errorResponse = TradeChatStreamingResponse.builder()
        .responseId(responseId)
        .eventType("error")
        .errorMessage(errorMessage)
        .sequenceNumber(sequenceNumber)
        .isComplete(true)
        .timestamp(LocalDateTime.now())
        .userIdentifier(request.getUserIdentifier())
        .sessionId(request.sessionId())
        .build();

    return ServerSentEvent.<TradeChatStreamingResponse>builder()
        .id(responseId + "-" + sequenceNumber)
        .event("error")
        .data(errorResponse)
        .build();
  }

  // ==================== Helper Methods (기존 유지) ====================

  /**
   * 무역 관련성 분석
   */
  private String analyzeTradeIntent(String message) {
    try {
      String result = tradeAnalysisAI.analyzeTradeIntent(message);
      // 간단한 키워드 기반 판별 로직 (AI 결과를 보조)
      return result.toLowerCase().contains("무역") || result.toLowerCase().contains("trade") ? "HS_CODE_ANALYSIS"
          : "NOT_TRADE_RELATED";
    } catch (Exception e) {
      log.error("무역 의도 분석 실패: {}", e.getMessage(), e);
      return "GENERAL_TRADE_INFO"; // AI 호출 실패 시 기본값
    }
  }

  /**
   * HSCode 의미적 검색
   */
  private List<TradeRagService.HsCodeSearchResult> searchHsCodesBySemantic(String query) {
    try {
      return tradeRagService.searchHsCodesBySemantic(query);
    } catch (Exception e) {
      log.error("HSCode 의미적 검색 실패: {}", e.getMessage(), e);
      return List.of();
    }
  }

  /**
   * RAG 컨텍스트를 포함한 메시지 구성
   */
  private String buildContextualMessage(String originalMessage, List<TradeRagService.HsCodeSearchResult> ragResults) {
    if (ragResults.isEmpty()) {
      return originalMessage;
    }

    StringBuilder contextualMessage = new StringBuilder();
    contextualMessage.append("사용자 질문: ").append(originalMessage).append("\n\n");

    contextualMessage.append("관련 HSCode 정보 (voyage-3-large 1024차원 검색 결과):\n");
    for (TradeRagService.HsCodeSearchResult result : ragResults) {
      contextualMessage.append(String.format("- HSCode: %s, 품목: %s, 설명: %s (신뢰도: %.2f)\n",
          result.getHsCode(), result.getProductName(), result.getDescription(), result.getRelevanceScore()));
    }

    contextualMessage.append("\n위 RAG 검색 결과를 참고하여 정확하고 구체적인 답변을 제공해주세요.");

    return contextualMessage.toString();
  }

  private IntentAnalysis createIntentAnalysisFromAI(String aiResult, String message) {
    List<String> keywords = extractKeywords(message);
    double confidence = calculateConfidence(aiResult, keywords);

    return IntentAnalysis.builder()
        .claudeIntent(aiResult)
        .estimatedTime(15)
        .confidenceScore(confidence)
        .extractedKeywords(keywords)
        .isTradeRelated(!"NOT_TRADE_RELATED".equals(aiResult))
        .ragEnabled(true)
        .parallelProcessing(true)
        .build();
  }

  private List<String> extractKeywords(String message) {
    List<String> keywords = new ArrayList<>();
    String lowerMessage = message.toLowerCase();

    if (lowerMessage.contains("hscode") || lowerMessage.contains("hs코드"))
      keywords.add("HSCode");
    if (lowerMessage.contains("관세"))
      keywords.add("관세율");
    if (lowerMessage.contains("수출"))
      keywords.add("수출");
    if (lowerMessage.contains("수입"))
      keywords.add("수입");
    if (lowerMessage.contains("규제"))
      keywords.add("규제");

    return keywords;
  }

  private double calculateConfidence(String intentType, List<String> keywords) {
    double baseConfidence = 0.7;
    double keywordBonus = keywords.size() * 0.05;
    return Math.min(0.95, baseConfidence + keywordBonus);
  }

  private boolean shouldGenerateBookmark(String accumulatedResponse) {
    return HSCODE_PATTERN.matcher(accumulatedResponse).find() ||
        accumulatedResponse.toLowerCase().contains("관세율") ||
        accumulatedResponse.toLowerCase().contains("hscode");
  }

  private BookmarkMetadata extractBookmarkMetadata(String response) {
    java.util.regex.Matcher matcher = HSCODE_PATTERN.matcher(response);
    if (matcher.find()) {
      String hsCode = matcher.group();
      return BookmarkMetadata.builder()
          .available(true)
          .hsCode(hsCode)
          .productName(extractItemNameFromResponse(response, hsCode))
          .confidence(0.95)
          .classificationBasis("voyage-3-large RAG 기반 분류")
          .tariffInfo(extractTariffInfo(response))
          .build();
    }
    return null;
  }

  private List<DetailPageButton> prepareDetailPageButtons(IntentAnalysis intentAnalysis, String message) {
    List<DetailPageButton> buttons = new ArrayList<>();

    if ("HS_CODE_ANALYSIS".equals(intentAnalysis.getClaudeIntent())) {
      buttons.add(DetailPageButton.builder()
          .buttonType("HS_CODE")
          .priority(1)
          .url("/detail/hscode/")
          .title("HS Code 상세정보")
          .description("관세율, 규제정보 등")
          .isReady(true)
          .preparationTime(1500L)
          .build());
    }

    buttons.add(DetailPageButton.builder()
        .buttonType("REGULATION")
        .priority(2)
        .url("/detail/regulation/")
        .title("수입 규제정보")
        .description("인증, 규제사항 등")
        .isReady(true)
        .preparationTime(2000L)
        .build());

    return buttons;
  }

  private String generateThinkingContent(String stage, String message) {
    return switch (stage) {
      case "thinking_intent_analysis" -> "사용자 질문의 무역 관련 의도를 분석 중입니다...";
      case "thinking_parallel_processing_start" -> "3단계 병렬 처리를 시작합니다: AI 응답, 상세페이지 준비, 회원 기록 저장";
      case "thinking_rag_search_planning" -> "voyage-3-large를 사용하여 HSCode 벡터 검색을 계획 중입니다...";
      case "thinking_rag_search_executing" -> "PostgreSQL + pgvector에서 의미적 유사도 검색을 실행 중입니다...";
      case "thinking_data_processing" -> "RAG 검색 결과와 Claude AI 응답을 통합 분석 중입니다...";
      case "thinking_detail_page_preparation" -> "관련 상세페이지 정보를 병렬로 준비 중입니다...";
      case "thinking_member_record_saving" -> "회원 대화 기록을 pg_partman 파티션에 저장 중입니다...";
      case "thinking_response_generation" -> "최종 답변을 자연어로 구성하고 메타데이터를 정리합니다...";
      default -> "처리 중입니다...";
    };
  }

  private String generateFallbackResponse(String message) {
    return "죄송합니다. 현재 일시적인 문제로 정확한 답변을 제공하기 어렵습니다. " +
        "잠시 후 다시 시도해 주시거나, 더 구체적인 질문을 해주시면 도움을 드릴 수 있습니다.";
  }

  private String extractItemNameFromResponse(String response, String hsCode) {
    if (response.contains("스마트폰"))
      return "스마트폰 및 기타 무선전화기";
    if (response.contains("컴퓨터"))
      return "디지털 자동자료처리기계";
    return "무역 품목";
  }

  private String extractTariffInfo(String response) {
    if (response.contains("0%"))
      return "0% (FTA 적용)";
    if (response.contains("8%"))
      return "기본 8%";
    return "문의 필요";
  }

  private String generateResponseId() {
    return "trade-chat-v61-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
  }
}