package com.hscoderadar.domain.sms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * v4.0 SMS 인증 확인 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SmsVerifyRequest {

  @NotBlank(message = "인증 세션 ID가 비어있습니다")
  private String verificationId;

  @NotBlank(message = "인증 코드가 비어있습니다")
  @Pattern(regexp = "^\\d{6}$", message = "인증 코드는 6자리 숫자여야 합니다")
  private String verificationCode;
}