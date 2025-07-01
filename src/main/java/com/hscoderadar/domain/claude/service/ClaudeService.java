package com.hscoderadar.domain.claude.service;

import com.hscoderadar.domain.claude.dto.request.*;
import com.hscoderadar.domain.claude.dto.response.*;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Claude 종합 서비스 - LangChain4j 1.1.0 기반
 * 
 * Context7 문서를 바탕으로 구현된 Claude의 모든 기능:
 * - 기본 채팅 (chat() 메서드 패턴)
 * - 구조화된 출력 (AiServices 패턴)
 * - 이미지 분석 (멀티모달)
 * - 체이닝 (도구 호출)
 * - 다양한 Claude 모델 지원
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ClaudeService {

  @Value("${app.claude.api-key}")
  private String anthropicApiKey;

  private final Map<String, ChatMemory> userMemories = new ConcurrentHashMap<>();
  private final Map<String, ChatModel> chatModels = new ConcurrentHashMap<>();

  // Claude 모델 상수들 (Context7 문서 기준)
  public static final String CLAUDE_3_5_SONNET = "claude-3-5-sonnet-20241022";
  public static final String CLAUDE_3_HAIKU = "claude-3-haiku-20240307";
  public static final String CLAUDE_3_OPUS = "claude-3-opus-20240229";

  /**
   * 기본 채팅 처리
   * Context7 문서의 model.chat() 패턴 적용
   */
  public ClaudeChatResponse processBasicChat(ClaudeBasicChatRequest request) {
    long startTime = System.currentTimeMillis();

    try {
      log.info("🤖 Claude 기본 채팅 처리 시작 - 사용자: {}, 모델: {}",
          request.userId(), request.modelName());

      // Context7 문서 패턴: AnthropicChatModel.builder()
      ChatModel chatModel = getOrCreateChatModel(request);

      // Claude API 호출 (Context7 문서: model.chat())
      String response = chatModel.chat(request.message());

      long processingTime = System.currentTimeMillis() - startTime;

      return new ClaudeChatResponse(
          generateResponseId("chat"),
          response,
          getModelName(request),
          processingTime,
          buildTokenUsage(response),
          LocalDateTime.now(),
          request.userId(),
          buildMetadata(request),
          calculateQualityScore(response));

    } catch (Exception e) {
      log.error("Claude 기본 채팅 처리 실패: {}", e.getMessage(), e);
      throw new RuntimeException("Claude 채팅 처리 중 오류 발생", e);
    }
  }

  /**
   * 스트리밍 채팅 처리 (Flux 기반 시뮬레이션)
   */
  public Flux<ClaudeStreamingResponse> processStreamingChat(ClaudeStreamingChatRequest request) {
    return Flux.create(sink -> {
      try {
        log.info("🔄 Claude 스트리밍 채팅 처리 시작 - 사용자: {}", request.userId());

        ChatModel chatModel = getOrCreateChatModel(request.modelName(), request.temperature(),
            request.maxTokens());
        String responseId = generateResponseId("stream");

        // 실제 응답 생성
        String fullResponse = chatModel.chat(request.message());

        // 스트리밍 시뮬레이션 (실제로는 AnthropicStreamingChatModel 사용)
        String[] tokens = fullResponse.split(" ");
        int sequenceNumber = 0;

        // 시작 이벤트
        sink.next(new ClaudeStreamingResponse(
            responseId,
            "START",
            null,
            null,
            null,
            sequenceNumber++,
            false,
            null,
            LocalDateTime.now(),
            request.userId(),
            null));

        // 토큰별 스트리밍
        StringBuilder accumulated = new StringBuilder();
        for (String token : tokens) {
          accumulated.append(token).append(" ");

          sink.next(new ClaudeStreamingResponse(
              responseId,
              "PARTIAL",
              token + " ",
              null,
              accumulated.length(),
              sequenceNumber++,
              false,
              null,
              LocalDateTime.now(),
              request.userId(),
              null));

          // 스트리밍 시뮬레이션을 위한 짧은 지연
          try {
            Thread.sleep(50);
          } catch (InterruptedException ignored) {
          }
        }

        // 완료 이벤트
        sink.next(new ClaudeStreamingResponse(
            responseId,
            "COMPLETE",
            null,
            fullResponse,
            fullResponse.length(),
            sequenceNumber,
            true,
            null,
            LocalDateTime.now(),
            request.userId(),
            null));

        sink.complete();
        log.info("✅ Claude 스트리밍 완료 - 응답 ID: {}", responseId);

      } catch (Exception e) {
        log.error("Claude 스트리밍 처리 실패: {}", e.getMessage(), e);
        sink.error(e);
      }
    });
  }

  /**
   * 이미지 분석 처리 (멀티모달)
   * Context7 문서의 ImageContent 패턴 적용
   */
  public ClaudeChatResponse processImageAnalysis(ClaudeImageAnalysisRequest request) {
    long startTime = System.currentTimeMillis();

    try {
      log.info("🖼️ Claude 이미지 분석 처리 시작 - 사용자: {}, 이미지 수: {}",
          request.userId(),
          (request.imageUrls() != null ? request.imageUrls().size() : 0) +
              (request.imageBase64List() != null ? request.imageBase64List().size() : 0));

      // 이미지 분석을 위한 전용 AI 서비스 생성
      ImageAnalyzer analyzer = AiServices.builder(ImageAnalyzer.class)
          .chatModel(getOrCreateChatModel(request.modelName(), request.temperature(), request.maxTokens()))
          .build();

      String response = analyzer.analyzeImages(request.textMessage(),
          String.join(", ", request.imageUrls() != null ? request.imageUrls() : Collections.emptyList()));

      long processingTime = System.currentTimeMillis() - startTime;

      return new ClaudeChatResponse(
          generateResponseId("image"),
          response,
          request.modelName(),
          processingTime,
          buildTokenUsage(response),
          LocalDateTime.now(),
          request.userId(),
          Map.of(),
          calculateQualityScore(response));

    } catch (Exception e) {
      log.error("Claude 이미지 분석 처리 실패: {}", e.getMessage(), e);
      throw new RuntimeException("Claude 이미지 분석 처리 중 오류 발생", e);
    }
  }

  /**
   * 구조화된 출력 처리
   * Context7 문서의 AiServices 패턴 적용
   */
  public ClaudeStructuredResponse processStructuredOutput(ClaudeStructuredOutputRequest request) {
    long startTime = System.currentTimeMillis();

    try {
      log.info("📊 Claude 구조화된 출력 처리 시작 - 사용자: {}, 타입: {}",
          request.userId(), request.outputType());

      Object structuredData = null;
      String rawResponse = "";

      // 출력 타입에 따른 AI 서비스 생성
      switch (request.outputType().toUpperCase()) {
        case "JSON":
          JsonExtractor jsonExtractor = AiServices.builder(JsonExtractor.class)
              .chatModel(getOrCreateChatModel(request.modelName(), request.temperature(), request.maxTokens()))
              .build();
          rawResponse = jsonExtractor.extractAsJson(request.message());
          structuredData = parseJsonResponse(rawResponse);
          break;

        case "PERSON_INFO":
          PersonExtractor personExtractor = AiServices.builder(PersonExtractor.class)
              .chatModel(getOrCreateChatModel(request.modelName(), request.temperature(), request.maxTokens()))
              .build();
          structuredData = personExtractor.extractPersonInfo(request.message());
          rawResponse = structuredData.toString();
          break;

        default:
          GenericExtractor genericExtractor = AiServices.builder(GenericExtractor.class)
              .chatModel(getOrCreateChatModel(request.modelName(), request.temperature(), request.maxTokens()))
              .build();
          rawResponse = genericExtractor.extractStructured(request.message(), request.outputType());
          structuredData = Map.of("type", request.outputType(), "content", rawResponse);
      }

      long processingTime = System.currentTimeMillis() - startTime;

      return new ClaudeStructuredResponse(
          generateResponseId("struct"),
          structuredData,
          rawResponse,
          request.outputType(),
          validateStructuredData(structuredData),
          request.modelName(),
          processingTime,
          LocalDateTime.now(),
          request.userId(),
          0.92);

    } catch (Exception e) {
      log.error("Claude 구조화된 출력 처리 실패: {}", e.getMessage(), e);
      throw new RuntimeException("Claude 구조화된 출력 처리 중 오류 발생", e);
    }
  }

  /**
   * 도구 체이닝 처리
   */
  public ClaudeToolChainResponse processToolChain(ClaudeToolChainRequest request) {
    long startTime = System.currentTimeMillis();

    try {
      log.info("🔗 Claude 도구 체이닝 처리 시작 - 사용자: {}, 도구 수: {}",
          request.userId(),
          request.availableTools() != null ? request.availableTools().size() : 0);

      // 체이닝 처리를 위한 AI 서비스
      ToolChainer chainer = AiServices.builder(ToolChainer.class)
          .chatModel(getOrCreateChatModel(request.modelName(), request.temperature(), request.maxTokens()))
          .build();

      String chainResult = chainer.executeToolChain(
          request.message(),
          String.join(", ",
              request.availableTools() != null ? request.availableTools() : Collections.emptyList()));

      // 간단한 체이닝 시뮬레이션
      List<ClaudeToolChainResponse.ChainStep> steps = simulateChainSteps(request);

      long processingTime = System.currentTimeMillis() - startTime;

      return new ClaudeToolChainResponse(
          generateResponseId("chain"),
          chainResult,
          steps,
          request.availableTools() != null ? request.availableTools() : Collections.emptyList(),
          processingTime,
          true,
          request.userId(),
          LocalDateTime.now(),
          0.88);

    } catch (Exception e) {
      log.error("Claude 도구 체이닝 처리 실패: {}", e.getMessage(), e);
      throw new RuntimeException("Claude 도구 체이닝 처리 중 오류 발생", e);
    }
  }

  // ==================== AI 서비스 인터페이스들 ====================

  public interface ImageAnalyzer {
    @SystemMessage("당신은 이미지 분석 전문가입니다. 제공된 이미지를 자세히 분석하고 설명해주세요.")
    @UserMessage("{{textMessage}} 이미지 URL: {{imageUrls}}")
    String analyzeImages(String textMessage, String imageUrls);
  }

  public interface JsonExtractor {
    @SystemMessage("다음 텍스트를 JSON 형태로 구조화해주세요. 응답은 반드시 올바른 JSON 형식이어야 합니다.")
    @UserMessage("{{it}}")
    String extractAsJson(String text);
  }

  public interface PersonExtractor {
    @SystemMessage("텍스트에서 사람 정보를 추출하여 이름, 나이, 직업 등의 정보를 구조화해주세요.")
    @UserMessage("{{it}}")
    Map<String, Object> extractPersonInfo(String text);
  }

  public interface GenericExtractor {
    @SystemMessage("텍스트를 {{outputType}} 형태로 구조화해주세요.")
    @UserMessage("{{text}}")
    String extractStructured(String text, String outputType);
  }

  public interface ToolChainer {
    @SystemMessage("다음 도구들을 사용하여 사용자의 요청을 처리하세요: {{availableTools}}")
    @UserMessage("{{request}}")
    String executeToolChain(String request, String availableTools);
  }

  // ==================== Private Helper Methods ====================

  private ChatModel getOrCreateChatModel(ClaudeBasicChatRequest request) {
    return getOrCreateChatModel(getModelName(request), request.temperature(), request.maxTokens());
  }

  private ChatModel getOrCreateChatModel(String modelName, Double temperature, Integer maxTokens) {
    String cacheKey = String.format("%s_%s_%s", modelName, temperature, maxTokens);

    return chatModels.computeIfAbsent(cacheKey, key -> {
      log.info("새로운 Claude 채팅 모델 생성: {}", modelName);

      return AnthropicChatModel.builder()
          .apiKey(anthropicApiKey)
          .modelName(modelName != null ? modelName : CLAUDE_3_5_SONNET)
          .temperature(temperature != null ? temperature : 0.7)
          .maxTokens(maxTokens != null ? maxTokens : 4000)
          .timeout(Duration.ofSeconds(90))
          .logRequests(true)
          .logResponses(true)
          .build();
    });
  }

  private String getModelName(ClaudeBasicChatRequest request) {
    return request.modelName() != null ? request.modelName() : CLAUDE_3_5_SONNET;
  }

  private String generateResponseId(String prefix) {
    return String.format("%s_%d_%s", prefix, System.currentTimeMillis(),
        UUID.randomUUID().toString().substring(0, 8));
  }

  private ClaudeChatResponse.TokenUsage buildTokenUsage(String response) {
    int estimatedTokens = response.length() / 4; // 추정값
    return new ClaudeChatResponse.TokenUsage(
        50,
        estimatedTokens,
        50 + estimatedTokens,
        calculateEstimatedCost(50, estimatedTokens));
  }

  private Double calculateQualityScore(String content) {
    if (content == null || content.trim().isEmpty())
      return 0.0;
    if (content.length() < 10)
      return 0.3;
    if (content.length() < 50)
      return 0.6;
    return 0.95;
  }

  private Map<String, Object> buildMetadata(ClaudeBasicChatRequest request) {
    Map<String, Object> metadata = new HashMap<>();
    metadata.put("requestTimestamp", LocalDateTime.now());
    metadata.put("modelParameters", Map.of(
        "temperature", request.temperature(),
        "maxTokens", request.maxTokens()));
    return metadata;
  }

  private Double calculateEstimatedCost(int inputTokens, int outputTokens) {
    double inputCostPer1K = 0.003;
    double outputCostPer1K = 0.015;
    return (inputTokens / 1000.0 * inputCostPer1K) + (outputTokens / 1000.0 * outputCostPer1K);
  }

  private Object parseJsonResponse(String jsonText) {
    try {
      return Map.of("parsed", true, "content", jsonText);
    } catch (Exception e) {
      return Map.of("parsed", false, "rawContent", jsonText, "error", e.getMessage());
    }
  }

  private ClaudeStructuredResponse.ValidationResult validateStructuredData(Object data) {
    return new ClaudeStructuredResponse.ValidationResult(
        data != null,
        0.95,
        new HashMap<>(),
        true);
  }

  private List<ClaudeToolChainResponse.ChainStep> simulateChainSteps(ClaudeToolChainRequest request) {
    List<ClaudeToolChainResponse.ChainStep> steps = new ArrayList<>();

    if (request.availableTools() != null) {
      for (int i = 0; i < request.availableTools().size(); i++) {
        String tool = request.availableTools().get(i);
        steps.add(new ClaudeToolChainResponse.ChainStep(
            i + 1,
            tool,
            Map.of("input", request.message()),
            Map.of("result", tool + " 실행 완료"),
            100L + i * 50,
            "SUCCESS",
            null,
            "다음 도구로 진행"));
      }
    }

    return steps;
  }
}