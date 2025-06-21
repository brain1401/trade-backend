package com.hscoderadar.domain.system.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "system_logs")
@Data
@EqualsAndHashCode(callSuper = false)
public class SystemLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "log_type", nullable = false)
    private LogType logType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSON")
    private String details; // JSON 문자열을 저장

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LogStatus status;

    @Column(name = "execution_time_ms")
    private Integer executionTimeMs;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum LogType {
        MONITORING_BATCH, NEWS_CLEANUP, API_ERROR
    }

    public enum LogStatus {
        SUCCESS, FAILURE, WARNING
    }
}