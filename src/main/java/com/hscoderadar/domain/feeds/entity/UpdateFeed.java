package com.hscoderadar.domain.feeds.entity;

import com.hscoderadar.domain.news.entity.News;
import com.hscoderadar.domain.users.entity.User;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "update_feeds")
@Data
@EqualsAndHashCode(callSuper = false)
public class UpdateFeed {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "feed_type", nullable = false)
    private FeedType feedType;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type")
    private TargetType targetType;

    @Column(name = "target_value", length = 50)
    private String targetValue;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "source_url", length = 1000)
    private String sourceUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "news_id")
    private News news;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum FeedType {
        HS_CODE_CHANGE, CARGO_CHANGE, NEWS_UPDATE
    }

    public enum TargetType {
        HS_CODE, CARGO
    }
}