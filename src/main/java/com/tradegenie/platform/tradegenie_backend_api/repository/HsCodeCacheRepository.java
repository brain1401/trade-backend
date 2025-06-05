package com.tradegenie.platform.tradegenie_backend_api.repository;

import com.tradegenie.platform.tradegenie_backend_api.entity.HsCodeCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface HsCodeCacheRepository extends JpaRepository<HsCodeCache, Long> {

  /**
   * HS Code로 캐시 조회
   */
  @Query("SELECT h FROM HsCodeCache h WHERE h.hsCode = :hsCode")
  Optional<HsCodeCache> findByHsCode(@Param("hsCode") String hsCode);

  /**
   * 특정 시간 이후 업데이트된 캐시 조회
   */
  @Query("SELECT h FROM HsCodeCache h WHERE h.lastUpdated >= :cutoffTime " +
      "ORDER BY h.lastUpdated DESC")
  java.util.List<HsCodeCache> findRecentlyUpdated(@Param("cutoffTime") LocalDateTime cutoffTime);

  /**
   * HS Code 존재 여부 확인
   */
  @Query("SELECT COUNT(h) > 0 FROM HsCodeCache h WHERE h.hsCode = :hsCode")
  boolean existsByHsCode(@Param("hsCode") String hsCode);

  /**
   * 제품명 업데이트
   */
  @Modifying
  @Query("UPDATE HsCodeCache h SET h.productName = :productName, h.lastUpdated = CURRENT_TIMESTAMP WHERE h.hsCode = :hsCode")
  void updateProductName(@Param("hsCode") String hsCode, @Param("productName") String productName);

  /**
   * 설명 업데이트
   */
  @Modifying
  @Query("UPDATE HsCodeCache h SET h.description = :description, h.lastUpdated = CURRENT_TIMESTAMP WHERE h.hsCode = :hsCode")
  void updateDescription(@Param("hsCode") String hsCode, @Param("description") String description);

  /**
   * 무역 통계 데이터 업데이트
   */
  @Modifying
  @Query("UPDATE HsCodeCache h SET h.tradeStats = :tradeStats, h.lastUpdated = CURRENT_TIMESTAMP WHERE h.hsCode = :hsCode")
  void updateTradeStats(@Param("hsCode") String hsCode, @Param("tradeStats") Map<String, Object> tradeStats);

  /**
   * Comtrade 데이터 업데이트
   */
  @Modifying
  @Query("UPDATE HsCodeCache h SET h.comtradeData = :comtradeData, h.lastUpdated = CURRENT_TIMESTAMP WHERE h.hsCode = :hsCode")
  void updateComtradeData(@Param("hsCode") String hsCode, @Param("comtradeData") Map<String, Object> comtradeData);

  /**
   * 제품명으로 검색 (부분 일치)
   */
  @Query("SELECT h FROM HsCodeCache h WHERE h.productName LIKE %:productName% ORDER BY h.lastUpdated DESC")
  List<HsCodeCache> findByProductNameContaining(@Param("productName") String productName);

  /**
   * 설명으로 검색 (부분 일치)
   */
  @Query("SELECT h FROM HsCodeCache h WHERE h.description LIKE %:description% ORDER BY h.lastUpdated DESC")
  List<HsCodeCache> findByDescriptionContaining(@Param("description") String description);

  /**
   * 오래된 캐시 조회 (업데이트가 필요한 항목)
   */
  @Query("SELECT h FROM HsCodeCache h WHERE h.lastUpdated < :cutoffTime ORDER BY h.lastUpdated ASC")
  List<HsCodeCache> findStaleCache(@Param("cutoffTime") LocalDateTime cutoffTime);

  /**
   * HS Code 패턴으로 검색
   */
  @Query("SELECT h FROM HsCodeCache h WHERE h.hsCode LIKE :hsCodePattern ORDER BY h.hsCode ASC")
  List<HsCodeCache> findByHsCodePattern(@Param("hsCodePattern") String hsCodePattern);

  /**
   * 최근 업데이트된 캐시 수 조회
   */
  @Query("SELECT COUNT(h) FROM HsCodeCache h WHERE h.lastUpdated >= :cutoffTime")
  long countRecentlyUpdatedCache(@Param("cutoffTime") LocalDateTime cutoffTime);

  /**
   * 무역 통계가 있는 캐시 조회
   */
  @Query("SELECT h FROM HsCodeCache h WHERE h.tradeStats IS NOT NULL ORDER BY h.lastUpdated DESC")
  List<HsCodeCache> findCacheWithTradeStats();

  /**
   * Comtrade 데이터가 있는 캐시 조회
   */
  @Query("SELECT h FROM HsCodeCache h WHERE h.comtradeData IS NOT NULL ORDER BY h.lastUpdated DESC")
  List<HsCodeCache> findCacheWithComtradeData();

  /**
   * 특정 HS Code 삭제
   */
  @Modifying
  @Query("DELETE FROM HsCodeCache h WHERE h.hsCode = :hsCode")
  void deleteByHsCode(@Param("hsCode") String hsCode);

  /**
   * 오래된 캐시 일괄 삭제
   */
  @Modifying
  @Query("DELETE FROM HsCodeCache h WHERE h.lastUpdated < :cutoffTime")
  void deleteStaleCache(@Param("cutoffTime") LocalDateTime cutoffTime);

  /**
   * 전체 캐시 데이터 업데이트 시간 갱신
   */
  @Modifying
  @Query("UPDATE HsCodeCache h SET h.lastUpdated = CURRENT_TIMESTAMP WHERE h.id = :id")
  void refreshLastUpdated(@Param("id") Long id);

  /**
   * 캐시 통계 - 총 캐시 수
   */
  @Query("SELECT COUNT(h) FROM HsCodeCache h")
  long countTotalCache();
}