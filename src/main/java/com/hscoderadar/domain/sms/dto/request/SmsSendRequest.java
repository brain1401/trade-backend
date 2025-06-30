package com.hscoderadar.domain.sms.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * SMS 인증번호 발송 요청 DTO
 * 
 * @param to 수신자 전화번호
 */
@Schema(description = "SMS 인증번호 발송 요청")
public record SmsSendRequest(
    @Schema(description = "수신자 전화번호", example = "01012345678") @NotBlank(message = "수신자 전화번호는 필수입니다.") @Pattern(regexp = "^010[0-9]{8}$", message = "유효한 전화번호 형식이 아닙니다.") String to) {
}