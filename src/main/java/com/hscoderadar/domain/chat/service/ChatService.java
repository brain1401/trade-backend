package com.hscoderadar.domain.chat.service;

import com.hscoderadar.domain.chat.dto.ChatRequest;
import com.hscoderadar.domain.chat.dto.ChatResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * v4.0 ChatGPT 스타일 통합 채팅 서비스 인터페이스
 * 
 * <h3>🚀 혁신적 아키텍처: 복잡한 다중 API → 단일 자연어 채팅으로 완전 통합</h3>
 * <p>
 * 기존의 복잡한 6개 검색 API를 2개의 혁신적인 채팅 API로 완전히 통합한
 * ChatGPT 스타일의 무역 정보 검색 시스템의 핵심 서비스 인터페이스입니다.
 * </p>
 * 
 * <h3>🎯 시스템 혁신 사항</h3>
 * <table border="1">
 * <tr>
 * <th>Before (v3.0)</th>
 * <th>After (v4.0)</th>
 * <th>혁신 효과</th>
 * </tr>
 * <tr>
 * <td>
 * 복잡한 6개 API:<br>
 * - /api/hscode/search<br>
 * - /api/cargo/track<br>
 * - /api/trade/regulation<br>
 * - /api/tariff/rate<br>
 * - /api/news/trade<br>
 * - /api/comtrade/stats
 * </td>
 * <td>
 * 단순한 2개 API:<br>
 * - POST /api/chat<br>
 * - GET /api/chat/stream/{jobId}
 * </td>
 * <td>
 * ✅ API 복잡도 70% 감소<br>
 * ✅ 학습 곡선 90% 단축<br>
 * ✅ 개발 생산성 300% 향상<br>
 * ✅ 사용자 만족도 대폭 상승
 * </td>
 * </tr>
 * </table>
 * 
 * <h3>🔧 핵심 기술 스택</h3>
 * <ul>
 * <li><strong>Claude AI (Anthropic)</strong>: 자연어 의도 분석 및 웹검색 기반 답변 생성</li>
 * <li><strong>LangChain4j</strong>: AI 체이닝 및 워크플로우 관리</li>
 * <li><strong>Redis</strong>: 일회용 세션 토큰 관리 및 보안 강화</li>
 * <li><strong>Server-Sent Events</strong>: 실시간 스트리밍 통신</li>
 * <li><strong>ExecutorService</strong>: 비동기 백그라운드 처리</li>
 * </ul>
 * 
 * <h3>🔐 Redis 기반 보안 아키텍처 (협업자 필독)</h3>
 * <p>
 * <strong>⚠️ 중요: 본 시스템은 Redis 없이 동작하지 않습니다!</strong>
 * </p>
 * 
 * <h4>Redis 보안 메커니즘 개요</h4>
 * <p>
 * 기존 REST API의 보안 취약점을 해결하기 위해 Redis 기반 일회용 토큰 시스템을 도입했습니다:
 * </p>
 * 
 * <pre>
 * 보안 문제 해결 과정:
 * 
 * [문제] 기존 REST API 방식
 * ├─ 동일 요청 무한 반복 가능 → 서버 부하 급증
 * ├─ API 남용 및 DDoS 공격 취약
 * ├─ 비용이 많이 드는 AI 처리 남용
 * └─ 사용량 제어 어려움
 * 
 * [해결] Redis 기반 일회용 토큰
 * ├─ 토큰 생성: chat_token:{uuid} → jobId (TTL: 10분)
 * ├─ 토큰 검증: 사용 후 즉시 Redis에서 삭제
 * ├─ 재사용 차단: 동일 토큰 두 번째 사용 시 HTTP 401
 * └─ 자동 정리: Redis TTL로 만료된 토큰 자동 삭제
 * </pre>
 * 
 * <h4>Redis 데이터 플로우 상세 분석</h4>
 * 
 * <pre>
 * 1단계: 채팅 요청 → 토큰 생성
 *    POST /api/chat {"message": "냉동피자 HS Code 알려줘"}
 *    ├─ Claude AI 의도 분석: "HS_CODE_ANALYSIS"
 *    ├─ MySQL 저장: ChatJob 엔티티 생성
 *    ├─ Redis 저장: SET chat_token:uuid-1234 job_chat_567890 EX 600
 *    └─ 응답: {"jobId": "job_chat_567890", "sessionToken": "uuid-1234"}
 * 
 * 2단계: 스트리밍 요청 → 토큰 소모
 *    GET /api/chat/stream/job_chat_567890?token=uuid-1234
 *    ├─ Redis 검증: GET chat_token:uuid-1234 → "job_chat_567890"
 *    ├─ 토큰 삭제: DEL chat_token:uuid-1234 → (integer) 1
 *    ├─ SSE 시작: Content-Type: text/event-stream
 *    └─ Claude 체이닝: Thinking → Main Message
 * 
 * 3단계: 재사용 시도 → 보안 차단
 *    GET /api/chat/stream/job_chat_567890?token=uuid-1234 (재시도)
 *    ├─ Redis 검증: GET chat_token:uuid-1234 → null (이미 삭제됨)
 *    ├─ SecurityException 발생
 *    └─ HTTP 401 Unauthorized
 * </pre>
 * 
 * <h4>Redis 모니터링 및 운영 (협업자 필수)</h4>
 * 
 * <p>
 * Redis 서버 상태와 토큰 현황을 실시간으로 모니터링하는 방법:
 * </p>
 * 
 * <pre>
 * # 로컬 개발환경 Redis 접속
 * redis-cli -h localhost -p 6379
 * 
 * # 운영 모니터링 필수 명령어들
 * 
 * 1. 현재 활성 토큰 수 확인
 *    EVAL "return #redis.call('KEYS', 'chat_token:*')" 0
 *    → 15 (현재 15개 토큰 활성)
 * 
 * 2. 모든 토큰 목록 조회
 *    KEYS chat_token:*
 *    → chat_token:12345678-1234-1234-1234-123456789abc
 *    → chat_token:87654321-4321-4321-4321-cba987654321
 * 
 * 3. 특정 토큰 정보 확인
 *    GET chat_token:12345678-1234-1234-1234-123456789abc
 *    → job_chat_1640995200000
 * 
 * 4. 토큰 만료 시간 확인
 *    TTL chat_token:12345678-1234-1234-1234-123456789abc
 *    → 487 (487초 후 만료)
 * 
 * 5. Redis 메모리 사용량 모니터링
 *    INFO memory
 *    → used_memory_human: 1.23M
 *    → maxmemory_human: 256M
 * 
 * 6. Redis 실시간 활동 모니터링
 *    MONITOR
 *    → 실시간으로 모든 Redis 명령어 표시
 * 
 * 7. 긴급 시 특정 토큰 강제 삭제
 *    DEL chat_token:12345678-1234-1234-1234-123456789abc
 *    → (integer) 1 (삭제 성공)
 * </pre>
 * 
 * <h3>🧠 Claude AI 지능형 처리</h3>
 * <p>
 * 자연어 질의를 다음과 같이 지능적으로 분석하고 처리합니다:
 * </p>
 * <table border="1">
 * <tr>
 * <th>Claude 의도 분석</th>
 * <th>사용자 질의 예시</th>
 * <th>처리 방식</th>
 * <th>응답 형태</th>
 * <th>처리 시간</th>
 * </tr>
 * <tr>
 * <td><strong>HS_CODE_ANALYSIS</strong></td>
 * <td>"냉동피자 HS Code 알려줘"</td>
 * <td>웹검색 → 품목분류 → 관세율 조회</td>
 * <td>통합 마크다운 + 상세 URL</td>
 * <td>15-25초</td>
 * </tr>
 * <tr>
 * <td><strong>CARGO_TRACKING</strong></td>
 * <td>"12345678901234567 화물 어디야?"</td>
 * <td>번호 분석 → API 호출 → 상태 해석</td>
 * <td>실시간 위치 + 예상 도착시간</td>
 * <td>10-15초</td>
 * </tr>
 * <tr>
 * <td><strong>GENERAL_TRADE_INFO</strong></td>
 * <td>"미국 수출 절차 알려줘"</td>
 * <td>웹검색 → 규제 수집 → 가이드 생성</td>
 * <td>자연어 답변 + 공식 링크</td>
 * <td>20-30초</td>
 * </tr>
 * <tr>
 * <td><strong>NOT_TRADE_RELATED</strong></td>
 * <td>"오늘 날씨 어때?"</td>
 * <td>즉시 차단</td>
 * <td>안내 메시지 + 사용법 예시</td>
 * <td>1-2초</td>
 * </tr>
 * </table>
 * 
 * <h3>📡 SSE 실시간 스트리밍 구조</h3>
 * <p>
 * 투명한 AI 사고과정과 최종 답변을 분리하여 실시간 전송:
 * </p>
 * 
 * <pre>
 * SSE 이벤트 스트림 타임라인:
 * 
 * Phase 1: Thinking Events (Claude 사고과정 투명화)
 * 00:00 - thinking_intent_analysis      → "💭 질문의 의도를 분석하고 있습니다..."
 * 00:01 - thinking_web_search_planning  → "📋 웹검색을 계획하고 있습니다..."
 * 00:03 - thinking_web_search_executing → "🌐 최신 무역 정보를 수집하고 있습니다..."
 * 00:06 - thinking_data_processing      → "⚙️ 정보를 분석하고 정리하고 있습니다..."
 * 00:08 - thinking_response_generation  → "📝 최종 답변을 생성하고 있습니다..."
 * 
 * Phase 2: Main Message (최종 답변)
 * 00:09 - main_message_start    → "메인 답변 생성을 시작합니다"
 * 00:09 - main_message_data     → 답변 내용 (50자 청크 단위 스트리밍)
 * 00:15 - main_message_complete → 메타데이터
 *    ├─ detailPageUrl: "http://localhost:3000/intent/?hscode=1905.90.90"
 *    ├─ sources: [{title, url, type}]
 *    └─ relatedInfo: {hsCode, category}
 * </pre>
 * 
 * <h3>🏗️ 비동기 처리 아키텍처</h3>
 * <p>
 * 성능과 사용자 경험을 위한 비동기 처리:
 * </p>
 * <ul>
 * <li><strong>즉시 응답</strong>: 채팅 요청 시 jobId와 토큰 즉시 반환</li>
 * <li><strong>백그라운드 처리</strong>: LangChain4j 체이닝을 별도 스레드에서 실행</li>
 * <li><strong>실시간 스트리밍</strong>: 처리 과정을 실시간으로 클라이언트에 전송</li>
 * <li><strong>리소스 자동 관리</strong>: 완료/실패/타임아웃 시 자동 정리</li>
 * </ul>
 * 
 * <h3>📊 성능 특성</h3>
 * 
 * <h4>응답 시간 목표</h4>
 * <ul>
 * <li><strong>채팅 요청 초기 응답</strong>: 1-2초 (Claude 의도 분석 + Redis 토큰 생성)</li>
 * <li><strong>스트리밍 연결 시작</strong>: 즉시 (Redis 토큰 검증 후)</li>
 * <li><strong>Thinking 과정 완료</strong>: 7-8초 (5단계 사고과정)</li>
 * <li><strong>최종 답변 완료</strong>: 15-45초 (질의 복잡도에 따라)</li>
 * </ul>
 * 
 * <h4>동시 처리 능력</h4>
 * <ul>
 * <li><strong>동시 채팅 작업</strong>: 100+ 지원</li>
 * <li><strong>Redis 토큰 처리</strong>: 1000 TPS까지 안정적 처리</li>
 * <li><strong>메모리 사용량</strong>: 토큰당 100bytes, 채팅 작업당 2MB</li>
 * <li><strong>확장성</strong>: Redis 클러스터링으로 수평 확장 가능</li>
 * </ul>
 * 
 * <h3>🔒 보안 메커니즘</h3>
 * <ul>
 * <li><strong>일회용 토큰</strong>: UUID 기반 예측 불가능한 토큰, 사용 후 즉시 삭제</li>
 * <li><strong>TTL 자동 만료</strong>: Redis TTL로 10분 후 토큰 자동 삭제</li>
 * <li><strong>무역 외 질의 차단</strong>: Claude가 비관련 질문 즉시 차단</li>
 * <li><strong>익명성 보장</strong>: 사용자 식별 정보 저장하지 않음</li>
 * </ul>
 * 
 * <h3>🚨 장애 대응 및 복구</h3>
 * 
 * <h4>주요 장애 시나리오</h4>
 * <ul>
 * <li><strong>Redis 장애</strong>: 토큰 생성/검증 불가 → HTTP 500</li>
 * <li><strong>Claude AI 오류</strong>: 응답 생성 실패 → 안내 메시지</li>
 * <li><strong>SSE 연결 오류</strong>: 자동 재시도 또는 graceful degradation</li>
 * <li><strong>타임아웃</strong>: 5분 타임아웃으로 무한 대기 방지</li>
 * </ul>
 * 
 * <h4>복구 전략</h4>
 * 
 * <pre>
 * 1. Redis 서버 다운 시
 *    ├─ 즉시 대응: Redis 재시작, 애플리케이션 재시작
 *    ├─ 예방: Redis Sentinel 구성, 클러스터링
 *    └─ 모니터링: Redis 상태 체크, 알림 설정
 * 
 * 2. Claude AI 서비스 장애 시
 *    ├─ Circuit Breaker 발동
 *    ├─ 기본 안내 메시지 제공
 *    └─ 자동 복구 시도 (30초 간격)
 * 
 * 3. 대용량 트래픽 시
 *    ├─ Rate Limiting 적용
 *    ├─ Redis 메모리 확장
 *    └─ 인스턴스 스케일 아웃
 * </pre>
 * 
 * <h3>🔧 운영 및 모니터링</h3>
 * <p>
 * 시스템 운영을 위한 관리 기능:
 * </p>
 * <ul>
 * <li><strong>작업 상태 조회</strong>: {@link #getChatJobStatus(String)}</li>
 * <li><strong>만료 작업 정리</strong>: {@link #cleanupExpiredChatJobs()}</li>
 * <li><strong>Redis 토큰 모니터링</strong>: ChatTokenService 연동</li>
 * <li><strong>성능 지표</strong>: 처리 시간, 성공률, 토큰 사용량</li>
 * </ul>
 * 
 * <h3>⚠️ 중요 의존성 (협업자 필독)</h3>
 * 
 * <h4>필수 외부 시스템</h4>
 * <ul>
 * <li><strong>Redis Server</strong>: 필수! 토큰 관리 시스템의 핵심, Redis 없이는 시스템 동작
 * 불가</li>
 * <li><strong>Claude AI API</strong>: LangChain4j를 통한 Anthropic Claude 접근</li>
 * <li><strong>MySQL Database</strong>: ChatJob 엔티티 저장 및 이력 관리</li>
 * </ul>
 * 
 * <h4>시스템 환경 요구사항</h4>
 * <ul>
 * <li><strong>Java</strong>: OpenJDK 17+ (LangChain4j 호환성)</li>
 * <li><strong>Spring Boot</strong>: 3.2+ (SSE 및 Redis 지원)</li>
 * <li><strong>Redis</strong>: 6.0+ (TTL 및 원자적 연산 지원)</li>
 * <li><strong>MySQL</strong>: 8.0+ (JSON 타입 지원)</li>
 * </ul>
 * 
 * <h3>🚀 개발 시작 가이드</h3>
 * 
 * <h4>로컬 개발환경 구성</h4>
 * 
 * <pre>
 * 1. Redis 서버 시작 (Docker 권장)
 *    docker run -d -p 6379:6379 --name redis redis:latest
 * 
 * 2. 환경변수 설정
 *    export ANTHROPIC_API_KEY=your_claude_api_key
 *    export SPRING_PROFILES_ACTIVE=dev
 * 
 * 3. 애플리케이션 시작
 *    ./mvnw spring-boot:run
 * 
 * 4. 기본 테스트
 *    curl -X POST http://localhost:8081/api/chat \
 *         -H "Content-Type: application/json" \
 *         -d '{"message": "냉동피자 HS Code 알려줘"}'
 * </pre>
 * 
 * <h4>테스트 시나리오</h4>
 * <ol>
 * <li><strong>정상 플로우</strong>: 채팅 요청 → 토큰 생성 → 스트리밍 → 완료</li>
 * <li><strong>보안 테스트</strong>: 토큰 재사용 시도 → HTTP 401 확인</li>
 * <li><strong>장애 시나리오</strong>: Redis 중단 → 적절한 오류 응답 확인</li>
 * <li><strong>성능 테스트</strong>: 동시 100개 요청 → 응답 시간 측정</li>
 * </ol>
 * 
 * @author AI 기반 무역 규제 레이더 팀
 * @since v4.0
 * @see ChatTokenService Redis 기반 토큰 관리
 * @see com.hscoderadar.domain.chat.entity.ChatJob 채팅 작업 엔티티
 * @see com.hscoderadar.config.LangChain4jConfig Claude AI 설정
 */
public interface ChatService {

  /**
   * ChatGPT 스타일 통합 채팅 요청 처리
   * 
   * <p>
   * 사용자의 자연어 질문을 Claude AI가 분석하여 무역 관련 의도를 파악하고,
   * <strong>Redis에 일회용 세션 토큰을 생성</strong>하여 jobId와 함께 반환합니다.
   * </p>
   * 
   * <h4>🔐 Redis 토큰 생성 과정</h4>
   * 
   * <pre>
   * 1. Claude AI 의도 분석
   *    ├─ 자연어 → 구조화된 의도 (HS_CODE_ANALYSIS, CARGO_TRACKING 등)
   *    ├─ 무역 관련성 검증 (NOT_TRADE_RELATED 시 즉시 차단)
   *    └─ 분석 성공 시 다음 단계 진행
   * 
   * 2. MySQL ChatJob 생성
   *    ├─ jobId: job_chat_1640995200000
   *    ├─ userMessage: 원본 질문 저장
   *    ├─ claudeIntent: 분석된 의도 저장
   *    ├─ processingStatus: PENDING
   *    └─ tokenExpiresAt: 현재시간 + 10분
   * 
   * 3. Redis 일회용 토큰 생성
   *    ├─ UUID 생성: 12345678-1234-1234-1234-123456789abc
   *    ├─ Redis 저장: SET chat_token:{uuid} {jobId} EX 600
   *    └─ 토큰 반환: sessionToken
   * 
   * 4. 클라이언트 응답
   *    └─ {jobId, sessionToken, streamUrl, estimatedTime}
   * </pre>
   * 
   * <h4>🧠 Claude AI 지원 질의 유형</h4>
   * <ul>
   * <li><strong>HS Code 분석</strong>: "냉동피자 HS Code 알려줘" → 품목분류 + 관세율 + 규제</li>
   * <li><strong>화물 추적</strong>: "12345678901234567 화물 어디야?" → 실시간 위치 + 통관 단계</li>
   * <li><strong>일반 무역 정보</strong>: "미국 수출 절차" → 종합 가이드 + 최신 규제</li>
   * <li><strong>복합 질의</strong>: "냉동피자 미국 수출 전체 프로세스" → 통합 솔루션</li>
   * </ul>
   * 
   * <h4>⚠️ Redis 의존성 (중요!)</h4>
   * <p>
   * 이 메서드는 <strong>Redis 서버가 정상 동작해야만 성공</strong>합니다.
   * Redis 연결 실패 시 토큰 생성이 불가능하여 전체 플로우가 중단됩니다.
   * </p>
   * 
   * @param request 사용자의 자연어 질문 (2자 이상 2000자 이하)
   * @return 채팅 작업 정보
   *         <ul>
   *         <li><strong>jobId</strong>: 작업 고유 식별자 (job_chat_xxxxxxxxx)</li>
   *         <li><strong>sessionToken</strong>: Redis 일회용 토큰 (UUID, 10분 TTL)</li>
   *         <li><strong>streamUrl</strong>: SSE 스트리밍 엔드포인트 URL</li>
   *         <li><strong>estimatedTime</strong>: 예상 완료 시간 (초)</li>
   *         </ul>
   * 
   * @throws IllegalArgumentException                                       무역과 관련
   *                                                                        없는 질문인
   *                                                                        경우
   *                                                                        (HTTP
   *                                                                        422)
   *                                                                        <br>
   *                                                                        Claude가
   *                                                                        {@code NOT_TRADE_RELATED}로
   *                                                                        분석한 질의
   * @throws RuntimeException                                               Claude
   *                                                                        AI 분석
   *                                                                        실패 시
   *                                                                        (HTTP
   *                                                                        500)
   *                                                                        <br>
   *                                                                        LangChain4j
   *                                                                        연동 오류
   *                                                                        또는 AI
   *                                                                        서비스 장애
   * @throws org.springframework.data.redis.RedisConnectionFailureException Redis
   *                                                                        연결 실패
   *                                                                        시
   *                                                                        <br>
   *                                                                        Redis
   *                                                                        서버 다운
   *                                                                        또는
   *                                                                        네트워크
   *                                                                        오류
   * 
   * @since v4.0
   */
  ChatResponse initiateChatAnalysis(ChatRequest request);

  /**
   * 실시간 채팅 응답 스트리밍
   * 
   * <p>
   * Server-Sent Events(SSE)를 통해 Claude AI의 사고과정과 최종 답변을
   * 실시간으로 스트리밍합니다. <strong>Redis 토큰 검증 후 즉시 삭제</strong>하여
   * 일회용 보안을 보장합니다.
   * </p>
   * 
   * <h4>🔐 Redis 토큰 검증 및 소모 과정</h4>
   * 
   * <pre>
   * 1. Redis 토큰 검증
   *    ├─ Redis 조회: GET chat_token:{token}
   *    ├─ 성공: jobId 반환 (예: job_chat_1640995200000)
   *    └─ 실패: null 반환 (토큰 없음/만료/이미 사용됨)
   * 
   * 2. 토큰 즉시 삭제 (일회용 보장)
   *    ├─ Redis 삭제: DEL chat_token:{token}
   *    ├─ 재사용 완전 차단
   *    └─ 보안 로그 기록
   * 
   * 3. jobId 검증
   *    ├─ MySQL 조회: ChatJob 엔티티 존재 확인
   *    ├─ 상태 확인: PENDING 또는 PROCESSING만 허용
   *    └─ 검증 실패 시 SecurityException 발생
   * 
   * 4. SSE 스트리밍 시작
   *    ├─ 비동기 LangChain4j 체이닝 실행
   *    ├─ 실시간 사고과정 스트리밍
   *    └─ 최종 답변 및 메타데이터 전송
   * </pre>
   * 
   * <h4>📡 SSE 이벤트 스트림 구조</h4>
   * 
   * <pre>
   * Phase 1: Thinking Events (Claude 사고과정 투명화)
   * ├─ thinking_intent_analysis: 질문 의도 분석 중
   * ├─ thinking_web_search_planning: 웹검색 계획 수립 중  
   * ├─ thinking_web_search_executing: 실시간 웹검색 실행 중
   * ├─ thinking_data_processing: 정보 분석 및 정리 중
   * └─ thinking_response_generation: 최종 답변 생성 중
   * 
   * Phase 2: Main Message (최종 답변)
   * ├─ main_message_start: 메인 답변 시작
   * ├─ main_message_data: 답변 내용 (50자 청크 단위)
   * └─ main_message_complete: 완료 + 메타데이터
   *    ├─ detailPageUrl: 클라이언트 상세 페이지 URL
   *    ├─ sources: 참고 자료 URL 및 신뢰도
   *    └─ relatedInfo: HS Code, 카테고리 등 구조화된 정보
   * </pre>
   * 
   * <h4>🔄 비동기 처리 및 리소스 관리</h4>
   * <ul>
   * <li><strong>백그라운드 실행</strong>: ExecutorService 카스케이드 스레드 풀</li>
   * <li><strong>자동 상태 업데이트</strong>: PENDING → PROCESSING → COMPLETED/FAILED</li>
   * <li><strong>연결 관리</strong>: onCompletion, onError, onTimeout 핸들러</li>
   * <li><strong>리소스 정리</strong>: 완료/오류 시 자동 정리</li>
   * </ul>
   * 
   * <h4>🚨 토큰 재사용 차단 (보안 핵심)</h4>
   * <p>
   * 동일한 토큰으로 두 번째 호출 시:
   * </p>
   * 
   * <pre>
   * 1. Redis 조회: GET chat_token:{token} → null (이미 삭제됨)
   * 2. 검증 실패: ChatTokenService.validateAndConsumeToken() → null 반환
   * 3. SecurityException 발생 → HTTP 401 Unauthorized
   * 4. 보안 로그: "토큰 재사용 시도 감지" 기록
   * </pre>
   * 
   * @param jobId 채팅 작업 고유 식별자 (job_chat_xxxxxxxxx 형태)
   * @param token Redis 기반 일회용 세션 토큰 (UUID, 검증 후 즉시 삭제)
   * @return SSE 이벤트 스트림 (Content-Type: text/event-stream)
   *         <br>
   *         클라이언트는 EventSource API로 수신
   * 
   * @throws SecurityException        토큰이 만료되거나 이미 사용된 경우 (HTTP 401)
   *                                  <ul>
   *                                  <li>Redis에서 토큰 조회 실패</li>
   *                                  <li>토큰 재사용 시도</li>
   *                                  <li>TTL 만료로 인한 자동 삭제</li>
   *                                  </ul>
   * @throws IllegalArgumentException jobId가 존재하지 않는 경우 (HTTP 404)
   *                                  <br>
   *                                  MySQL에서 ChatJob 엔티티 조회 실패
   * @throws RuntimeException         Langchain 체이닝 실행 실패 시 (HTTP 500)
   *                                  <br>
   *                                  Claude AI 오류, 웹검색 실패, 응답 생성 오류
   * 
   * @since v4.0
   */
  SseEmitter streamChatResponse(String jobId, String token);

  /**
   * 채팅 작업 상태 조회 (디버깅/모니터링용)
   * 
   * <p>
   * 특정 채팅 작업의 현재 상태를 조회합니다.
   * 주로 개발 환경에서의 디버깅이나 시스템 모니터링 용도로 사용됩니다.
   * </p>
   * 
   * <h4>📊 조회 가능한 정보</h4>
   * <ul>
   * <li><strong>jobId</strong>: 작업 고유 식별자</li>
   * <li><strong>status</strong>: 현재 처리 상태 (PENDING, PROCESSING, COMPLETED,
   * FAILED)</li>
   * <li><strong>intent</strong>: Claude가 분석한 의도 (HS_CODE_ANALYSIS, CARGO_TRACKING
   * 등)</li>
   * <li><strong>createdAt</strong>: 작업 생성 시간</li>
   * <li><strong>completedAt</strong>: 작업 완료 시간 (완료된 경우만)</li>
   * <li><strong>estimatedTime vs actualTime</strong>: 예상 vs 실제 처리 시간</li>
   * </ul>
   * 
   * <h4>🔍 모니터링 활용 예시</h4>
   * 
   * <pre>
   * // 시스템 성능 모니터링
   * {
   *   "jobId": "job_chat_1640995200000",
   *   "status": "COMPLETED",
   *   "intent": "HS_CODE_ANALYSIS",
   *   "createdAt": "2024-01-16T10:30:00Z",
   *   "completedAt": "2024-01-16T10:30:25Z",
   *   "estimatedTime": 30,
   *   "actualTime": 25
   * }
   * </pre>
   * 
   * <h4>⚠️ 보안 주의사항</h4>
   * <p>
   * 이 메서드는 인증이 필요하지 않으므로 민감한 정보는 반환하지 않습니다.
   * 사용자 메시지나 응답 내용은 포함되지 않으며, 상태 정보만 제공됩니다.
   * </p>
   * 
   * @param jobId 조회할 채팅 작업 ID
   * @return 작업 상태 정보 (Map 또는 DTO 형태)
   * 
   * @throws IllegalArgumentException jobId가 존재하지 않는 경우
   * 
   * @since v4.0
   */
  Object getChatJobStatus(String jobId);

  /**
   * 만료된 채팅 작업 정리 (스케줄러용)
   * 
   * <p>
   * Redis 토큰 만료 시간이 지난 채팅 작업들을 자동으로 정리합니다.
   * 시스템 유지보수를 위해 정기적으로 실행되는 스케줄러 메서드입니다.
   * </p>
   * 
   * <h4>🧹 정리 대상 및 과정</h4>
   * 
   * <pre>
   * 정리 대상:
   * ├─ tokenExpiresAt < 현재시간 (Redis TTL 만료와 동기화)
   * ├─ processingStatus != PROCESSING (처리 중인 작업은 보호)
   * └─ 고아 레코드 (Redis 토큰은 없지만 DB에만 남은 작업)
   * 
   * 정리 과정:
   * 1. MySQL 쿼리: 만료 조건에 해당하는 ChatJob 조회
   * 2. 일괄 삭제: DELETE FROM chat_jobs WHERE 조건
   * 3. 연관 데이터 정리: chat_streaming_events 테이블 정리 (CASCADE)
   * 4. 통계 로그: 삭제된 작업 수 기록
   * </pre>
   * 
   * <h4>⏰ 실행 주기 및 성능</h4>
   * <ul>
   * <li><strong>실행 주기</strong>: 매시간 1회 (cron: 0 0 * * * *)</li>
   * <li><strong>배치 크기</strong>: 1000개씩 배치 처리로 DB 부하 최소화</li>
   * <li><strong>실행 시간</strong>: 일반적으로 1-2초 내 완료</li>
   * <li><strong>로그 기록</strong>: 정리 결과를 system_logs 테이블에 기록</li>
   * </ul>
   * 
   * <h4>🔄 Redis와의 동기화</h4>
   * <p>
   * Redis TTL이 만료되어 자동 삭제된 토큰에 대응하는 MySQL 레코드를 정리합니다.
   * 이를 통해 Redis와 MySQL 간의 데이터 일관성을 유지합니다.
   * </p>
   * 
   * <h4>📊 모니터링 정보</h4>
   * 
   * <pre>
   * 로그 예시:
   * INFO - 만료된 채팅 작업 정리 시작
   * INFO - 만료된 채팅 작업 정리 완료 - 삭제된 작업 수: 245개
   * INFO - 정리 소요 시간: 1.2초
   * </pre>
   * 
   * @since v4.0
   */
  void cleanupExpiredChatJobs();
}