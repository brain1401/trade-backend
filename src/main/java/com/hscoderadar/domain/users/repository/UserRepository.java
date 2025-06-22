package com.hscoderadar.domain.users.repository;

import com.hscoderadar.domain.users.entity.User;

import org.springframework.data.jpa.repository.EntityGraph;
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
    @EntityGraph(attributePaths = {"userSettings", "snsAccounts"})
    Optional<User> findByEmail(@Param("email") String email);

    /**
     * 이메일 존재 여부 확인
     */
    boolean existsByEmail(@Param("email") String email);

    /**
     * 비밀번호 업데이트
     */
    @Modifying
    @Query("UPDATE User u SET u.passwordHash = :passwordHash, u.updatedAt = CURRENT_TIMESTAMP WHERE u.id = :userId")
    void updatePassword(@Param("userId") Long userId, @Param("passwordHash") String passwordHash);

    /**
     * 사용자 이름 업데이트
     */
    @Modifying
    @Query("UPDATE User u SET u.name = :name, u.updatedAt = CURRENT_TIMESTAMP WHERE u.id = :userId")
    void updateName(@Param("userId") Long userId, @Param("name") String name);

    /**
     * 이름으로 사용자 검색
     */
    List<User> findByNameContainingOrderByNameAsc(@Param("name") String name);

    /**
     * 특정 기간 가입 사용자 조회
     */
    @Query("SELECT u FROM User u WHERE u.createdAt BETWEEN :startDate AND :endDate ORDER BY u.createdAt DESC")
    List<User> findUsersCreatedBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * 사용자별 활성 북마크 수 조회
     */
    @Query("SELECT COUNT(b) FROM Bookmark b WHERE b.user.id = :userId AND b.monitoringEnabled = true")
    long countActiveBookmarksByUserId(@Param("userId") Long userId);

    /**
     * 리프레시 토큰으로 사용자 조회
     */
    Optional<User> findByRefreshToken(String refreshToken);
}