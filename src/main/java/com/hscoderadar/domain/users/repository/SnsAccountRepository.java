package com.hscoderadar.domain.users.repository;

import com.hscoderadar.domain.users.entity.SnsAccount;
import com.hscoderadar.domain.users.entity.SnsAccount.SnsProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SnsAccountRepository extends JpaRepository<SnsAccount, Long> {
   
    /**
     *  SNS 아이디 조회
     */
    Optional<SnsAccount> findByProviderAndProviderId(SnsProvider provider, String providerId);
}