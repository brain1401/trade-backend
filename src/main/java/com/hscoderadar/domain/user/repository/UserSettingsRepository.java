package com.hscoderadar.domain.user.repository;

import com.hscoderadar.domain.user.entity.User;
import com.hscoderadar.domain.user.entity.UserSettings;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * 사용자 설정 정보 Repository
 *
 * <p>SMS/이메일 통합 알림 설정 및 사용자별 선호도 관리를 위한 사용자 설정 정보 조회 및 관리 기능 제공
 */
@Repository
public interface UserSettingsRepository extends JpaRepository<UserSettings, Long> {

  /** 사용자로 설정 정보 조회 */
  Optional<UserSettings> findByUser(User user);

  /** 사용자 ID로 설정 정보 조회 */
  Optional<UserSettings> findByUserId(Long userId);

  /** 사용자별 설정 존재 여부 확인 */
  boolean existsByUser(User user);

  /** 사용자 ID별 설정 존재 여부 확인 */
  boolean existsByUserId(Long userId);

  // 알림 설정별 조회 메서드

  /** SMS 알림이 활성화된 사용자 설정 목록 조회 */
  List<UserSettings> findBySmsNotificationEnabledTrue();

  /** 이메일 알림이 활성화된 사용자 설정 목록 조회 */
  List<UserSettings> findByEmailNotificationEnabledTrue();

  /** SMS와 이메일 알림이 모두 활성화된 사용자 설정 목록 조회 */
  List<UserSettings> findBySmsNotificationEnabledTrueAndEmailNotificationEnabledTrue();

  /** 특정 알림 주기를 사용하는 사용자 설정 목록 조회 */
  List<UserSettings> findByNotificationFrequency(String frequency);

  /** 일일 알림을 사용하는 사용자 설정 목록 조회 */
  default List<UserSettings> findDailyNotificationUsers() {
    return findByNotificationFrequency("DAILY");
  }

  /** 주간 알림을 사용하는 사용자 설정 목록 조회 */
  default List<UserSettings> findWeeklyNotificationUsers() {
    return findByNotificationFrequency("WEEKLY");
  }

  /** 특정 시간에 알림을 받는 사용자 설정 목록 조회 */
  List<UserSettings> findByNotificationTime(LocalTime time);

  /** 특정 시간 범위에 알림을 받는 사용자 설정 목록 조회 */
  @Query(
      "SELECT us FROM UserSettings us "
          + "WHERE us.notificationTime BETWEEN :startTime AND :endTime")
  List<UserSettings> findByNotificationTimeBetween(
      @Param("startTime") LocalTime startTime, @Param("endTime") LocalTime endTime);

  // 통합 알림 조회 메서드

  /** 알림이 활성화된 사용자 설정 목록 조회 (SMS 또는 이메일 중 하나라도 활성화) */
  @Query(
      "SELECT us FROM UserSettings us "
          + "WHERE us.smsNotificationEnabled = true OR us.emailNotificationEnabled = true")
  List<UserSettings> findWithNotificationEnabled();

  /** 알림이 비활성화된 사용자 설정 목록 조회 (SMS와 이메일 모두 비활성화) */
  @Query(
      "SELECT us FROM UserSettings us "
          + "WHERE us.smsNotificationEnabled = false AND us.emailNotificationEnabled = false")
  List<UserSettings> findWithNotificationDisabled();

  /** 특정 주기와 시간에 알림을 받는 사용자 설정 목록 조회 */
  @Query(
      "SELECT us FROM UserSettings us "
          + "WHERE us.notificationFrequency = :frequency "
          + "AND us.notificationTime = :time "
          + "AND (us.smsNotificationEnabled = true OR us.emailNotificationEnabled = true)")
  List<UserSettings> findForScheduledNotification(
      @Param("frequency") String frequency, @Param("time") LocalTime time);

  // 통계 조회 메서드

  /** SMS 알림 활성화 사용자 수 조회 */
  long countBySmsNotificationEnabledTrue();

  /** 이메일 알림 활성화 사용자 수 조회 */
  long countByEmailNotificationEnabledTrue();

  /** 특정 알림 주기 사용자 수 조회 */
  long countByNotificationFrequency(String frequency);

  /** 알림이 활성화된 사용자 수 조회 */
  @Query(
      "SELECT COUNT(us) FROM UserSettings us "
          + "WHERE us.smsNotificationEnabled = true OR us.emailNotificationEnabled = true")
  long countWithNotificationEnabled();

  /** 알림이 비활성화된 사용자 수 조회 */
  @Query(
      "SELECT COUNT(us) FROM UserSettings us "
          + "WHERE us.smsNotificationEnabled = false AND us.emailNotificationEnabled = false")
  long countWithNotificationDisabled();

  // 관리자용 조회 메서드

  /** 알림 시간대별 사용자 분포 조회 */
  @Query(
      "SELECT us.notificationTime, COUNT(us) FROM UserSettings us "
          + "WHERE us.smsNotificationEnabled = true OR us.emailNotificationEnabled = true "
          + "GROUP BY us.notificationTime "
          + "ORDER BY us.notificationTime")
  List<Object[]> getNotificationTimeDistribution();

  /** 알림 주기별 사용자 분포 조회 */
  @Query(
      "SELECT us.notificationFrequency, COUNT(us) FROM UserSettings us "
          + "WHERE us.smsNotificationEnabled = true OR us.emailNotificationEnabled = true "
          + "GROUP BY us.notificationFrequency")
  List<Object[]> getNotificationFrequencyDistribution();

  /** 알림 유형별 사용자 분포 조회 */
  @Query(
      "SELECT "
          + "SUM(CASE WHEN us.smsNotificationEnabled = true AND us.emailNotificationEnabled = true THEN 1 ELSE 0 END) AS both, "
          + "SUM(CASE WHEN us.smsNotificationEnabled = true AND us.emailNotificationEnabled = false THEN 1 ELSE 0 END) AS smsOnly, "
          + "SUM(CASE WHEN us.smsNotificationEnabled = false AND us.emailNotificationEnabled = true THEN 1 ELSE 0 END) AS emailOnly, "
          + "SUM(CASE WHEN us.smsNotificationEnabled = false AND us.emailNotificationEnabled = false THEN 1 ELSE 0 END) AS none "
          + "FROM UserSettings us")
  Object getNotificationTypeDistribution();
}
