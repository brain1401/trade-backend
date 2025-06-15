package com.hscoderadar.domain.user.repository;

import com.hscoderadar.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

  /**
   * 이메일로 사용자 조회
   */
  @Query("SELECT u FROM User u WHERE u.email = :email")
  Optional<User> findByEmail(@Param("email") String email);

  /**
   * 이메일 존재 여부 확인
   */
  @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.email = :email")
  boolean existsByEmail(@Param("email") String email);

  /**
   * 활성 사용자 수 조회
   */
  @Query("SELECT COUNT(u) FROM User u WHERE u.pushNotificationEnabled = true")
  long countActiveUsers();

  /**
   * 푸시 알림 설정 업데이트
   */
  @Modifying
  @Query("UPDATE User u SET u.pushNotificationEnabled = :enabled, u.updatedAt = CURRENT_TIMESTAMP WHERE u.id = :userId")
  void updatePushNotificationSetting(@Param("userId") Long userId, @Param("enabled") Boolean enabled);

  /**
   * 비밀번호 업데이트
   */
  @Modifying
  @Query("UPDATE User u SET u.passwordHash = :passwordHash, u.updatedAt = CURRENT_TIMESTAMP WHERE u.id = :userId")
  void updatePassword(@Param("userId") Long userId, @Param("passwordHash") String passwordHash);

  /**
   * 사용자 정보 업데이트
   */
  @Modifying
  @Query("UPDATE User u SET u.name = :name, u.companyName = :companyName, u.updatedAt = CURRENT_TIMESTAMP WHERE u.id = :userId")
  void updateUserInfo(@Param("userId") Long userId, @Param("name") String name,
      @Param("companyName") String companyName);

  /**
   * 회사명으로 사용자 조회
   */
  @Query("SELECT u FROM User u WHERE u.companyName = :companyName ORDER BY u.createdAt DESC")
  List<User> findByCompanyName(@Param("companyName") String companyName);

  /**
   * 이름으로 사용자 검색 (부분 일치)
   */
  @Query("SELECT u FROM User u WHERE u.name LIKE %:name% ORDER BY u.name ASC")
  List<User> findByNameContaining(@Param("name") String name);

  /**
   * 특정 기간 가입 사용자 조회
   */
  @Query("SELECT u FROM User u WHERE u.createdAt >= :startDate AND u.createdAt <= :endDate ORDER BY u.createdAt DESC")
  List<User> findUsersCreatedBetween(@Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate);

  /**
   * 푸시 알림 활성화된 사용자 조회
   */
  @Query("SELECT u FROM User u WHERE u.pushNotificationEnabled = true ORDER BY u.name ASC")
  List<User> findUsersWithPushNotificationEnabled();

  /**
   * 사용자별 북마크 수 조회
   */
  @Query("SELECT COUNT(b) FROM User u JOIN u.bookmarks b WHERE u.id = :userId AND b.isActive = true")
  long countActiveBookmarksByUserId(@Param("userId") Long userId);

  /**
   * 이메일 패턴으로 사용자 검색
   */
  @Query("SELECT u FROM User u WHERE u.email LIKE %:emailPattern% ORDER BY u.email ASC")
  List<User> findByEmailPattern(@Param("emailPattern") String emailPattern);

  /**
   * 최근 활동 사용자 조회 (북마크가 있는 사용자)
   */
  @Query("SELECT DISTINCT u FROM User u JOIN u.bookmarks b WHERE b.isActive = true ORDER BY u.updatedAt DESC")
  List<User> findActiveUsersWithBookmarks();

  /**
   * 일괄 푸시 알림 설정 업데이트
   */
  @Modifying
  @Query("UPDATE User u SET u.pushNotificationEnabled = :enabled, u.updatedAt = CURRENT_TIMESTAMP WHERE u.id IN :userIds")
  void bulkUpdatePushNotificationSetting(@Param("userIds") List<Long> userIds, @Param("enabled") Boolean enabled);

  /**
   * 비활성 사용자 조회 (북마크가 없는 사용자)
   */
  @Query("SELECT u FROM User u WHERE u.id NOT IN (SELECT DISTINCT b.user.id FROM Bookmark b WHERE b.isActive = true) ORDER BY u.createdAt ASC")
  List<User> findInactiveUsers();

  /**
   * 통계 - 일별 가입자 수
   */
  @Query("SELECT COUNT(u) FROM User u WHERE DATE(u.createdAt) = DATE(:date)")
  long countUsersByRegistrationDate(@Param("date") LocalDateTime date);
}