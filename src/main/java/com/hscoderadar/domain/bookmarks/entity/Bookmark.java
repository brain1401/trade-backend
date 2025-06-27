package com.hscoderadar.domain.bookmarks.entity;

import com.hscoderadar.domain.users.entity.User;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "bookmarks")
@Data
@EqualsAndHashCode(callSuper = false)
public class Bookmark {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "bookmark_id", length = 20, unique = true)
  private String bookmarkId;

  @ToString.Exclude
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private BookmarkType type;

  @Column(name = "target_value", length = 50, nullable = false)
  private String targetValue;

  @Column(name = "display_name", length = 100, nullable = false)
  private String displayName;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Column(name = "monitoring_enabled", nullable = false)
  private Boolean monitoringEnabled = true;

  @Column(name = "sms_notification_enabled", nullable = false)
  private Boolean smsNotificationEnabled = false;

  @Column(name = "alert_count", nullable = false)
  private Integer alertCount = 0;

  @Column(name = "last_alert")
  private LocalDateTime lastAlert;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  public enum BookmarkType {
    HS_CODE, CARGO
  }
}