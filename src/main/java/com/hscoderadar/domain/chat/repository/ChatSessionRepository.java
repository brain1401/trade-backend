package com.hscoderadar.domain.chat.repository;

import com.hscoderadar.domain.chat.entity.ChatSession;
import com.hscoderadar.domain.user.entity.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, UUID> {

  // 사용자의 채팅 세션 목록을 최신순으로 페이징하여 조회
  Page<ChatSession> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

  // UUID로 채팅 세션 조회
  Optional<ChatSession> findBySessionUuid(UUID sessionUuid);
}