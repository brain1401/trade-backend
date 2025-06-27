package com.hscoderadar.domain.chat.repository;

import com.hscoderadar.domain.chat.entity.ChatJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * v4.0 채팅 작업 관리 Repository
 */
@Repository
public interface ChatJobRepository extends JpaRepository<ChatJob, Long> {

  /**
   * jobId로 채팅 작업 조회
   */
  Optional<ChatJob> findByJobId(String jobId);

  /**
   * sessionToken으로 채팅 작업 조회
   */
  Optional<ChatJob> findBySessionToken(String sessionToken);

  /**
   * sessionToken으로 채팅 작업 조회 후 토큰 사용 처리 (일회용)
   */
  @Modifying
  @Query("UPDATE ChatJob c SET c.tokenUsedAt = :usedAt WHERE c.sessionToken = :sessionToken AND c.tokenUsedAt IS NULL")
  int markTokenAsUsed(@Param("sessionToken") String sessionToken, @Param("usedAt") LocalDateTime usedAt);

  /**
   * 만료된 토큰을 가진 작업들 삭제 (정리 작업용)
   */
  @Modifying
  @Query("DELETE FROM ChatJob c WHERE c.tokenExpiresAt < :now AND c.processingStatus != 'PROCESSING'")
  int deleteExpiredJobs(@Param("now") LocalDateTime now);

  /**
   * 처리 상태별 작업 개수 조회
   */
  long countByProcessingStatus(ChatJob.ProcessingStatus status);
}