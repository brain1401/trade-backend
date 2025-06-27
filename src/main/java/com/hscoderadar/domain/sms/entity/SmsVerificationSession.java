package com.hscoderadar.domain.sms.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;
import org.springframework.data.redis.core.index.Indexed;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * v4.1 SMS 인증 세션 Redis 엔티티
 * 휴대폰 번호 인증을 위한 6자리 코드 발송 및 검증 관리
 * Redis TTL을 활용한 자동 만료 처리 (5분)
 */
@RedisHash("sms_verification_session")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SmsVerificationSession implements Serializable {

  @Id
  private String verificationId;

  @Indexed
  private Long userId;

  @Indexed
  private String phoneNumber;

  private String verificationCodeHash;

  @Builder.Default
  private Integer attemptCount = 0;

  @Builder.Default
  private Integer maxAttempts = 5;

  @Builder.Default
  private Boolean isVerified = false;

  private LocalDateTime createdAt;

  private LocalDateTime verifiedAt;

  /**
   * TTL 설정 (초 단위)
   * 5분 = 300초 후 자동 삭제
   */
  @TimeToLive
  @Builder.Default
  private Long ttl = 300L;

  /**
   * 인증 시도 횟수 증가
   */
  public void incrementAttemptCount() {
    this.attemptCount++;
  }

  /**
   * 최대 시도 횟수 초과 여부 확인
   */
  public boolean isMaxAttemptsExceeded() {
    return this.attemptCount >= this.maxAttempts;
  }

  /**
   * 인증 완료 처리
   */
  public void markAsVerified() {
    this.isVerified = true;
    this.verifiedAt = LocalDateTime.now();
  }

  /**
   * 생성 시 자동으로 현재 시간 설정
   */
  public void onCreate() {
    if (this.createdAt == null) {
      this.createdAt = LocalDateTime.now();
    }
  }
}