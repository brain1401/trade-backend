package com.hscoderadar.domain.tradenews.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "trade_news")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class TradeNews {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 500)
  private String title;

  @Column(columnDefinition = "TEXT")
  private String summary;

  @Column(name = "source_name", nullable = false, length = 200)
  private String sourceName;

  @Column(name = "source_url", length = 1000)
  private String sourceUrl;

  @Column(name = "published_at", nullable = false)
  private LocalDateTime publishedAt;

  @Column(length = 50)
  private String category;

  @Column(nullable = false)
  private Integer priority = 1;

  @Column(name = "fetched_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
  private LocalDateTime fetchedAt;

  @Builder
  public TradeNews(String title, String summary, String sourceName, String sourceUrl, LocalDateTime publishedAt,
      String category, Integer priority) {
    this.title = title;
    this.summary = summary;
    this.sourceName = sourceName;
    this.sourceUrl = sourceUrl;
    this.publishedAt = publishedAt;
    this.category = category;
    this.priority = priority;
    this.fetchedAt = LocalDateTime.now();
  }
}