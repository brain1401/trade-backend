package com.hscoderadar.domain.notifications.entity;

import com.hscoderadar.domain.bookmarks.entity.Bookmark;
import com.hscoderadar.domain.users.entity.User;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "push_notifications")
@Data
@EqualsAndHashCode(callSuper = false)
public class PushNotification {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "bookmark_id", nullable = false)
  private Bookmark bookmark;

  @Column(name = "title", nullable = false)
  private String title;

  @Column(name = "content", nullable = false, columnDefinition = "TEXT")
  private String content;

  @Enumerated(EnumType.STRING)
  @Column(name = "change_type", nullable = false)
  private ChangeType changeType;

  @Column(name = "is_sent", nullable = false)
  private Boolean isSent = false;

  @Column(name = "is_read", nullable = false)
  private Boolean isRead = false;

  @Column(name = "sent_at")
  private LocalDateTime sentAt;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  public enum ChangeType {
    REGULATION, TARIFF, NEWS
  }
}