package com.hscoderadar.domain.sms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * v4.0 SMS 인증 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SmsVerificationRequest {

  @NotBlank(message = "휴대폰 번호가 비어있습니다")
  @Pattern(regexp = "^(010-?\\d{4}-?\\d{4}|01[016789]-?\\d{3,4}-?\\d{4})$", message = "휴대폰 번호 형식이 올바르지 않습니다")
  private String phoneNumber;
}