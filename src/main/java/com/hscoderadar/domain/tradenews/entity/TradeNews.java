package com.hscoderadar.domain.tradenews.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "trade_news_cache")
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

    @Column(name = "source_url", nullable = false, length = 1000)
    private String sourceUrl;
    
    @Column(name = "published_at", nullable = false)
    private LocalDateTime publishedAt;

    @Column(length = 50)
    private String category;
    
    @Column(nullable = false)
    private Integer priority = 1;

    @Column(name = "source_api", nullable = false, length = 100)
    private String sourceApi;

    @Column(name = "fetched_at", nullable = false)
    private LocalDateTime fetchedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @Builder
    public TradeNews(String title, String sourceName, String sourceUrl, LocalDateTime publishedAt, String sourceApi, LocalDateTime expiresAt) {
        this.title = title;
        this.sourceName = sourceName;
        this.sourceUrl = sourceUrl;
        this.publishedAt = publishedAt;
        this.sourceApi = sourceApi;
        this.fetchedAt = LocalDateTime.now();
        this.expiresAt = expiresAt;
    }
}