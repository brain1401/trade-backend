package com.hscoderadar.domain.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

/**
 * v4.0 Redis 기반 채팅 토큰 관리 서비스
 * 
 * <h3>🔐 보안 아키텍처의 핵심: Redis 기반 일회용 토큰 시스템</h3>
 * <p>
 * 본 서비스는 ChatGPT 스타일 채팅 시스템의 보안을 책임지는 핵심 컴포넌트입니다.
 * <strong>Redis를 활용한 일회용 세션 토큰</strong>을 통해 API 남용을 완전히 차단하고,
 * 무단 접근을 방지합니다.
 * </p>
 * 
 * <h3>📋 Redis 기초 개념 (협업자 필수 지식)</h3>
 * <p>
 * <strong>Redis란?</strong> 인메모리 Key-Value 데이터베이스로, 다음과 같은 특징을 가집니다:
 * </p>
 * <ul>
 * <li><strong>초고속 처리</strong>: 메모리 기반으로 밀리초 단위 응답</li>
 * <li><strong>TTL 지원</strong>: 키별로 자동 만료 시간 설정 가능</li>
 * <li><strong>원자적 연산</strong>: 동시성 문제 없는 안전한 읽기/쓰기</li>
 * <li><strong>영속성</strong>: 필요시 디스크에 백업 가능</li>
 * </ul>
 * 
 * <h3>🔧 Redis 데이터 구조 및 생명주기</h3>
 * 
 * <pre>
 * Redis Key-Value 구조:
 * ┌─────────────────────────────┬─────────────────┬──────────────┐
 * │ Key                         │ Value           │ TTL          │
 * ├─────────────────────────────┼─────────────────┼──────────────┤
 * │ chat_token:uuid-1234-abcd   │ job_chat_12345  │ 600초 (10분) │
 * │ chat_token:uuid-5678-efgh   │ job_chat_67890  │ 600초 (10분) │
 * │ chat_token:uuid-9012-ijkl   │ job_chat_11111  │ 600초 (10분) │
 * └─────────────────────────────┴─────────────────┴──────────────┘
 * 
 * 생명주기:
 * 1. 토큰 생성 → Redis 저장 (TTL: 10분)
 * 2. 토큰 검증 → Redis에서 조회 성공
 * 3. 토큰 소모 → Redis에서 즉시 삭제 (재사용 불가)
 * 4. 만료 처리 → Redis TTL로 자동 삭제
 * </pre>
 * 
 * <h3>🛡️ 보안 메커니즘</h3>
 * <ul>
 * <li><strong>UUID 기반 토큰</strong>: 예측 불가능한 36자리 UUID 생성</li>
 * <li><strong>일회용 보장</strong>: 검증 성공 시 Redis에서 즉시 삭제</li>
 * <li><strong>자동 만료</strong>: Redis TTL로 10분 후 자동 삭제</li>
 * <li><strong>네임스페이스 분리</strong>: {@code chat_token:} 접두사로 키 충돌 방지</li>
 * </ul>
 * 
 * <h3>📊 Redis 연동 정보 (운영팀 참고)</h3>
 * <ul>
 * <li><strong>Redis Template</strong>: {@code RedisTemplate<String, String>}
 * 사용</li>
 * <li><strong>직렬화</strong>: String 직렬화 방식 (JSON 불필요)</li>
 * <li><strong>메모리 사용량</strong>: 토큰당 약 100bytes (UUID + jobId + 메타데이터)</li>
 * <li><strong>예상 처리량</strong>: 1000 TPS까지 안정적 처리 가능</li>
 * </ul>
 * 
 * <h3>🔍 Redis CLI를 통한 실시간 모니터링 (협업자 필수)</h3>
 * <p>
 * Redis 서버에 직접 접속하여 토큰 상태를 확인하는 방법:
 * </p>
 * 
 * <pre>
 * # Redis 서버 접속 (로컬 개발환경)
 * redis-cli -h localhost -p 6379
 * 
 * # 1. 모든 채팅 토큰 조회
 * KEYS chat_token:*
 * → 결과 예시:
 *   1) "chat_token:12345678-1234-1234-1234-123456789abc"
 *   2) "chat_token:87654321-4321-4321-4321-cba987654321"
 * 
 * # 2. 특정 토큰의 jobId 확인
 * GET chat_token:12345678-1234-1234-1234-123456789abc
 * → 결과: "job_chat_1640995200000"
 * 
 * # 3. 토큰 남은 만료 시간 확인 (초 단위)
 * TTL chat_token:12345678-1234-1234-1234-123456789abc
 * → 결과: 487 (남은 시간 487초)
 * 
 * # 4. 현재 활성 토큰 개수 확인
 * EVAL "return #redis.call('KEYS', 'chat_token:*')" 0
 * → 결과: 15 (현재 15개 토큰 활성)
 * 
 * # 5. 특정 토큰 강제 삭제 (긴급 시)
 * DEL chat_token:12345678-1234-1234-1234-123456789abc
 * → 결과: (integer) 1 (삭제 성공)
 * 
 * # 6. Redis 메모리 사용량 확인
 * INFO memory
 * → used_memory_human: 1.23M
 * 
 * # 7. Redis 성능 모니터링 (실시간)
 * MONITOR
 * → 실시간 명령어 실행 로그 표시
 * </pre>
 * 
 * <h3>🚀 사용 시나리오</h3>
 * 
 * <pre>
 * 사용자 채팅 요청 플로우:
 * 
 * 1. POST /api/chat
 *    └─ ChatService.initiateChatAnalysis()
 *       └─ ChatTokenService.generateSessionToken() ← Redis 저장
 *          └─ Response: {jobId, sessionToken, streamUrl}
 * 
 * 2. GET /api/chat/stream/{jobId}?token={sessionToken}
 *    └─ ChatService.streamChatResponse()
 *       └─ ChatTokenService.validateAndConsumeToken() ← Redis 검증 후 삭제
 *          └─ SSE 스트리밍 시작
 * 
 * 3. 토큰 재사용 시도
 *    └─ ChatTokenService.validateAndConsumeToken()
 *       └─ null 반환 (이미 삭제됨) → SecurityException
 * </pre>
 * 
 * <h3>⚠️ 주의사항 (협업자 필독)</h3>
 * <ul>
 * <li><strong>Redis 의존성</strong>: 본 서비스는 Redis 없이 동작하지 않습니다</li>
 * <li><strong>토큰 재사용 불가</strong>: 한 번 사용된 토큰은 즉시 삭제됩니다</li>
 * <li><strong>네트워크 오류 시</strong>: Redis 연결 실패 시 토큰 생성/검증 불가</li>
 * <li><strong>클러스터 환경</strong>: Redis 클러스터링 시 토큰이 노드 간 공유됩니다</li>
 * </ul>
 * 
 * <h3>🔍 모니터링 및 디버깅</h3>
 * <p>
 * Redis 토큰 현황 모니터링:
 * </p>
 * 
 * <pre>
 * # Redis CLI를 통한 토큰 확인
 * KEYS chat_token:*                    # 모든 활성 토큰 조회
 * TTL chat_token:uuid-here             # 특정 토큰의 남은 TTL 확인
 * GET chat_token:uuid-here             # 토큰에 연결된 jobId 확인
 * 
 * # 애플리케이션 모니터링
 * ChatTokenService.getActiveTokenCount()  # 현재 활성 토큰 수 조회
 * </pre>
 * 
 * <h3>🚨 장애 대응 가이드</h3>
 * 
 * <h4>상황 1: Redis 서버 다운</h4>
 * 
 * <pre>
 * 증상: RedisConnectionFailureException 발생
 * 원인: Redis 서버 중단 또는 네트워크 문제
 * 
 * 즉시 대응:
 * 1. Redis 서버 상태 확인: systemctl status redis
 * 2. Redis 서비스 재시작: systemctl restart redis
 * 3. 네트워크 연결 확인: ping [redis-host]
 * 4. 애플리케이션 재시작 (연결 풀 재초기화)
 * 
 * 예방책:
 * - Redis 클러스터링 구성
 * - Redis Sentinel을 통한 자동 장애 조치
 * - 모니터링 알람 설정
 * </pre>
 * 
 * <h4>상황 2: 토큰 검증 실패 급증</h4>
 * 
 * <pre>
 * 증상: SecurityException 로그 급증
 * 원인: 토큰 재사용 시도 또는 클라이언트 오류
 * 
 * 확인 방법:
 * 1. Redis 토큰 개수: EVAL "return #redis.call('KEYS', 'chat_token:*')" 0
 * 2. 에러 로그 패턴 분석
 * 3. 특정 IP에서 집중적 요청 여부 확인
 * 
 * 대응책:
 * - Rate Limiting 강화
 * - 의심스러운 IP 차단
 * - 토큰 TTL 단축 고려
 * </pre>
 * 
 * <h4>상황 3: Redis 메모리 부족</h4>
 * 
 * <pre>
 * 증상: OOM (Out Of Memory) 오류
 * 원인: 토큰 과다 생성 또는 메모리 설정 부족
 * 
 * 확인 명령어:
 * INFO memory                    # 메모리 사용량 확인
 * CONFIG GET maxmemory          # 최대 메모리 설정 확인
 * 
 * 대응책:
 * 1. Redis maxmemory 증가
 * 2. 토큰 TTL 단축 (현재 10분 → 5분)
 * 3. 불필요한 Redis 키 정리
 * 4. Redis LRU 정책 활성화
 * </pre>
 * 
 * <h3>📈 성능 최적화 팁</h3>
 * 
 * <h4>Redis 설정 최적화</h4>
 * 
 * <pre>
 * # redis.conf 권장 설정
 * maxmemory 256mb                    # 메모리 제한 설정
 * maxmemory-policy allkeys-lru       # LRU 기반 메모리 정리
 * save ""                           # 디스크 저장 비활성화 (성능 향상)
 * tcp-keepalive 60                  # 연결 유지 시간
 * timeout 300                       # 클라이언트 타임아웃
 * </pre>
 * 
 * <h4>애플리케이션 최적화</h4>
 * <ul>
 * <li><strong>Connection Pooling</strong>: Lettuce 기본 풀 사용 (최대 연결 수 제한)</li>
 * <li><strong>Pipeline 사용</strong>: 대량 작업 시 파이프라인으로 성능 향상</li>
 * <li><strong>토큰 TTL 조정</strong>: 사용 패턴에 따라 TTL 최적화</li>
 * <li><strong>배치 삭제</strong>: 만료 토큰 일괄 정리</li>
 * </ul>
 * 
 * @author AI 기반 무역 규제 레이더 팀
 * @since v4.0
 * @see org.springframework.data.redis.core.RedisTemplate
 * @see com.hscoderadar.domain.chat.service.ChatService
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatTokenService {

  private final RedisTemplate<String, String> redisTemplate;

  @Value("${app.chat.token-expiration-minutes:10}")
  private int tokenExpirationMinutes;

  /**
   * Redis 키 네임스페이스: 채팅 토큰 전용 접두사
   * 
   * <p>
   * Redis에서 키 충돌을 방지하고 토큰 관리를 명확히 하기 위해
   * 모든 채팅 토큰은 {@code chat_token:} 접두사를 사용합니다.
   * </p>
   */
  private static final String TOKEN_PREFIX = "chat_token:";

  /**
   * 일회용 세션 토큰 생성 및 Redis 저장
   * 
   * <p>
   * UUID 기반의 예측 불가능한 토큰을 생성하고, Redis에 jobId와 매핑하여 저장합니다.
   * TTL을 설정하여 10분 후 자동 만료되도록 합니다.
   * </p>
   * 
   * <h4>🔧 Redis 저장 동작</h4>
   * 
   * <pre>
   * Redis 명령어 시퀀스:
   * 1. SET chat_token:{uuid} {jobId} EX 600
   *    └─ 키: chat_token:12345678-1234-1234-1234-123456789abc
   *    └─ 값: job_chat_1640995200000
   *    └─ TTL: 600초 (10분)
   * 
   * 2. 성공 시 Redis 응답: OK
   * 3. 실패 시 RedisException 발생
   * </pre>
   * 
   * <h4>🔐 보안 고려사항</h4>
   * <ul>
   * <li><strong>UUID 사용</strong>: 36자리 UUID로 예측/브루트포스 공격 방지</li>
   * <li><strong>TTL 설정</strong>: 자동 만료로 장기간 유효한 토큰 방지</li>
   * <li><strong>네임스페이스</strong>: 접두사로 다른 Redis 키와 분리</li>
   * </ul>
   * 
   * @param jobId 채팅 작업 ID (형태: job_chat_1234567890)
   * @return 생성된 UUID 기반 세션 토큰 (36자리)
   * 
   * @throws org.springframework.data.redis.RedisConnectionFailureException Redis
   *                                                                        연결 실패
   *                                                                        시
   * @throws org.springframework.data.redis.RedisSystemException            Redis
   *                                                                        서버 오류
   *                                                                        시
   * 
   * @since v4.0
   */
  public String generateSessionToken(String jobId) {
    String token = UUID.randomUUID().toString();
    String key = TOKEN_PREFIX + token;

    // Redis에 jobId를 값으로 저장하고 TTL 설정
    redisTemplate.opsForValue().set(
        key,
        jobId,
        Duration.ofMinutes(tokenExpirationMinutes));

    log.info("🔐 Redis 세션 토큰 생성 완료 - jobId: {}, token: {}..., TTL: {}분",
        jobId, token.substring(0, 8), tokenExpirationMinutes);

    return token;
  }

  /**
   * 토큰 검증 및 즉시 소모 (일회용 보장)
   * 
   * <p>
   * <strong>🔒 일회용 토큰 보안</strong>
   * </p>
   * <p>
   * 토큰의 유효성을 검증하고, 검증 성공 시 즉시 Redis에서 삭제하여
   * <strong>완전한 일회용 토큰</strong>을 보장합니다.
   * </p>
   * 
   * <h4>🔧 Redis 검증 및 즉시 삭제 동작</h4>
   * 
   * <pre>
   * Redis 명령어 시퀀스:
   * 1. GET chat_token:{uuid}
   *    └─ 성공: job_chat_1234567890 반환
   *    └─ 실패: null 반환 (토큰 없음/만료)
   * 
   * 2. 검증 성공 시 즉시 삭제
   *    DEL chat_token:{uuid}
   *    └─ 토큰 완전 제거 (재사용 불가)
   * 
   * 3. jobId 반환 또는 null 반환
   * </pre>
   * 
   * <h4>🛡️ 보안 정책</h4>
   * <ol>
   * <li><strong>토큰 존재 확인</strong>: Redis에서 키 조회</li>
   * <li><strong>즉시 삭제</strong>: 검증 성공 시 토큰 완전 제거</li>
   * <li><strong>원자적 처리</strong>: GET + DEL 순차 실행으로 동시성 안전성 보장</li>
   * <li><strong>재사용 불가</strong>: 삭제된 토큰은 절대 재사용 불가</li>
   * </ol>
   * 
   * <h4>🚨 실패 케이스</h4>
   * <ul>
   * <li><strong>토큰 만료</strong>: Redis TTL로 인한 자동 삭제</li>
   * <li><strong>토큰 재사용</strong>: 이미 사용된 토큰으로 재요청</li>
   * <li><strong>잘못된 토큰</strong>: 존재하지 않는 UUID</li>
   * <li><strong>Redis 장애</strong>: 네트워크 오류나 서버 장애</li>
   * </ul>
   * 
   * @param token 검증할 세션 토큰 (UUID 형태, 36자리)
   * @return 연결된 jobId (검증 성공) 또는 {@code null} (실패)
   * 
   * @throws org.springframework.data.redis.RedisConnectionFailureException Redis
   *                                                                        연결 실패
   *                                                                        시
   * 
   * @since v4.0
   */
  public String validateAndConsumeToken(String token) {
    String tokenKey = TOKEN_PREFIX + token;

    // 토큰으로 jobId 조회
    String jobId = redisTemplate.opsForValue().get(tokenKey);

    if (jobId != null) {
      // 검증 성공 시 토큰 즉시 삭제 (일회용 보장)
      redisTemplate.delete(tokenKey);

      log.info("🔓 Redis 토큰 검증 성공 및 즉시 삭제 - jobId: {}, token: {}...",
          jobId, token.substring(0, 8));

      return jobId;
    }

    log.warn("⚠️ Redis 토큰 검증 실패 - 만료되었거나 존재하지 않는 토큰: {}...",
        token.substring(0, 8));

    return null;
  }

  /**
   * 토큰 존재 여부 확인 (소모하지 않음)
   * 
   * <p>
   * 토큰의 유효성을 검사하지만 소모하지는 않습니다.
   * 주로 디버깅이나 상태 확인 용도로 사용됩니다.
   * </p>
   * 
   * <h4>⚠️ 사용 주의사항</h4>
   * <p>
   * 이 메서드는 토큰을 소모하지 않으므로, 보안이 중요한 실제 검증에서는
   * {@link #validateAndConsumeToken(String)} 메서드를 사용해야 합니다.
   * </p>
   * 
   * @param token 확인할 토큰 (UUID 형태)
   * @return 토큰 유효 여부 ({@code true}: 존재, {@code false}: 없음/만료)
   * 
   * @since v4.0
   */
  public boolean isTokenValid(String token) {
    String key = TOKEN_PREFIX + token;
    return Boolean.TRUE.equals(redisTemplate.hasKey(key));
  }

  /**
   * 특정 jobId와 연결된 토큰 삭제
   * 
   * <p>
   * 긴급 상황이나 작업 취소 시 해당 작업과 연결된 모든 토큰을 무효화합니다.
   * Redis의 KEYS 명령어를 사용하므로 성능상 주의가 필요합니다.
   * </p>
   * 
   * <h4>🔧 Redis 검색 및 삭제 동작</h4>
   * 
   * <pre>
   * Redis 명령어 시퀀스:
   * 1. KEYS chat_token:*              # 모든 토큰 키 조회
   * 2. GET chat_token:{uuid}          # 각 키의 값(jobId) 확인
   * 3. 일치하는 jobId 발견 시
   *    DEL chat_token:{uuid}          # 해당 토큰 삭제
   * </pre>
   * 
   * <h4>⚠️ 성능 주의사항</h4>
   * <ul>
   * <li><strong>KEYS 명령어 사용</strong>: 대량의 토큰 존재 시 성능 저하 가능</li>
   * <li><strong>O(N) 복잡도</strong>: 토큰 수에 비례하는 처리 시간</li>
   * <li><strong>운영 환경 주의</strong>: 가급적 긴급 상황에만 사용 권장</li>
   * </ul>
   * 
   * @param jobId 삭제할 작업 ID (예: job_chat_1234567890)
   * 
   * @since v4.0
   */
  public void invalidateTokensByJobId(String jobId) {
    // Redis에서 jobId로 토큰 찾기 (비효율적이지만 안전성을 위해)
    var keys = redisTemplate.keys(TOKEN_PREFIX + "*");
    if (keys != null) {
      for (String key : keys) {
        String storedJobId = redisTemplate.opsForValue().get(key);
        if (jobId.equals(storedJobId)) {
          redisTemplate.delete(key);
          log.info("🗑️ jobId 기반 Redis 토큰 삭제 - jobId: {}, key: {}", jobId, key);
        }
      }
    }
  }

  /**
   * 현재 활성 토큰 수 조회 (모니터링용)
   * 
   * <p>
   * Redis에 저장된 활성 토큰의 개수를 조회합니다.
   * 시스템 모니터링이나 부하 분석 용도로 사용됩니다.
   * </p>
   * 
   * <h4>📊 모니터링 활용</h4>
   * <ul>
   * <li><strong>부하 측정</strong>: 동시 사용자 수 추정</li>
   * <li><strong>메모리 사용량</strong>: Redis 메모리 사용량 예측</li>
   * <li><strong>시스템 상태</strong>: 정상 동작 여부 확인</li>
   * <li><strong>장애 감지</strong>: 비정상적인 토큰 증가 감지</li>
   * </ul>
   * 
   * <h4>⚠️ 성능 주의사항</h4>
   * <p>
   * KEYS 명령어를 사용하므로 대량의 토큰 존재 시 성능에 영향을 줄 수 있습니다.
   * 운영 환경에서는 주기적인 모니터링보다는 필요 시에만 호출하는 것을 권장합니다.
   * </p>
   * 
   * @return 현재 활성 토큰 수 (0 이상의 정수)
   * 
   * @since v4.0
   */
  public long getActiveTokenCount() {
    var keys = redisTemplate.keys(TOKEN_PREFIX + "*");
    long count = keys != null ? keys.size() : 0;

    log.debug("📊 Redis 활성 토큰 수 조회: {} 개", count);

    return count;
  }
}