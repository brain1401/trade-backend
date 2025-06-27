package com.hscoderadar.domain.users.repository;

import com.hscoderadar.domain.users.entity.SnsAccount;
import com.hscoderadar.domain.users.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * SNS 계정 연동 정보 관리를 위한 데이터 액세스 객체
 * 
 * 사용자와 소셜 로그인 제공업체 간의 연동 관계를 관리
 */
@Repository
public interface SnsAccountRepository extends JpaRepository<SnsAccount, Long> {

    /**
     * 제공업체와 제공업체 ID로 SNS 계정 조회
     * 
     * @param provider   SNS 제공업체 (Google, Kakao, Naver)
     * @param providerId 제공업체 내의 사용자 ID
     * @return SNS 계정 정보
     */
    Optional<SnsAccount> findByProviderAndProviderId(SnsAccount.Provider provider, String providerId);

    /**
     * 사용자와 제공업체로 SNS 계정 조회
     * 
     * @param user     사용자
     * @param provider SNS 제공업체
     * @return SNS 계정 정보
     */
    Optional<SnsAccount> findByUserAndProvider(User user, SnsAccount.Provider provider);
}