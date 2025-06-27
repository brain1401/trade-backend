package com.hscoderadar.domain.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * v4.0 통합 채팅 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRequest {

  @NotBlank(message = "메시지가 비어있습니다")
  @Size(min = 2, max = 2000, message = "메시지는 2자 이상 2000자 이하여야 합니다")
  private String message;
}