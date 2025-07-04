package com.hscoderadar.domain.chat.repository;

import com.hscoderadar.domain.chat.entity.ChatSession;
import com.hscoderadar.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 채팅 세션 Repository
 */
@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, String> {

  /**
   * 사용자의 모든 채팅 세션 조회 (최신순)
   */
  List<ChatSession> findByUserOrderByLastActivityAtDesc(User user);

  /**
   * 사용자의 최근 활성 세션 조회
   */
  Optional<ChatSession> findFirstByUserOrderByLastActivityAtDesc(User user);

  /**
   * 특정 기간 동안 활동이 없는 세션 조회
   */
  @Query("SELECT cs FROM ChatSession cs WHERE cs.lastActivityAt < :inactiveTime")
  List<ChatSession> findInactiveSessions(@Param("inactiveTime") LocalDateTime inactiveTime);
}