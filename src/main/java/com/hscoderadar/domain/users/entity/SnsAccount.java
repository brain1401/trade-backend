package com.hscoderadar.domain.users.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "sns_accounts")
@Data
@EqualsAndHashCode(callSuper = false)
public class SnsAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SnsProvider provider;

    @Column(name = "provider_id", nullable = false)
    private String providerId;

    @Column(name = "provider_email", nullable = false)
    private String providerEmail;
    
    @Column(name = "provider_name", nullable = false, length = 100)
    private String providerName;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum SnsProvider {
        GOOGLE, KAKAO, NAVER
    }
}