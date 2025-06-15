package com.hscoderadar.domain.bookmark.entity;

import com.hscoderadar.domain.user.entity.User;
import com.hscoderadar.domain.monitoring.entity.ChangeDetectionLog;
import com.hscoderadar.domain.notification.entity.PushNotification;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "bookmarks")
@Data
@EqualsAndHashCode(callSuper = false)
public class Bookmark {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "hscode", length = 20)
  private String hsCode;

  @Column(name = "product_name", nullable = false)
  private String productName;

  @Column(name = "monitoring_keywords", nullable = false, columnDefinition = "JSON")
  @JdbcTypeCode(SqlTypes.JSON)
  private List<String> monitoringKeywords;

  @Column(name = "is_active", nullable = false)
  private Boolean isActive = true;

  @Column(name = "last_checked_at")
  private LocalDateTime lastCheckedAt;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  // 관계 매핑
  @OneToMany(mappedBy = "bookmark", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  private List<ChangeDetectionLog> changeDetectionLogs;

  @OneToMany(mappedBy = "bookmark", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  private List<PushNotification> pushNotifications;
}