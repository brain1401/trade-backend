package com.hscoderadar.domain.user.repository;

import com.hscoderadar.domain.user.entity.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * 사용자 엔티티 Repository
 *
 * <p>v6.1 JWT 세부화 정책과 회원 전용 채팅 시스템을 지원하는 사용자 정보 조회 및 관리 기능 제공
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

  /** 이메일로 사용자 조회 */
  Optional<User> findByEmail(String email);

  /** 이메일 존재 여부 확인 */
  boolean existsByEmail(String email);

  /** 휴대폰 번호로 사용자 조회 */
  Optional<User> findByPhoneNumber(String phoneNumber);

  /** 휴대폰 인증 완료된 사용자들 조회 */
  List<User> findByPhoneVerifiedTrue();

  // 🆕 v6.1: JWT 세부화 정책 지원 메서드

  /** 리프레시 토큰으로 사용자 조회 */
  Optional<User> findByRefreshToken(String refreshToken);

  /** 유효한 리프레시 토큰을 가진 사용자 조회 */
  @Query(
      "SELECT u FROM User u WHERE u.refreshToken = :refreshToken "
          + "AND u.refreshTokenExpiresAt > :currentTime")
  Optional<User> findByValidRefreshToken(
      @Param("refreshToken") String refreshToken, @Param("currentTime") LocalDateTime currentTime);

  /** Remember Me가 활성화된 사용자 수 조회 */
  long countByRememberMeEnabledTrue();

  /** 만료된 리프레시 토큰을 가진 사용자들 조회 */
  @Query(
      "SELECT u FROM User u WHERE u.refreshToken IS NOT NULL "
          + "AND u.refreshTokenExpiresAt < :currentTime")
  List<User> findUsersWithExpiredRefreshTokens(@Param("currentTime") LocalDateTime currentTime);

  /** 만료된 리프레시 토큰 정리 (배치 작업용) */
  @Modifying
  @Query(
      "UPDATE User u SET u.refreshToken = NULL, u.refreshTokenExpiresAt = NULL "
          + "WHERE u.refreshTokenExpiresAt < :currentTime")
  int cleanupExpiredRefreshTokens(@Param("currentTime") LocalDateTime currentTime);

  // 활성 사용자 조회 메서드

  /** 최근 토큰 갱신 기준 활성 사용자 조회 */
  @Query("SELECT u FROM User u WHERE u.lastTokenRefresh >= :since")
  List<User> findActiveUsersSince(@Param("since") LocalDateTime since);

  /** 특정 기간 내 가입한 사용자 조회 */
  @Query("SELECT u FROM User u WHERE u.createdAt BETWEEN :startDate AND :endDate")
  List<User> findUsersCreatedBetween(
      @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

  // 알림 관련 조회 메서드

  /** 휴대폰 인증이 완료되고 SMS 알림이 활성화된 사용자 조회 */
  @Query(
      "SELECT u FROM User u "
          + "JOIN u.userSettings us "
          + "WHERE u.phoneVerified = true AND us.smsNotificationEnabled = true")
  List<User> findUsersWithEnabledSmsNotification();

  /** 이메일 알림이 활성화된 사용자 조회 */
  @Query(
      "SELECT u FROM User u "
          + "JOIN u.userSettings us "
          + "WHERE us.emailNotificationEnabled = true")
  List<User> findUsersWithEnabledEmailNotification();

  // 관리자용 통계 메서드

  /** OAuth 전용 사용자 수 조회 */
  long countByPasswordHashIsNull();

  /** 자체 가입 사용자 수 조회 */
  long countByPasswordHashIsNotNull();

  /** 휴대폰 인증 완료 사용자 수 조회 */
  long countByPhoneVerifiedTrue();

  /** 특정 기간 동안 토큰을 갱신한 활성 사용자 수 조회 */
  @Query("SELECT COUNT(DISTINCT u) FROM User u " + "WHERE u.lastTokenRefresh >= :since")
  long countActiveUsersSince(@Param("since") LocalDateTime since);
}
