package com.hscoderadar.domain.news.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "news")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class News {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String title;
    
    @Column(name = "source_url", nullable = false, length = 1000)
    private String sourceUrl;
    
    @Column(name = "source_name", nullable = false, length = 200)
    private String sourceName;
    
    @Column(name = "published_at", nullable = false)
    private LocalDateTime publishedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public News(String title, String sourceUrl, String sourceName, LocalDateTime publishedAt) {
        this.title = title;
        this.sourceUrl = sourceUrl;
        this.sourceName = sourceName;
        this.publishedAt = publishedAt;
    }
}