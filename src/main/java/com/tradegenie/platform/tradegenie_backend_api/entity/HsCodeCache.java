package com.tradegenie.platform.tradegenie_backend_api.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "hscode_cache")
@Data
@EqualsAndHashCode(callSuper = false)
public class HsCodeCache {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "hscode", unique = true, nullable = false)
  private String hsCode;

  @Column(name = "product_name")
  private String productName;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Column(name = "trade_stats", columnDefinition = "JSON")
  @JdbcTypeCode(SqlTypes.JSON)
  private Map<String, Object> tradeStats;

  @Column(name = "comtrade_data", columnDefinition = "JSON")
  @JdbcTypeCode(SqlTypes.JSON)
  private Map<String, Object> comtradeData;

  @UpdateTimestamp
  @Column(name = "last_updated", nullable = false)
  private LocalDateTime lastUpdated;
}