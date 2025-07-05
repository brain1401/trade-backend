package com.hscoderadar.config.jwt;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * JWT 토큰 관리를 위한 Redis 서비스
 *
 * <p>
 * v6.1 스키마 요구사항에 따른 Redis 구조 구현: - jwt:refresh_in_progress:{userId} # 토큰 갱신 진행
 * 중 상태 관리 -
 * jwt:blacklist:{tokenJti} # 토큰 블랙리스트 - jwt:issue_log:{userId}:{date} # 토큰 발급
 * 기록
 */
@Service
@Slf4j
public class JwtRedisService {

  private final RedisTemplate<String, Object> redisTemplate;

  public JwtRedisService(@Qualifier("redisObjectTemplate") RedisTemplate<String, Object> redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  private static final String REFRESH_IN_PROGRESS_PREFIX = "jwt:refresh_in_progress:";
  private static final String BLACKLIST_PREFIX = "jwt:blacklist:";
  private static final String ISSUE_LOG_PREFIX = "jwt:issue_log:";

  /**
   * 토큰 갱신 진행 중 상태를 Redis에 저장 TTL: 30초
   *
   * @param userId          사용자 ID
   * @param oldRefreshToken 기존 리프레시 토큰
   * @param newRefreshToken 새로운 리프레시 토큰
   * @param accessToken     새로운 액세스 토큰
   * @param rememberMe      remember me 설정
   */
  public void setRefreshInProgress(
      Long userId,
      String oldRefreshToken,
      String newRefreshToken,
      String accessToken,
      boolean rememberMe) {
    String key = REFRESH_IN_PROGRESS_PREFIX + userId;

    Map<String, Object> refreshData = new HashMap<>();
    refreshData.put("oldRefreshToken", oldRefreshToken);
    refreshData.put("newRefreshToken", newRefreshToken);
    refreshData.put("accessToken", accessToken);
    refreshData.put("rememberMe", rememberMe);
    refreshData.put("startedAt", System.currentTimeMillis());

    redisTemplate.opsForHash().putAll(key, refreshData);
    redisTemplate.expire(key, Duration.ofSeconds(30));

    log.debug("토큰 갱신 진행 상태 저장: userId={}, rememberMe={}", userId, rememberMe);
  }

  /**
   * 토큰 갱신 진행 중 상태를 Redis에서 조회
   *
   * @param userId 사용자 ID
   * @return 갱신 진행 상태 데이터
   */
  public Map<Object, Object> getRefreshInProgress(Long userId) {
    String key = REFRESH_IN_PROGRESS_PREFIX + userId;
    return redisTemplate.opsForHash().entries(key);
  }

  /**
   * 토큰 갱신 완료 후 진행 상태 삭제
   *
   * @param userId 사용자 ID
   */
  public void clearRefreshInProgress(Long userId) {
    String key = REFRESH_IN_PROGRESS_PREFIX + userId;
    redisTemplate.delete(key);
    log.debug("토큰 갱신 진행 상태 삭제: userId={}", userId);
  }

  /**
   * 토큰을 블랙리스트에 추가 TTL: 원본 토큰의 만료 시간과 동일
   *
   * @param tokenJti           토큰 JTI (JWT ID)
   * @param reason             블랙리스트 사유
   * @param userId             사용자 ID
   * @param originalTtlSeconds 원본 토큰 TTL (초)
   */
  public void addToBlacklist(String tokenJti, String reason, Long userId, long originalTtlSeconds) {
    String key = BLACKLIST_PREFIX + tokenJti;

    Map<String, Object> blacklistData = new HashMap<>();
    blacklistData.put("reason", reason);
    blacklistData.put("userId", userId);
    blacklistData.put("blacklistedAt", System.currentTimeMillis());

    redisTemplate.opsForHash().putAll(key, blacklistData);
    redisTemplate.expire(key, Duration.ofSeconds(originalTtlSeconds));

    log.info("토큰 블랙리스트 추가: jti={}, reason={}, userId={}", tokenJti, reason, userId);
  }

  /**
   * 토큰이 블랙리스트에 있는지 확인
   *
   * @param tokenJti 토큰 JTI
   * @return 블랙리스트에 있으면 true
   */
  public boolean isTokenBlacklisted(String tokenJti) {
    String key = BLACKLIST_PREFIX + tokenJti;
    return Boolean.TRUE.equals(redisTemplate.hasKey(key));
  }

  /**
   * 토큰 발급 기록을 Redis에 저장 TTL: 24시간
   *
   * @param userId        사용자 ID
   * @param isAccessToken 액세스 토큰 여부 (false면 리프레시 토큰)
   */
  public void logTokenIssue(Long userId, boolean isAccessToken) {
    String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    String key = ISSUE_LOG_PREFIX + userId + ":" + today;

    String tokenTypeField = isAccessToken ? "accessTokenCount" : "refreshTokenCount";

    // 카운트 증가
    redisTemplate.opsForHash().increment(key, tokenTypeField, 1);

    // 마지막 발급 시간 업데이트
    redisTemplate.opsForHash().put(key, "lastIssueTime", System.currentTimeMillis());

    // TTL 설정 (24시간)
    redisTemplate.expire(key, Duration.ofDays(1));

    log.debug(
        "토큰 발급 기록: userId={}, type={}, date={}",
        userId,
        isAccessToken ? "ACCESS" : "REFRESH",
        today);
  }

  /**
   * 사용자의 일일 토큰 발급 기록 조회
   *
   * @param userId 사용자 ID
   * @return 토큰 발급 기록
   */
  public Map<Object, Object> getTokenIssueLog(Long userId) {
    String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    String key = ISSUE_LOG_PREFIX + userId + ":" + today;
    return redisTemplate.opsForHash().entries(key);
  }

  /**
   * 사용자의 일일 토큰 발급 횟수 확인 (보안 모니터링용)
   *
   * @param userId    사용자 ID
   * @param tokenType 토큰 타입 ("ACCESS" 또는 "REFRESH")
   * @return 발급 횟수
   */
  public long getTokenIssueCount(Long userId, String tokenType) {
    String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    String key = ISSUE_LOG_PREFIX + userId + ":" + today;
    String field = tokenType.equals("ACCESS") ? "accessTokenCount" : "refreshTokenCount";

    Object count = redisTemplate.opsForHash().get(key, field);
    return count != null ? Long.parseLong(count.toString()) : 0L;
  }

  /**
   * 사용자의 모든 토큰 갱신 진행 상태 정리 (정리 작업용)
   *
   * @param userId 사용자 ID
   */
  public void cleanupUserTokenStates(Long userId) {
    clearRefreshInProgress(userId);
    log.info("사용자 토큰 상태 정리 완료: userId={}", userId);
  }
}
