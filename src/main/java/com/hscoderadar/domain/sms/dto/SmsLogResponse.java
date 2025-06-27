package com.hscoderadar.domain.sms.dto;

import com.hscoderadar.domain.sms.entity.SmsLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * v4.0 SMS 발송 로그 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SmsLogResponse {

  private Long id;
  private SmsLog.MessageType messageType;
  private String phoneNumber; // 마스킹 처리된 번호
  private String content;
  private SmsLog.SmsStatus status;
  private Integer costKrw;
  private LocalDateTime sentAt;
  private LocalDateTime deliveredAt;
  private LocalDateTime createdAt;
}