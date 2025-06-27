package com.hscoderadar.domain.sms.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

/**
 * v4.1 SMS Redis 관리 서비스
 * 쿨다운, 일일 시도 횟수 제한, 스팸 방지 등을 Redis로 관리
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SmsRedisService {

  private final RedisTemplate<String, String> redisTemplate;

  // Redis 키 패턴
  private static final String COOLDOWN_KEY_PREFIX = "sms_cooldown:";
  private static final String DAILY_ATTEMPTS_KEY_PREFIX = "sms_daily_attempts:";
  private static final String HOURLY_ATTEMPTS_KEY_PREFIX = "sms_hourly_attempts:";

  // 제한 설정
  private static final int COOLDOWN_MINUTES = 2;
  private static final int MAX_DAILY_ATTEMPTS = 10;
  private static final int MAX_HOURLY_ATTEMPTS = 5;

  /**
   * 쿨다운 설정 (2분)
   */
  public void setCooldown(Long userId, String phoneNumber) {
    String key = COOLDOWN_KEY_PREFIX + userId + ":" + phoneNumber;
    redisTemplate.opsForValue().set(key, "true", COOLDOWN_MINUTES, TimeUnit.MINUTES);
    log.debug("SMS 쿨다운 설정: userId={}, phoneNumber={}, duration={}분", userId, phoneNumber, COOLDOWN_MINUTES);
  }

  /**
   * 쿨다운 중인지 확인
   */
  public boolean isInCooldown(Long userId, String phoneNumber) {
    String key = COOLDOWN_KEY_PREFIX + userId + ":" + phoneNumber;
    return Boolean.TRUE.equals(redisTemplate.hasKey(key));
  }

  /**
   * 쿨다운 남은 시간 조회 (초)
   */
  public long getCooldownRemainingSeconds(Long userId, String phoneNumber) {
    String key = COOLDOWN_KEY_PREFIX + userId + ":" + phoneNumber;
    Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
    return ttl != null ? Math.max(0, ttl) : 0;
  }

  /**
   * 일일 시도 횟수 증가
   */
  public void incrementDailyAttempts(Long userId) {
    String key = DAILY_ATTEMPTS_KEY_PREFIX + userId + ":" + LocalDate.now();
    String currentCount = redisTemplate.opsForValue().get(key);

    if (currentCount == null) {
      // 첫 시도인 경우 자정까지 TTL 설정
      long secondsUntilMidnight = ChronoUnit.SECONDS.between(
          LocalDateTime.now(),
          LocalDateTime.now().plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0));
      redisTemplate.opsForValue().set(key, "1", secondsUntilMidnight, TimeUnit.SECONDS);
    } else {
      redisTemplate.opsForValue().increment(key);
    }

    log.debug("일일 SMS 시도 횟수 증가: userId={}, count={}", userId, getDailyAttempts(userId));
  }

  /**
   * 시간당 시도 횟수 증가
   */
  public void incrementHourlyAttempts(Long userId) {
    String key = HOURLY_ATTEMPTS_KEY_PREFIX + userId + ":" + LocalDateTime.now().getHour();
    String currentCount = redisTemplate.opsForValue().get(key);

    if (currentCount == null) {
      // 첫 시도인 경우 1시간 TTL 설정
      redisTemplate.opsForValue().set(key, "1", 1, TimeUnit.HOURS);
    } else {
      redisTemplate.opsForValue().increment(key);
    }

    log.debug("시간당 SMS 시도 횟수 증가: userId={}, count={}", userId, getHourlyAttempts(userId));
  }

  /**
   * 일일 시도 횟수 조회
   */
  public int getDailyAttempts(Long userId) {
    String key = DAILY_ATTEMPTS_KEY_PREFIX + userId + ":" + LocalDate.now();
    String count = redisTemplate.opsForValue().get(key);
    return count != null ? Integer.parseInt(count) : 0;
  }

  /**
   * 시간당 시도 횟수 조회
   */
  public int getHourlyAttempts(Long userId) {
    String key = HOURLY_ATTEMPTS_KEY_PREFIX + userId + ":" + LocalDateTime.now().getHour();
    String count = redisTemplate.opsForValue().get(key);
    return count != null ? Integer.parseInt(count) : 0;
  }

  /**
   * 일일 시도 횟수 초과 여부 확인
   */
  public boolean isDailyLimitExceeded(Long userId) {
    return getDailyAttempts(userId) >= MAX_DAILY_ATTEMPTS;
  }

  /**
   * 시간당 시도 횟수 초과 여부 확인
   */
  public boolean isHourlyLimitExceeded(Long userId) {
    return getHourlyAttempts(userId) >= MAX_HOURLY_ATTEMPTS;
  }

  /**
   * 모든 제한 사항 확인
   */
  public SmsLimitCheckResult checkLimits(Long userId, String phoneNumber) {
    return SmsLimitCheckResult.builder()
        .inCooldown(isInCooldown(userId, phoneNumber))
        .cooldownRemainingSeconds(getCooldownRemainingSeconds(userId, phoneNumber))
        .dailyLimitExceeded(isDailyLimitExceeded(userId))
        .hourlyLimitExceeded(isHourlyLimitExceeded(userId))
        .dailyAttempts(getDailyAttempts(userId))
        .hourlyAttempts(getHourlyAttempts(userId))
        .maxDailyAttempts(MAX_DAILY_ATTEMPTS)
        .maxHourlyAttempts(MAX_HOURLY_ATTEMPTS)
        .build();
  }

  /**
   * SMS 제한 확인 결과
   */
  public static class SmsLimitCheckResult {
    private boolean inCooldown;
    private long cooldownRemainingSeconds;
    private boolean dailyLimitExceeded;
    private boolean hourlyLimitExceeded;
    private int dailyAttempts;
    private int hourlyAttempts;
    private int maxDailyAttempts;
    private int maxHourlyAttempts;

    public static SmsLimitCheckResultBuilder builder() {
      return new SmsLimitCheckResultBuilder();
    }

    public boolean isBlocked() {
      return inCooldown || dailyLimitExceeded || hourlyLimitExceeded;
    }

    public String getBlockReason() {
      if (inCooldown) {
        return String.format("쿨다운 중입니다. %d초 후 재시도 가능합니다.", cooldownRemainingSeconds);
      }
      if (dailyLimitExceeded) {
        return String.format("일일 발송 한도(%d회)를 초과했습니다.", maxDailyAttempts);
      }
      if (hourlyLimitExceeded) {
        return String.format("시간당 발송 한도(%d회)를 초과했습니다.", maxHourlyAttempts);
      }
      return null;
    }

    // Getters and Builder class
    public boolean isInCooldown() {
      return inCooldown;
    }

    public long getCooldownRemainingSeconds() {
      return cooldownRemainingSeconds;
    }

    public boolean isDailyLimitExceeded() {
      return dailyLimitExceeded;
    }

    public boolean isHourlyLimitExceeded() {
      return hourlyLimitExceeded;
    }

    public int getDailyAttempts() {
      return dailyAttempts;
    }

    public int getHourlyAttempts() {
      return hourlyAttempts;
    }

    public int getMaxDailyAttempts() {
      return maxDailyAttempts;
    }

    public int getMaxHourlyAttempts() {
      return maxHourlyAttempts;
    }

    public static class SmsLimitCheckResultBuilder {
      private boolean inCooldown;
      private long cooldownRemainingSeconds;
      private boolean dailyLimitExceeded;
      private boolean hourlyLimitExceeded;
      private int dailyAttempts;
      private int hourlyAttempts;
      private int maxDailyAttempts;
      private int maxHourlyAttempts;

      public SmsLimitCheckResultBuilder inCooldown(boolean inCooldown) {
        this.inCooldown = inCooldown;
        return this;
      }

      public SmsLimitCheckResultBuilder cooldownRemainingSeconds(long cooldownRemainingSeconds) {
        this.cooldownRemainingSeconds = cooldownRemainingSeconds;
        return this;
      }

      public SmsLimitCheckResultBuilder dailyLimitExceeded(boolean dailyLimitExceeded) {
        this.dailyLimitExceeded = dailyLimitExceeded;
        return this;
      }

      public SmsLimitCheckResultBuilder hourlyLimitExceeded(boolean hourlyLimitExceeded) {
        this.hourlyLimitExceeded = hourlyLimitExceeded;
        return this;
      }

      public SmsLimitCheckResultBuilder dailyAttempts(int dailyAttempts) {
        this.dailyAttempts = dailyAttempts;
        return this;
      }

      public SmsLimitCheckResultBuilder hourlyAttempts(int hourlyAttempts) {
        this.hourlyAttempts = hourlyAttempts;
        return this;
      }

      public SmsLimitCheckResultBuilder maxDailyAttempts(int maxDailyAttempts) {
        this.maxDailyAttempts = maxDailyAttempts;
        return this;
      }

      public SmsLimitCheckResultBuilder maxHourlyAttempts(int maxHourlyAttempts) {
        this.maxHourlyAttempts = maxHourlyAttempts;
        return this;
      }

      public SmsLimitCheckResult build() {
        SmsLimitCheckResult result = new SmsLimitCheckResult();
        result.inCooldown = this.inCooldown;
        result.cooldownRemainingSeconds = this.cooldownRemainingSeconds;
        result.dailyLimitExceeded = this.dailyLimitExceeded;
        result.hourlyLimitExceeded = this.hourlyLimitExceeded;
        result.dailyAttempts = this.dailyAttempts;
        result.hourlyAttempts = this.hourlyAttempts;
        result.maxDailyAttempts = this.maxDailyAttempts;
        result.maxHourlyAttempts = this.maxHourlyAttempts;
        return result;
      }
    }
  }
}