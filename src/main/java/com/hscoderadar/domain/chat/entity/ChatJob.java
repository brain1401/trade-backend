package com.hscoderadar.domain.chat.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * v4.0 채팅 작업 관리 엔티티
 * 
 * <h3>🎯 목적</h3>
 * <p>
 * ChatGPT 스타일 통합 채팅 시스템의 작업 상태와 결과를 관리하는 핵심 엔티티입니다.
 * 각 사용자의 채팅 요청은 하나의 ChatJob으로 생성되어 전체 생명주기를 추적합니다.
 * </p>
 * 
 * <h3>🔄 생명주기 및 상태 관리</h3>
 * 
 * <pre>
 * 채팅 작업 생명주기:
 * 
 * 1. PENDING (대기)
 *    ├─ POST /api/chat 요청 시 생성
 *    ├─ Claude AI 의도 분석 완료
 *    ├─ Redis 토큰 연동: sessionToken ↔ jobId
 *    └─ 사용자에게 토큰 반환
 * 
 * 2. PROCESSING (처리 중)
 *    ├─ SSE 스트리밍 시작 시 변경
 *    ├─ LangChain4j 체이닝 실행
 *    ├─ Claude 웹검색 및 분석 진행
 *    └─ 실시간 응답 생성
 * 
 * 3. COMPLETED (완료)
 *    ├─ 최종 응답 전송 완료
 *    ├─ SSE 연결 정상 종료
 *    ├─ 관련 Redis 토큰 자동 정리
 *    └─ completedAt 시간 기록
 * 
 * 4. FAILED (실패)
 *    ├─ Claude AI 오류 또는 시스템 오류
 *    ├─ SSE 연결 오류 또는 타임아웃
 *    ├─ Redis 토큰 검증 실패
 *    └─ 에러 메시지 기록
 * </pre>
 * 
 * <h3>🔗 Redis 연동 구조 (중요!)</h3>
 * <p>
 * 이 엔티티는 Redis 토큰 시스템과 밀접하게 연동됩니다:
 * </p>
 * 
 * <h4>Redis ↔ MySQL 연동 구조</h4>
 * 
 * <pre>
 * 1. 채팅 요청 시
 *    ├─ MySQL: ChatJob 엔티티 생성
 *    │  ├─ jobId: job_chat_1640995200000
 *    │  ├─ sessionToken: "" (빈 값, 보안상 저장하지 않음)
 *    │  ├─ tokenExpiresAt: 현재시간 + 10분
 *    │  └─ processingStatus: PENDING
 *    └─ Redis: 실제 토큰 저장
 *       ├─ 키: chat_token:uuid-1234-5678
 *       ├─ 값: job_chat_1640995200000 (jobId)
 *       └─ TTL: 600초 (10분)
 * 
 * 2. 스트리밍 요청 시
 *    ├─ Redis: 토큰 검증 후 즉시 삭제
 *    │  ├─ GET chat_token:uuid-1234-5678 → jobId 반환
 *    │  └─ DEL chat_token:uuid-1234-5678 → 토큰 삭제
 *    └─ MySQL: ChatJob 상태 업데이트
 *       ├─ processingStatus: PROCESSING
 *       └─ tokenUsedAt: 현재시간
 * 
 * 3. 정리 작업 시
 *    ├─ Redis: TTL 만료로 자동 삭제
 *    └─ MySQL: 스케줄러가 만료된 ChatJob 일괄 삭제
 *       └─ WHERE tokenExpiresAt < NOW() AND status != 'PROCESSING'
 * </pre>
 * 
 * <h4>보안 설계 원칙</h4>
 * <ul>
 * <li><strong>jobId ↔ Redis 토큰</strong>: {@code chat_token:{uuid} → jobId}
 * 매핑</li>
 * <li><strong>tokenExpiresAt</strong>: Redis TTL과 동기화 (10분)</li>
 * <li><strong>tokenUsedAt</strong>: 토큰 소모 시점 기록 (Redis에서 삭제됨)</li>
 * <li><strong>sessionToken 필드</strong>: DB에는 빈 값, 실제 토큰은 Redis에서 관리</li>
 * </ul>
 * 
 * <h3>📊 데이터베이스 스키마 매핑</h3>
 * 
 * <pre>
 * MySQL 테이블: chat_jobs
 * ┌─────────────────────┬─────────────────┬──────────────────────────────┐
 * │ 컬럼명               │ 타입            │ 설명                         │
 * ├─────────────────────┼─────────────────┼──────────────────────────────┤
 * │ id                  │ BIGINT AI PK    │ 내부 시퀀스 ID               │
 * │ job_id              │ VARCHAR(50) UK  │ 외부 공개 식별자             │
 * │ session_token       │ VARCHAR(50) UK  │ Redis 연동용 (DB엔 빈 값)    │
 * │ user_message        │ TEXT            │ 사용자 원본 질문             │
 * │ claude_intent       │ VARCHAR(50)     │ Claude 분석 의도             │
 * │ processing_status   │ ENUM            │ 작업 처리 상태               │
 * │ thinking_events     │ JSON            │ 사고과정 이벤트 목록         │
 * │ main_response       │ TEXT            │ 최종 응답 내용               │
 * │ detail_page_url     │ VARCHAR(500)    │ 상세 페이지 URL              │
 * │ sources             │ JSON            │ 참고 자료 소스 목록          │
 * │ related_info        │ JSON            │ 관련 정보 메타데이터         │
 * │ token_expires_at    │ TIMESTAMP       │ Redis TTL과 동기화           │
 * │ token_used_at       │ TIMESTAMP       │ 토큰 소모 시점               │
 * │ created_at          │ TIMESTAMP       │ 작업 생성 시간               │
 * │ completed_at        │ TIMESTAMP       │ 작업 완료 시간               │
 * └─────────────────────┴─────────────────┴──────────────────────────────┘
 * </pre>
 * 
 * <h3>🔍 주요 필드 상세 설명</h3>
 * 
 * <h4>식별자 필드</h4>
 * <ul>
 * <li><strong>id</strong>: MySQL 내부 Auto Increment PK (외부 노출 안함)</li>
 * <li><strong>jobId</strong>: {@code job_chat_1234567890} 형태의 고유 식별자 (타임스탬프
 * 기반)</li>
 * <li><strong>sessionToken</strong>: 빈 값 저장 (실제 토큰은 Redis에서만 관리)</li>
 * </ul>
 * 
 * <h4>업무 로직 필드</h4>
 * <ul>
 * <li><strong>userMessage</strong>: 사용자 원본 질문 (XSS 방지 처리됨)</li>
 * <li><strong>claudeIntent</strong>: HS_CODE_ANALYSIS, CARGO_TRACKING,
 * GENERAL_TRADE_INFO, NOT_TRADE_RELATED</li>
 * <li><strong>processingStatus</strong>: PENDING, PROCESSING, COMPLETED,
 * FAILED</li>
 * </ul>
 * 
 * <h4>응답 데이터 필드</h4>
 * <ul>
 * <li><strong>thinkingEvents</strong>: SSE thinking_* 이벤트들의 JSON 배열</li>
 * <li><strong>mainResponse</strong>: Claude가 생성한 최종 마크다운 응답</li>
 * <li><strong>sources</strong>: 웹검색으로 수집한 참고 자료 URL 및 메타데이터</li>
 * <li><strong>relatedInfo</strong>: HS Code, 카테고리 등 구조화된 정보</li>
 * </ul>
 * 
 * <h4>시간 관리 필드</h4>
 * <ul>
 * <li><strong>tokenExpiresAt</strong>: Redis TTL과 정확히 동기화 (10분)</li>
 * <li><strong>tokenUsedAt</strong>: 토큰 소모 시점 (재사용 방지 추적)</li>
 * <li><strong>estimatedTimeSeconds vs actualTimeSeconds</strong>: 성능 분석용</li>
 * </ul>
 * 
 * <h3>📈 성능 최적화</h3>
 * 
 * <h4>데이터베이스 인덱스 전략</h4>
 * <ul>
 * <li><strong>인덱스 활용</strong>: jobId, processingStatus, tokenExpiresAt에
 * 인덱스</li>
 * <li><strong>복합 인덱스</strong>: (processingStatus, tokenExpiresAt) 정리 쿼리
 * 최적화</li>
 * <li><strong>생성시간 인덱스</strong>: created_at 기준 효율적 페이징 쿼리</li>
 * </ul>
 * 
 * <h4>JSON 필드 최적화</h4>
 * <ul>
 * <li><strong>MySQL JSON 타입</strong>: MySQL 8.0+ JSON 타입으로 효율적 저장</li>
 * <li><strong>JSON 경로 인덱스</strong>: 필요시 JSON 내부 필드에 인덱스 생성 가능</li>
 * <li><strong>압축</strong>: 대용량 JSON 데이터의 자동 압축</li>
 * </ul>
 * 
 * <h4>TTL 기반 정리</h4>
 * <ul>
 * <li><strong>자동 정리 스케줄러</strong>: tokenExpiresAt 기준 만료 작업 일괄 삭제</li>
 * <li><strong>배치 처리</strong>: 1000개씩 배치로 DB 부하 최소화</li>
 * <li><strong>페이징 지원</strong>: created_at 기준 효율적 페이징 쿼리</li>
 * </ul>
 * 
 * <h3>🔒 보안 고려사항</h3>
 * 
 * <h4>토큰 보안</h4>
 * <ul>
 * <li><strong>sessionToken 필드</strong>: 보안상 DB에는 저장하지 않음 (Redis만 사용)</li>
 * <li><strong>토큰 매핑</strong>: jobId ↔ Redis 토큰 간접 매핑으로 예측 방지</li>
 * <li><strong>토큰 수명</strong>: Redis TTL과 DB tokenExpiresAt 이중 관리</li>
 * </ul>
 * 
 * <h4>개인정보 보호</h4>
 * <ul>
 * <li><strong>사용자 입력</strong>: userMessage는 XSS 방지를 위해 이스케이프 처리</li>
 * <li><strong>개인정보</strong>: 사용자 식별 정보는 저장하지 않음 (익명성 보장)</li>
 * <li><strong>로그 보안</strong>: 민감한 정보는 errorMessage에 포함하지 않음</li>
 * </ul>
 * 
 * <h4>데이터 무결성</h4>
 * <ul>
 * <li><strong>외래키 없음</strong>: 사용자 정보와 분리하여 개인정보 보호</li>
 * <li><strong>상태 검증</strong>: 비즈니스 로직에서 상태 전이 규칙 검증</li>
 * <li><strong>트랜잭션</strong>: 상태 변경 시 원자적 처리</li>
 * </ul>
 * 
 * <h3>🔄 연관 관계</h3>
 * 
 * <h4>외부 시스템 연동</h4>
 * <ul>
 * <li><strong>Redis (chat_token:*)</strong>: sessionToken 기반 토큰 연동</li>
 * <li><strong>chat_streaming_events</strong>: jobId 기반 상세 스트리밍 로그 (선택사항)</li>
 * <li><strong>system_logs</strong>: 시스템 로그에 jobId 참조 기록</li>
 * </ul>
 * 
 * <h4>의존성 다이어그램</h4>
 * 
 * <pre>
 * ChatJob Entity ↔ Systems
 * 
 * ┌─────────────────┐    Redis 토큰 매핑    ┌─────────────────┐
 * │   ChatJob       │←─────────────────────→│ Redis           │
 * │                 │   chat_token:{uuid}   │ chat_token:*    │
 * │ - jobId         │      ↕ jobId          │ TTL: 600초      │
 * │ - sessionToken  │                       │                 │
 * │ - tokenExpires  │                       │                 │
 * └─────────────────┘                       └─────────────────┘
 *         │                                          │
 *         │ 상태 관리                                │ 보안 검증
 *         ▼                                          ▼
 * ┌─────────────────┐                       ┌─────────────────┐
 * │ ChatService     │                       │ ChatTokenService│
 * │                 │                       │                 │
 * │ - 상태 업데이트  │                       │ - 토큰 생성     │
 * │ - SSE 스트리밍  │                       │ - 토큰 검증     │
 * │ - 응답 저장     │                       │ - 토큰 삭제     │
 * └─────────────────┘                       └─────────────────┘
 * </pre>
 * 
 * <h3>🔧 개발자 가이드</h3>
 * 
 * <h4>엔티티 생성 패턴</h4>
 * 
 * <pre>
 * // 올바른 ChatJob 생성 방법
 * ChatJob chatJob = ChatJob.builder()
 *     .jobId("job_chat_" + System.currentTimeMillis())
 *     .sessionToken("") // 빈 값! Redis에서 별도 관리
 *     .userMessage(request.getMessage())
 *     .claudeIntent(analyzedIntent)
 *     .processingStatus(ProcessingStatus.PENDING)
 *     .estimatedTimeSeconds(300)
 *     .tokenExpiresAt(LocalDateTime.now().plusMinutes(10))
 *     .build();
 * 
 * // Repository 저장
 * chatJobRepository.save(chatJob);
 * 
 * // Redis 토큰 생성 (별도)
 * String sessionToken = chatTokenService.generateSessionToken(chatJob.getJobId());
 * </pre>
 * 
 * <h4>상태 전이 규칙</h4>
 * 
 * <pre>
 * 허용되는 상태 전이:
 * PENDING → PROCESSING  ✅ (스트리밍 시작)
 * PROCESSING → COMPLETED ✅ (정상 완료)
 * PROCESSING → FAILED    ✅ (오류 발생)
 * PENDING → FAILED       ✅ (초기 실패)
 * 
 * 금지되는 상태 전이:
 * COMPLETED → *          ❌ (완료 후 변경 불가)
 * FAILED → *            ❌ (실패 후 변경 불가)
 * PROCESSING → PENDING   ❌ (역행 불가)
 * </pre>
 * 
 * <h4>정리 작업 쿼리</h4>
 * 
 * <pre>
 * // 스케줄러에서 사용하는 정리 쿼리
 * &#64;Query("DELETE FROM ChatJob c WHERE c.tokenExpiresAt < :now AND c.processingStatus != 'PROCESSING'")
 * int deleteExpiredJobs(@Param("now") LocalDateTime now);
 * 
 * // 성능 모니터링 쿼리
 * &#64;Query("SELECT COUNT(c) FROM ChatJob c WHERE c.processingStatus = :status")
 * long countByProcessingStatus(@Param("status") ProcessingStatus status);
 * </pre>
 * 
 * <h3>🚨 문제 해결 가이드</h3>
 * 
 * <h4>일반적인 문제 상황</h4>
 * 
 * <pre>
 * 문제 1: Redis 토큰은 있지만 ChatJob이 없음
 * 원인: MySQL 저장 실패 또는 동시성 문제
 * 해결: Redis 토큰 강제 삭제 후 재시도
 * 명령어: DEL chat_token:{uuid}
 * 
 * 문제 2: ChatJob은 있지만 Redis 토큰이 없음
 * 원인: Redis TTL 만료 또는 토큰 생성 실패
 * 해결: ChatJob 상태를 FAILED로 변경
 * 
 * 문제 3: PROCESSING 상태에서 무한 대기
 * 원인: SSE 연결 오류 또는 Claude AI 장애
 * 해결: 타임아웃 처리 후 FAILED 상태로 변경
 * 
 * 문제 4: JSON 직렬화 오류
 * 원인: JSON 필드에 잘못된 데이터 삽입
 * 해결: ObjectMapper validation 강화
 * </pre>
 * 
 * <h4>모니터링 쿼리</h4>
 * 
 * <pre>
 * -- 현재 작업 상태 분포
 * SELECT processing_status, COUNT(*) as count 
 * FROM chat_jobs 
 * WHERE created_at > DATE_SUB(NOW(), INTERVAL 1 HOUR)
 * GROUP BY processing_status;
 * 
 * -- 평균 처리 시간 분석
 * SELECT 
 *   claude_intent,
 *   AVG(actual_time_seconds) as avg_time,
 *   COUNT(*) as total_jobs
 * FROM chat_jobs 
 * WHERE processing_status = 'COMPLETED'
 *   AND actual_time_seconds IS NOT NULL
 * GROUP BY claude_intent;
 * 
 * -- 만료 예정 작업 확인
 * SELECT COUNT(*) as expiring_soon
 * FROM chat_jobs 
 * WHERE token_expires_at BETWEEN NOW() AND DATE_ADD(NOW(), INTERVAL 5 MINUTE)
 *   AND processing_status = 'PENDING';
 * </pre>
 * 
 * @author AI 기반 무역 규제 레이더 팀
 * @since v4.0
 * @see com.hscoderadar.domain.chat.service.ChatTokenService Redis 토큰 연동
 * @see com.hscoderadar.domain.chat.repository.ChatJobRepository 데이터 접근 계층
 */
@Entity
@Table(name = "chat_jobs")
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatJob {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "job_id", length = 50, unique = true, nullable = false)
  private String jobId;

  @Column(name = "session_token", length = 50, unique = true, nullable = false)
  private String sessionToken;

  @Column(name = "user_message", columnDefinition = "TEXT", nullable = false)
  private String userMessage;

  @Column(name = "claude_intent", length = 50)
  private String claudeIntent;

  @Enumerated(EnumType.STRING)
  @Column(name = "processing_status", nullable = false)
  @Builder.Default
  private ProcessingStatus processingStatus = ProcessingStatus.PENDING;

  @Column(name = "thinking_events", columnDefinition = "JSON")
  private String thinkingEvents;

  @Column(name = "main_response", columnDefinition = "TEXT")
  private String mainResponse;

  @Column(name = "detail_page_url", length = 500)
  private String detailPageUrl;

  @Column(name = "sources", columnDefinition = "JSON")
  private String sources;

  @Column(name = "related_info", columnDefinition = "JSON")
  private String relatedInfo;

  @Column(name = "estimated_time_seconds")
  private Integer estimatedTimeSeconds;

  @Column(name = "actual_time_seconds")
  private Integer actualTimeSeconds;

  @Column(name = "error_message", columnDefinition = "TEXT")
  private String errorMessage;

  @Column(name = "token_expires_at", nullable = false)
  private LocalDateTime tokenExpiresAt;

  @Column(name = "token_used_at")
  private LocalDateTime tokenUsedAt;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "completed_at")
  private LocalDateTime completedAt;

  /**
   * 채팅 작업 처리 상태
   */
  public enum ProcessingStatus {
    PENDING, // 대기 중
    PROCESSING, // 처리 중
    COMPLETED, // 완료
    FAILED // 실패
  }
}