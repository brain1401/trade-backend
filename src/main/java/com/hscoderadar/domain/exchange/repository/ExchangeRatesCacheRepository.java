package com.hscoderadar.domain.exchange.repository;

import com.hscoderadar.domain.exchange.entity.ExchangeRatesCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 실시간 환율 캐시 정보를 위한 Repository
 *
 * @author HsCodeRadar Team
 * @since 1.0.0
 */
@Repository
public interface ExchangeRatesCacheRepository extends JpaRepository<ExchangeRatesCache, Long> {

    /**
     * 활성화 상태이고 만료되지 않은 최신 환율 정보를 통화 코드로 조회
     * @return 최신 환율 정보 Optional 객체
     */
    @Query("SELECT e FROM ExchangeRatesCache e " +
           "WHERE e.currencyCode = :currencyCode " +
           "AND e.isActive = true " +
           "AND e.expiresAt > :now " +
           "ORDER BY e.fetchedAt DESC")
    Optional<ExchangeRatesCache> findTopByCurrencyCodeAndIsActiveTrueAndExpiresAtAfterOrderByFetchedAtDesc(
        String currencyCode,
        LocalDateTime now
    );

    /**
     * 활성화 상태이고 만료되지 않은 모든 최신 환율 정보를 조회
     * @return 환율 정보 리스트
     */
     @Query("SELECT e FROM ExchangeRatesCache e " +
            "WHERE e.id IN (" +
            "    SELECT MAX(sub.id) FROM ExchangeRatesCache sub " +
            "    WHERE sub.isActive = true AND sub.expiresAt > :now " +
            "    GROUP BY sub.currencyCode" +
            ")")
    List<ExchangeRatesCache> findLatestActiveExchangeRates(LocalDateTime now);
}