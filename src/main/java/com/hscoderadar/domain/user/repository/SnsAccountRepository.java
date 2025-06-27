package com.hscoderadar.domain.user.repository;

import com.hscoderadar.domain.user.entity.SnsAccount;
import com.hscoderadar.domain.user.entity.User;
import com.hscoderadar.domain.user.entity.enums.SnsProvider;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * SNS 계정 연동 정보 Repository
 *
 * <p>OAuth 소셜 로그인 및 계정 연동 관리를 위한 SNS 계정 정보 조회 및 관리 기능 제공
 */
@Repository
public interface SnsAccountRepository extends JpaRepository<SnsAccount, Long> {

  /** 사용자별 연동된 SNS 계정 목록 조회 */
  List<SnsAccount> findByUser(User user);

  /** 사용자 ID별 연동된 SNS 계정 목록 조회 */
  List<SnsAccount> findByUserId(Long userId);

  /** 특정 제공업체와 제공업체 ID로 SNS 계정 조회 */
  Optional<SnsAccount> findByProviderAndProviderId(SnsProvider provider, String providerId);

  /** 특정 제공업체와 제공업체 이메일로 SNS 계정 조회 */
  Optional<SnsAccount> findByProviderAndProviderEmail(SnsProvider provider, String providerEmail);

  /** 사용자의 특정 제공업체 연동 계정 조회 */
  Optional<SnsAccount> findByUserAndProvider(User user, SnsProvider provider);

  /** 사용자 ID와 제공업체로 연동 계정 조회 */
  Optional<SnsAccount> findByUserIdAndProvider(Long userId, SnsProvider provider);

  /** 특정 제공업체 연동 계정 존재 여부 확인 */
  boolean existsByProviderAndProviderId(SnsProvider provider, String providerId);

  /** 사용자의 특정 제공업체 연동 여부 확인 */
  boolean existsByUserAndProvider(User user, SnsProvider provider);

  /** 사용자 ID의 특정 제공업체 연동 여부 확인 */
  boolean existsByUserIdAndProvider(Long userId, SnsProvider provider);

  // 제공업체별 통계 조회 메서드

  /** 제공업체별 연동 계정 수 조회 */
  long countByProvider(SnsProvider provider);

  /** 구글 연동 계정 수 조회 */
  default long countGoogleAccounts() {
    return countByProvider(SnsProvider.GOOGLE);
  }

  /** 카카오 연동 계정 수 조회 */
  default long countKakaoAccounts() {
    return countByProvider(SnsProvider.KAKAO);
  }

  /** 네이버 연동 계정 수 조회 */
  default long countNaverAccounts() {
    return countByProvider(SnsProvider.NAVER);
  }

  /** 사용자별 연동 계정 수 조회 */
  long countByUser(User user);

  /** 사용자 ID별 연동 계정 수 조회 */
  long countByUserId(Long userId);

  // 관리자용 조회 메서드

  /** 제공업체별 연동 계정 목록 조회 (페이징 없음) */
  List<SnsAccount> findByProvider(SnsProvider provider);

  /** 제공업체 이메일로 연동 계정 목록 조회 */
  List<SnsAccount> findByProviderEmail(String providerEmail);

  /** 여러 제공업체에 연동된 사용자 조회 */
  @Query(
      "SELECT sa.user FROM SnsAccount sa " + "GROUP BY sa.user " + "HAVING COUNT(sa.provider) > 1")
  List<User> findUsersWithMultipleProviders();

  /** 특정 제공업체만 연동된 사용자 조회 */
  @Query(
      "SELECT DISTINCT sa.user FROM SnsAccount sa "
          + "WHERE sa.provider = :provider "
          + "AND sa.user NOT IN ("
          + "    SELECT sa2.user FROM SnsAccount sa2 "
          + "    WHERE sa2.provider != :provider"
          + ")")
  List<User> findUsersWithOnlyProvider(@Param("provider") SnsProvider provider);

  /** 모든 주요 제공업체에 연동된 사용자 조회 (Google, Kakao, Naver 모두) */
  @Query(
      "SELECT sa.user FROM SnsAccount sa "
          + "WHERE sa.provider IN :providers "
          + "GROUP BY sa.user "
          + "HAVING COUNT(DISTINCT sa.provider) = :providerCount")
  List<User> findUsersWithAllProviders(
      @Param("providers") List<SnsProvider> providers, @Param("providerCount") long providerCount);
}
