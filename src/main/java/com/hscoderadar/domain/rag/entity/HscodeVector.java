package com.hscoderadar.domain.rag.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "hscode_vectors")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class HscodeVector {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20, unique = true)
    private String hscode;

    @Column(name = "product_name", nullable = false, length = 500)
    private String productName;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, columnDefinition = "VECTOR(2048)")
    private String embedding; // 실제로는 pgvector의 VECTOR 타입 사용

    @Column(nullable = false, columnDefinition = "JSONB")
    private String metadata;

    @Column(name = "classification_basis", columnDefinition = "TEXT")
    private String classificationBasis;

    @Column(name = "similar_hscodes", columnDefinition = "JSONB")
    private String similarHscodes;

    @Column(name = "keywords", columnDefinition = "TEXT[]")
    private String[] keywords;

    @Column(name = "web_search_context", columnDefinition = "TEXT")
    private String webSearchContext;
    
    @Column(name = "hscode_differences", columnDefinition = "TEXT")
    private String hscodeDifferences;

    @Column(name = "confidence_score")
    private Float confidenceScore = 0.0f;

    private boolean verified = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public HscodeVector(String hscode, String productName, String description, String embedding, String metadata) {
        this.hscode = hscode;
        this.productName = productName;
        this.description = description;
        this.embedding = embedding;
        this.metadata = metadata;
    }
}