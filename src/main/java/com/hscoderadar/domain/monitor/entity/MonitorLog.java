package com.hscoderadar.domain.monitor.entity;

import com.hscoderadar.domain.user.entity.User;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "monitor_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class MonitorLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "api_endpoint", nullable = false, length = 200)
    private String apiEndpoint;

    @Column(name = "claude_model", nullable = false, length = 100)
    private String claudeModel;

    @Column(name = "input_tokens", nullable = false)
    private int inputTokens = 0;

    @Column(name = "output_tokens", nullable = false)
    private int outputTokens = 0;

    @Column(name = "total_cost_usd", nullable = false, precision = 10, scale = 6)
    private BigDecimal totalCostUsd = BigDecimal.ZERO;

    @Column(name = "response_time_ms", nullable = false)
    private int responseTimeMs = 0;
    
    @Column(nullable = false)
    private boolean success = true;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public MonitorLog(User user, String apiEndpoint, String claudeModel, int inputTokens, int outputTokens, BigDecimal totalCostUsd, int responseTimeMs, boolean success, String errorMessage) {
        this.user = user;
        this.apiEndpoint = apiEndpoint;
        this.claudeModel = claudeModel;
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
        this.totalCostUsd = totalCostUsd;
        this.responseTimeMs = responseTimeMs;
        this.success = success;
        this.errorMessage = errorMessage;
    }
}