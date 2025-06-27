package com.hscoderadar.domain.sms.entity;

import com.hscoderadar.domain.users.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * v4.0 SMS 발송 로그 엔티티
 * 모든 SMS 발송 이력과 상태를 추적
 */
@Entity
@Table(name = "sms_logs")
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SmsLog {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ToString.Exclude
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "phone_number", length = 20, nullable = false)
  private String phoneNumber;

  @Enumerated(EnumType.STRING)
  @Column(name = "message_type", nullable = false)
  private MessageType messageType;

  @Column(name = "content", columnDefinition = "TEXT", nullable = false)
  private String content;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  @Builder.Default
  private SmsStatus status = SmsStatus.PENDING;

  @Column(name = "external_message_id", length = 100)
  private String externalMessageId;

  @Column(name = "error_message", columnDefinition = "TEXT")
  private String errorMessage;

  @Column(name = "cost_krw")
  private Integer costKrw;

  @Column(name = "sent_at")
  private LocalDateTime sentAt;

  @Column(name = "delivered_at")
  private LocalDateTime deliveredAt;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  /**
   * SMS 메시지 타입
   */
  public enum MessageType {
    VERIFICATION, // 인증 코드
    NOTIFICATION // 알림 메시지
  }

  /**
   * SMS 발송 상태
   */
  public enum SmsStatus {
    PENDING, // 발송 대기
    SENT, // 발송 완료
    FAILED, // 발송 실패
    DELIVERED // 전달 완료
  }

  /**
   * 발송 성공 처리
   */
  public void markAsSent(String externalMessageId) {
    this.status = SmsStatus.SENT;
    this.externalMessageId = externalMessageId;
    this.sentAt = LocalDateTime.now();
  }

  /**
   * 발송 실패 처리
   */
  public void markAsFailed(String errorMessage) {
    this.status = SmsStatus.FAILED;
    this.errorMessage = errorMessage;
  }

  /**
   * 전달 완료 처리
   */
  public void markAsDelivered() {
    this.status = SmsStatus.DELIVERED;
    this.deliveredAt = LocalDateTime.now();
  }

  /**
   * 휴대폰 번호 마스킹 처리 (개인정보 보호)
   */
  public String getMaskedPhoneNumber() {
    if (phoneNumber == null || phoneNumber.length() < 8) {
      return phoneNumber;
    }
    String prefix = phoneNumber.substring(0, 3);
    String suffix = phoneNumber.substring(phoneNumber.length() - 4);
    return prefix + "-****-" + suffix;
  }
}