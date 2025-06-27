package com.hscoderadar.domain.user.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 사용자 기본 정보 엔티티 (v6.1 JWT 세부화 정책 지원)
 *
 * <p>v6.1 주요 특징: - JWT 세부화: Access Token 30분, Refresh Token 1일/30일 - 휴대폰 인증 지원 (AES-256 암호화) - 회원
 * 전용 채팅 시스템 연동 - OAuth 소셜 로그인 지원
 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 255)
  private String email;

  @Column(name = "password_hash", length = 255)
  private String passwordHash; // SNS 로그인 시 NULL 가능

  @Column(nullable = false, length = 100)
  private String name;

  @Column(name = "profile_image", length = 500)
  private String profileImage;

  @Column(name = "phone_number", length = 100)
  private String phoneNumber; // AES-256 암호화 저장

  @Column(name = "phone_verified", nullable = false)
  private Boolean phoneVerified = false;

  @Column(name = "phone_verified_at")
  private LocalDateTime phoneVerifiedAt;

  // 🆕 v6.1: JWT 세부화 정책 지원
  @Column(name = "refresh_token", length = 500)
  private String refreshToken; // 현재 유효한 리프레시 토큰

  @Column(name = "refresh_token_expires_at")
  private LocalDateTime refreshTokenExpiresAt; // 리프레시 토큰 만료 시간

  @Column(name = "remember_me_enabled", nullable = false)
  private Boolean rememberMeEnabled = false; // Remember me 설정 (30일 vs 1일)

  @Column(name = "last_token_refresh")
  private LocalDateTime lastTokenRefresh; // 마지막 토큰 갱신 시간

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  // 연관관계 매핑
  @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<SnsAccount> snsAccounts = new ArrayList<>();

  @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
  private UserSettings userSettings;

  @Builder
  public User(String email, String passwordHash, String name, String profileImage) {
    this.email = email;
    this.passwordHash = passwordHash;
    this.name = name;
    this.profileImage = profileImage;
    this.phoneVerified = false;
    this.rememberMeEnabled = false;
  }

  // 비즈니스 메서드

  /** 휴대폰 인증 완료 처리 */
  public void completePhoneVerification(String phoneNumber) {
    this.phoneNumber = phoneNumber;
    this.phoneVerified = true;
    this.phoneVerifiedAt = LocalDateTime.now();
  }

  /** JWT 토큰 정보 업데이트 (v6.1 세부화 정책) */
  public void updateRefreshToken(String refreshToken, LocalDateTime expiresAt, boolean rememberMe) {
    this.refreshToken = refreshToken;
    this.refreshTokenExpiresAt = expiresAt;
    this.rememberMeEnabled = rememberMe;
    this.lastTokenRefresh = LocalDateTime.now();
  }

  /** 리프레시 토큰 제거 (로그아웃 시) */
  public void clearRefreshToken() {
    this.refreshToken = null;
    this.refreshTokenExpiresAt = null;
    this.lastTokenRefresh = LocalDateTime.now();
  }

  /** 프로필 이미지 업데이트 */
  public void updateProfileImage(String profileImage) {
    this.profileImage = profileImage;
  }

  /** 사용자 이름 업데이트 */
  public void updateName(String name) {
    this.name = name;
  }

  /** 비밀번호 해시 업데이트 */
  public void updatePasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
  }

  /** Remember Me 설정 변경 */
  public void updateRememberMeEnabled(boolean rememberMeEnabled) {
    this.rememberMeEnabled = rememberMeEnabled;
  }

  /** 유효한 리프레시 토큰 보유 여부 확인 */
  public boolean hasValidRefreshToken() {
    return refreshToken != null
        && refreshTokenExpiresAt != null
        && refreshTokenExpiresAt.isAfter(LocalDateTime.now());
  }

  /** 휴대폰 인증 완료 여부 확인 */
  public boolean isPhoneVerified() {
    return Boolean.TRUE.equals(phoneVerified);
  }

  /** OAuth 전용 사용자 여부 확인 (비밀번호 없음) */
  public boolean isOAuthUser() {
    return passwordHash == null;
  }
}
