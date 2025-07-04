package com.hscoderadar.domain.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PasswordResetSendCodeRequest(
        @NotBlank @Email String email,
        @NotBlank String method,
        String name,
        String phoneNumber) {
}