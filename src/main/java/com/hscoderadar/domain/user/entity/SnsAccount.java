package com.hscoderadar.domain.user.entity;

import com.hscoderadar.domain.user.entity.enums.SnsProvider;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * SNS 계정 연동 정보 엔티티
 *
 * <p>하나의 사용자가 여러 SNS 계정을 연동할 수 있으며, 각 SNS 제공업체별로 고유한 연동 정보를 저장함
 */
@Entity
@Table(
    name = "sns_accounts",
    uniqueConstraints = @UniqueConstraint(columnNames = {"provider", "provider_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class SnsAccount {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private SnsProvider provider;

  @Column(name = "provider_id", nullable = false, length = 255)
  private String providerId; // SNS 제공업체에서 발급한 사용자 ID

  @Column(name = "provider_email", nullable = false, length = 255)
  private String providerEmail; // SNS 계정의 이메일

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Builder
  public SnsAccount(User user, SnsProvider provider, String providerId, String providerEmail) {
    this.user = user;
    this.provider = provider;
    this.providerId = providerId;
    this.providerEmail = providerEmail;
  }

  // 비즈니스 메서드

  /** SNS 계정 정보 업데이트 */
  public void updateProviderInfo(String providerId, String providerEmail) {
    this.providerId = providerId;
    this.providerEmail = providerEmail;
  }

  /** 연동된 SNS 제공업체 이름 조회 */
  public String getProviderDisplayName() {
    return provider.getDisplayName();
  }

  /** 동일한 제공업체인지 확인 */
  public boolean isSameProvider(SnsProvider provider) {
    return this.provider == provider;
  }

  /** 동일한 제공업체 계정인지 확인 */
  public boolean isSameAccount(SnsProvider provider, String providerId) {
    return this.provider == provider && this.providerId.equals(providerId);
  }
}
