package com.hscoderadar.domain.chat.repository;

import com.hscoderadar.domain.chat.entity.ChatMessage;
import com.hscoderadar.domain.chat.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 채팅 메시지 Repository
 */
@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

  /**
   * 특정 세션의 모든 메시지 조회 (시간순)
   */
  List<ChatMessage> findBySessionOrderByTimestamp(ChatSession session);

  /**
   * 특정 세션의 메시지 개수 조회
   */
  long countBySession(ChatSession session);
}