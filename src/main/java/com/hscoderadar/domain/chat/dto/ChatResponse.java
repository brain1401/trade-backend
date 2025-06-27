package com.hscoderadar.domain.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * v4.0 통합 채팅 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatResponse {

  private String jobId;
  private String sessionToken;
  private String streamUrl;
  private Integer estimatedTime;
}