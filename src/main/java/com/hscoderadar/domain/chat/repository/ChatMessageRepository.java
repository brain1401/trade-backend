package com.hscoderadar.domain.chat.repository;

import com.hscoderadar.domain.chat.entity.ChatMessage;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

  // 특정 세션 UUID에 속한 모든 메시지를 생성 시간 오름차순으로 조회
  List<ChatMessage> findBySessionUuidOrderByCreatedAtAsc(UUID sessionUuid);
}