package com.hscoderadar.domain.chat.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;

/**
 * 무역 정보 분석 AI 서비스 인터페이스 (v6.1)
 *
 * <p>
 * LangChain4j의 @AiService 기능을 사용하여 Claude AI와의 구조화된 대화를 지원.
 * Spring Boot Starter가 이 인터페이스의 구현체를 자동으로 생성하고 Spring Bean으로 등록.
 */
@AiService
public interface TradeAnalysisAI {

  /**
   * 사용자 질문의 무역 관련 의도 분석
   *
   * @param userMessage 사용자의 자연어 질문
   * @return 분석 결과 (HS_CODE_ANALYSIS, CARGO_TRACKING, GENERAL_TRADE_INFO,
   *         NOT_TRADE_RELATED)
   */
  @SystemMessage("""
      너는 무역 전문가. 사용자의 질문을 분석하여 다음 중 하나의 의도로 분류:

      1. HS_CODE_ANALYSIS: HS Code 분류, 관세율, 품목 분석 관련
      2. CARGO_TRACKING: 화물 추적, 통관 상태 조회 관련
      3. GENERAL_TRADE_INFO: 일반적인 무역 정보, 규제, 절차 관련
      4. NOT_TRADE_RELATED: 무역과 관련 없는 질문

      반드시 위 4가지 중 하나만 정확히 반환.
      """)
  @UserMessage("{{it}}")
  String analyzeTradeIntent(String userMessage);

  /**
   * Thinking 과정 생성 (사고과정 투명화)
   *
   * @param userMessage 사용자 질문
   * @param currentStep 현재 처리 단계
   * @return 현재 수행 중인 작업에 대한 설명
   */
  @SystemMessage("""
      사용자에게 현재 AI가 수행하고 있는 작업을 투명하게 설명.
      한 문장으로 간결하게, 현재 진행 상황을 알림.

      예시:
      - "질문의 의도를 분석하고 있습니다..."
      - "HS Code 관련 최신 정보를 웹에서 검색하고 있습니다..."
      - "수집된 정보를 정리하고 있습니다..."
      - "최종 답변을 생성하고 있습니다..."
      """)
  @UserMessage("""
      사용자 질문: {{userMessage}}
      현재 단계: {{currentStep}}

      현재 수행 중인 작업을 사용자에게 설명.
      """)
  String generateThinkingMessage(String userMessage, String currentStep);
}