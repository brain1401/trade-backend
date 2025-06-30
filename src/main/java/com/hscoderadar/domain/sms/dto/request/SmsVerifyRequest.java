package com.hscoderadar.domain.sms.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * SMS 인증번호 확인 요청 DTO
 * 
 * @param to   수신자 전화번호
 * @param code 인증번호
 */
@Schema(description = "SMS 인증번호 확인 요청")
public record SmsVerifyRequest(
    @Schema(description = "수신자 전화번호", example = "01012345678") @NotBlank(message = "수신자 전화번호는 필수입니다.") @Pattern(regexp = "^010[0-9]{8}$", message = "유효한 전화번호 형식이 아닙니다.") String to,

    @Schema(description = "인증번호", example = "123456") @NotBlank(message = "인증번호는 필수입니다.") @Size(min = 6, max = 6, message = "인증번호는 6자리여야 합니다.") String code) {
}