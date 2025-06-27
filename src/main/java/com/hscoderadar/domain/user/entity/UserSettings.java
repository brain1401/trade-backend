package com.hscoderadar.domain.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 사용자 설정 정보 엔티티
 * 
 * SMS/이메일 통합 알림 설정을 관리하며,
 * 사용자별 알림 선호도를 저장함
 */
@Entity
@Table(name = "user_settings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class UserSettings {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false, unique = true)
  private User user;

  // 통합 알림 설정 (SMS/이메일 동시 지원)
  @Column(name = "sms_notification_enabled", nullable = false)
  private Boolean smsNotificationEnabled = false; // 전체 SMS 알림 활성화

  @Column(name = "email_notification_enabled", nullable = false)
  private Boolean emailNotificationEnabled = true; // 전체 이메일 알림 활성화

  // 알림 발송 주기 설정 (개발자 제어)
  @Column(name = "notification_frequency", nullable = false, length = 20)
  private String notificationFrequency = "DAILY"; // 알림 주기: DAILY, WEEKLY

  // 알림 시간 설정
  @Column(name = "notification_time", nullable = false)
  private LocalTime notificationTime = LocalTime.of(9, 0); // 일일 알림 발송 시간

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Builder
  public UserSettings(User user) {
    this.user = user;
    this.smsNotificationEnabled = false;
    this.emailNotificationEnabled = true;
    this.notificationFrequency = "DAILY";
    this.notificationTime = LocalTime.of(9, 0);
  }

  // 비즈니스 메서드

  /**
   * SMS 알림 설정 업데이트
   */
  public void updateSmsNotificationEnabled(boolean enabled) {
    this.smsNotificationEnabled = enabled;
  }

  /**
   * 이메일 알림 설정 업데이트
   */
  public void updateEmailNotificationEnabled(boolean enabled) {
    this.emailNotificationEnabled = enabled;
  }

  /**
   * 알림 주기 설정 업데이트
   */
  public void updateNotificationFrequency(String frequency) {
    if (!isValidFrequency(frequency)) {
      throw new IllegalArgumentException("유효하지 않은 알림 주기: " + frequency);
    }
    this.notificationFrequency = frequency;
  }

  /**
   * 알림 시간 설정 업데이트
   */
  public void updateNotificationTime(LocalTime time) {
    this.notificationTime = time;
  }

  /**
   * 통합 알림 설정 업데이트
   */
  public void updateNotificationSettings(
      boolean smsEnabled,
      boolean emailEnabled,
      String frequency,
      LocalTime time) {
    this.smsNotificationEnabled = smsEnabled;
    this.emailNotificationEnabled = emailEnabled;
    updateNotificationFrequency(frequency);
    this.notificationTime = time;
  }

  /**
   * 알림 활성화 여부 확인 (SMS 또는 이메일 중 하나라도 활성화)
   */
  public boolean isNotificationEnabled() {
    return Boolean.TRUE.equals(smsNotificationEnabled) ||
        Boolean.TRUE.equals(emailNotificationEnabled);
  }

  /**
   * SMS 알림 활성화 여부 확인
   */
  public boolean isSmsNotificationEnabled() {
    return Boolean.TRUE.equals(smsNotificationEnabled);
  }

  /**
   * 이메일 알림 활성화 여부 확인
   */
  public boolean isEmailNotificationEnabled() {
    return Boolean.TRUE.equals(emailNotificationEnabled);
  }

  /**
   * 유효한 알림 주기인지 확인
   */
  private boolean isValidFrequency(String frequency) {
    return "DAILY".equals(frequency) || "WEEKLY".equals(frequency);
  }

  /**
   * 일일 알림 여부 확인
   */
  public boolean isDailyNotification() {
    return "DAILY".equals(notificationFrequency);
  }

  /**
   * 주간 알림 여부 확인
   */
  public boolean isWeeklyNotification() {
    return "WEEKLY".equals(notificationFrequency);
  }
}