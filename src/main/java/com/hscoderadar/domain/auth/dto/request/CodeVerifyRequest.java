package com.hscoderadar.domain.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CodeVerifyRequest(
        @NotBlank @Email String email,
        @NotBlank String code,
        @NotBlank String method // "phone" 또는 "email"
) {
}
