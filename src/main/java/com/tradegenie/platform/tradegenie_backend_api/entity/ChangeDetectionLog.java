package com.tradegenie.platform.tradegenie_backend_api.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "change_detection_log")
@Data
@EqualsAndHashCode(callSuper = false)
public class ChangeDetectionLog {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "bookmark_id", nullable = false)
  private Bookmark bookmark;

  @Enumerated(EnumType.STRING)
  @Column(name = "change_type", nullable = false)
  private ChangeType changeType;

  @Column(name = "change_summary", nullable = false, columnDefinition = "TEXT")
  private String changeSummary;

  @Column(name = "source_urls", columnDefinition = "JSON")
  @JdbcTypeCode(SqlTypes.JSON)
  private List<String> sourceUrls;

  @CreationTimestamp
  @Column(name = "detected_at", nullable = false, updatable = false)
  private LocalDateTime detectedAt;

  public enum ChangeType {
    REGULATION, TARIFF, NEWS
  }
}