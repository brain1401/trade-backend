package com.hscoderadar.domain.sms.repository;

import com.hscoderadar.domain.sms.entity.SmsVerificationSession;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * v4.1 SMS 인증 세션 Redis Repository
 * Redis TTL을 활용한 자동 만료 처리
 */
@Repository
public interface SmsVerificationSessionRepository extends CrudRepository<SmsVerificationSession, String> {

  /**
   * verificationId로 인증 세션 조회
   * TTL 만료시 자동으로 null 반환
   */
  Optional<SmsVerificationSession> findByVerificationId(String verificationId);

  /**
   * 사용자 ID로 모든 활성 인증 세션 조회
   */
  List<SmsVerificationSession> findByUserId(Long userId);

  /**
   * 사용자와 휴대폰 번호로 활성 인증 세션 조회
   */
  List<SmsVerificationSession> findByUserIdAndPhoneNumber(Long userId, String phoneNumber);

  /**
   * 인증 완료된 세션만 조회
   */
  List<SmsVerificationSession> findByUserIdAndIsVerified(Long userId, Boolean isVerified);
}