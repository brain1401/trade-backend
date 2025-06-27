package com.hscoderadar.domain.users.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * SNS 계정 연동 정보 엔티티 (v4.2)
 * 
 * 사용자와 소셜 로그인 제공업체 간의 연동 정보를 관리
 * Google, Kakao, Naver 등의 소셜 계정과 사용자 계정을 연결
 */
@Entity
@Table(name = "sns_accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, exclude = { "user" })
@ToString(exclude = { "user" })
public class SnsAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 연결된 사용자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * SNS 제공업체 (Google, Kakao, Naver)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Provider provider;

    /**
     * SNS 제공업체의 사용자 ID
     */
    @Column(name = "provider_id", nullable = false)
    private String providerId;

    /**
     * SNS 제공업체 이메일
     */
    @Column(name = "provider_email", nullable = false)
    private String providerEmail;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * SNS 제공업체 열거형
     */
    public enum Provider {
        GOOGLE, KAKAO, NAVER
    }
}