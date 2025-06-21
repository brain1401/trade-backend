package com.hscoderadar.domain.users.repository;

import com.hscoderadar.domain.users.entity.UserSettings;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserSettingsRepository extends JpaRepository<UserSettings, Long> {
    
    /**
     * 유저 세팅 조회
     */
    @EntityGraph(attributePaths = {"user"})
    Optional<UserSettings> findByUserId(Long userId);
    
    /**
     * 푸시 알림 발송 대상 조회
     */
    @EntityGraph(attributePaths = {"user"})
    List<UserSettings> findByPushNotificationEnabledIsTrue();

    /**
     * 이메일 발송 대상 조회
     */
    @EntityGraph(attributePaths = {"user"})
    List<UserSettings> findByEmailNotificationEnabledIsTrue();
}