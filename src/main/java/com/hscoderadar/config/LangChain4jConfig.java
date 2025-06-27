package com.hscoderadar.config;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * v4.0 LangChain4j 설정 클래스
 *
 * <p>ChatGPT 스타일 통합 채팅을 위한 LangChain4j 컴포넌트들을 Spring Bean으로 구성 - Claude AI 모델 설정 (내장 웹검색 기능 활용) -
 * 채팅 메모리 관리 - AI 서비스 체이닝
 */
@Configuration
@Slf4j
public class LangChain4jConfig {

  @Value("${app.claude.api-key}")
  private String anthropicApiKey;

  @Value("${app.claude.model:claude-sonnet-4-20250514}")
  private String modelName;

  @Value("${app.claude.max-tokens:64000}")
  private Integer maxTokens;

  @Value("${app.claude.temperature:0.7}")
  private Double temperature;

  @Value("${app.chat.memory-max-messages:20}")
  private Integer maxMessages;

  /**
   * Claude AI 채팅 모델 Bean 설정 (v1.1.0 기준)
   *
   * <p>LangChain4j 1.1.0부터 ChatLanguageModel이 ChatModel로 변경됨 Anthropic Claude 모델을 수동으로 구성
   */
  @Bean("claudeChatModel")
  public ChatModel claudeChatModel() {
    log.info("🤖 Claude AI 모델 초기화 - model: {}, maxTokens: {}", modelName, maxTokens);

    return AnthropicChatModel.builder()
        .apiKey(anthropicApiKey)
        .modelName(modelName)
        .maxTokens(maxTokens)
        .temperature(temperature)
        .timeout(Duration.ofSeconds(90))
        .logRequests(true)
        .logResponses(true)
        .build();
  }

  /**
   * 채팅 메모리 Bean 설정
   *
   * <p>대화 컨텍스트를 유지하여 연속적인 질문에 대해 이전 대화 내용을 고려한 답변 생성
   */
  @Bean("customChatMemory")
  public ChatMemory chatMemory() {
    log.info("💭 채팅 메모리 초기화 - maxMessages: {}", maxMessages);

    return MessageWindowChatMemory.withMaxMessages(maxMessages);
  }

  /**
   * 무역 정보 분석 AI 서비스 인터페이스
   *
   * <p>LangChain4j의 @UserMessage, @SystemMessage 어노테이션을 사용하여 Claude AI와의 구조화된 대화를 지원하는 인터페이스
   *
   * <p>🌟 Claude의 내장 웹검색 기능을 자동으로 활용합니다!
   */
  public interface TradeAnalysisAI {

    /**
     * 사용자 질문의 무역 관련 의도 분석
     *
     * @param userMessage 사용자의 자연어 질문
     * @return 분석 결과 (HS_CODE_ANALYSIS, CARGO_TRACKING, GENERAL_TRADE_INFO, NOT_TRADE_RELATED)
     */
    @dev.langchain4j.service.SystemMessage(
        """
                너는 무역 전문가. 사용자의 질문을 분석하여 다음 중 하나의 의도로 분류:

                1. HS_CODE_ANALYSIS: HS Code 분류, 관세율, 품목 분석 관련
                2. CARGO_TRACKING: 화물 추적, 통관 상태 조회 관련
                3. GENERAL_TRADE_INFO: 일반적인 무역 정보, 규제, 절차 관련
                4. NOT_TRADE_RELATED: 무역과 관련 없는 질문

                반드시 위 4가지 중 하나만 정확히 반환.
                """)
    @dev.langchain4j.service.UserMessage("{{it}}")
    String analyzeTradeIntent(String userMessage);

    /**
     * HS Code 분석 및 답변 생성 (Claude 4 Sonnet 내장 웹검색 활용)
     *
     * @param userMessage 사용자 질문
     * @return 구조화된 HS Code 분석 답변
     */
    @dev.langchain4j.service.SystemMessage(
        """
                **[IMPORTANT: 웹 검색 필수 수행]**
                너는 HS Code 분류 전문가. 다음 단계를 반드시 따라 답변:

                **Step 1: 실시간 웹검색 수행 (필수)**
                - 관세청(customs.go.kr), 유니패스(unipass.customs.go.kr)에서 최신 HS Code 정보 검색
                - KOTRA, 한국무역협회에서 품목별 수출입 규제 정보 검색
                - 최신 관세율표 및 FTA 협정세율 정보 검색

                **Step 2: 검색된 정보를 바탕으로 구조화된 답변 생성**

                ## 📋 기본 정보
                - **품목**: [정확한 품목명]
                - **HS Code**: [8자리 또는 10자리 분류번호]
                - **기본관세율**: [%] / **협정세율**: [해당 FTA별 세율]

                ## 📊 상세 분석
                [웹검색으로 확인된 최신 분류 근거와 관세 정보]

                ## 🔍 실시간 확인 정보
                - 수출입 허가/신고 요건: [웹검색 결과]
                - 검역/검사 대상 여부: [최신 규제 정보]
                - 원산지증명서 요구사항: [FTA별 요건]

                ## 📌 웹검색 출처 (실제 접속 확인)
                [검색으로 확인한 공식 URL들과 접속 일시]

                **🌐 웹검색 검증 표시**: "✅ [날짜 시간] 실시간 웹검색으로 확인된 정보입니다"
                **⚠️ 주의**: 최종 결정 전 관세청 또는 관세사 재확인 권장
                """)
    @dev.langchain4j.service.UserMessage("{{it}}")
    String generateHsCodeAnalysis(String userMessage);

    /**
     * 화물 추적 정보 해석 (필요시 웹검색 자동 수행)
     *
     * @param userMessage 사용자 질문
     * @param trackingData 추적 시스템 데이터
     * @return 자연어로 해석된 화물 상태
     */
    @dev.langchain4j.service.SystemMessage(
        """
                너는 화물 추적 전문가. 화물 추적 데이터를 분석하여 사용자가 이해하기 쉽게 설명:

                ## 🚛 현재 상태
                [화물의 현재 위치와 단계]

                ## ⏰ 진행 상황
                [지금까지의 진행 과정과 예상 일정]

                ## 📋 다음 단계
                [앞으로 예상되는 절차와 소요 시간]

                ## ⚠️ 주의사항
                [필요한 조치나 확인 사항]

                필요하다면 최신 통관 정보를 웹에서 검색하여 더 정확한 정보를 제공.
                기술적인 용어는 일반인이 이해할 수 있도록 쉽게 설명.
                """)
    @dev.langchain4j.service.UserMessage(
        """
                사용자 질문: {{userMessage}}

                추적 데이터: {{trackingData}}

                위 화물 정보를 사용자가 이해하기 쉽게 해석.
                """)
    String interpretCargoTracking(String userMessage, String trackingData);

    /**
     * 일반 무역 정보 답변 생성 (Claude 4 Sonnet 실시간 웹검색 활용)
     *
     * @param userMessage 사용자 질문
     * @return 최신 정보가 반영된 포괄적인 무역 정보 답변
     */
    @dev.langchain4j.service.SystemMessage(
        """
                **[IMPORTANT: 실시간 웹검색 필수]**
                너는 무역 컨설턴트. 다음 단계를 반드시 수행:

                **Step 1: 실시간 정보 수집 (필수)**
                - 관세청, 산업통상자원부, KOTRA 최신 공지사항 검색
                - 해당 분야 최신 규제변경, FTA 협정 업데이트 확인
                - 관련 업계 동향 및 정책 변화 검색

                **Step 2: 검색 결과 기반 종합 답변**

                ## 🎯 핵심 답변 (웹검색 확인)
                [실시간 검색으로 확인된 최신 정보 기반 답변]

                ## 📊 최신 동향 분석
                [웹검색으로 확인한 최근 정책 변화 및 업계 동향]

                ## 💡 실무 적용 가이드
                [검색된 최신 규정에 따른 실무 조치사항]

                ## 🔄 최근 변경사항
                [웹검색으로 확인한 최근 법령/규정 변경 내용]

                ## ⚠️ 중요 체크포인트
                [실시간 확인된 주의사항 및 필수 확인 요소]

                ## 📌 실시간 검색 출처
                [실제 접속 확인한 공식 사이트 URL과 최종 확인 시각]

                **🌐 웹검색 신뢰성 표시**: "✅ [현재 날짜시간] 실시간 웹검색 기반 최신 정보"
                **📞 추가 확인**: 정확한 실무 적용을 위해 해당 기관 직접 문의 권장
                """)
    @dev.langchain4j.service.UserMessage("{{it}}")
    String generateGeneralTradeResponse(String userMessage);

    /**
     * Thinking 과정 생성 (사고과정 투명화)
     *
     * @param userMessage 사용자 질문
     * @param currentStep 현재 처리 단계
     * @return 현재 수행 중인 작업에 대한 설명
     */
    @dev.langchain4j.service.SystemMessage(
        """
                사용자에게 현재 AI가 수행하고 있는 작업을 투명하게 설명.
                한 문장으로 간결하게, 현재 진행 상황을 알림.

                예시:
                - "질문의 의도를 분석하고 있습니다..."
                - "HS Code 관련 최신 정보를 웹에서 검색하고 있습니다..."
                - "수집된 정보를 정리하고 있습니다..."
                - "최종 답변을 생성하고 있습니다..."
                """)
    @dev.langchain4j.service.UserMessage(
        """
                사용자 질문: {{userMessage}}
                현재 단계: {{currentStep}}

                현재 수행 중인 작업을 사용자에게 설명.
                """)
    String generateThinkingMessage(String userMessage, String currentStep);
  }

  /**
   * 무역 분석 AI 서비스 Bean 설정
   *
   * <p>LangChain4j AiServices를 사용하여 TradeAnalysisAI 인터페이스의 구현체를 자동 생성하고 Claude AI 모델과 연결
   */
  @Bean
  public TradeAnalysisAI tradeAnalysisAI(
      @Qualifier("claudeChatModel") ChatModel chatModel,
      @Qualifier("customChatMemory") ChatMemory chatMemory) {

    log.info("🚀 무역 분석 AI 서비스 초기화 (Claude 웹검색 내장)");

    return AiServices.builder(TradeAnalysisAI.class)
        .chatModel(chatModel) // 명시적으로 ChatModel 지정
        .chatMemory(chatMemory)
        .build();
  }
}
