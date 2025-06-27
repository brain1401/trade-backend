package com.hscoderadar.domain.users.entity;

import com.hscoderadar.domain.bookmarks.entity.Bookmark;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 사용자 정보 엔티티 (v4.2)
 * 
 * 사용자의 기본 정보와 인증 관련 데이터를 관리
 * SNS 계정 연동은 별도 SnsAccount 테이블로 관리
 */
@Entity
@Table(name = "users")
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(unique = true, nullable = false)
  private String email;

  @Column(name = "password_hash")
  private String passwordHash;

  @Column(nullable = false)
  private String name;

  @Column(name = "profile_image", length = 500)
  private String profileImage;

  // v4.2: SMS 알림 관련 필드
  @Column(name = "phone_number", length = 100)
  private String phoneNumber;

  @Column(name = "phone_verified", nullable = false)
  @Builder.Default
  private Boolean phoneVerified = false;

  @Column(name = "phone_verified_at")
  private LocalDateTime phoneVerifiedAt;

  @Column(name = "refresh_token", length = 500)
  private String refreshToken;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Column(name = "last_login_at")
  private LocalDateTime lastLoginAt;

  // 관계 매핑
  @ToString.Exclude
  @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
  private UserSettings userSettings;

  @ToString.Exclude
  @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  private List<SnsAccount> snsAccounts;

  @ToString.Exclude
  @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  private List<Bookmark> bookmarks;
}