package com.hscoderadar.domain.sms.controller;

import com.hscoderadar.common.exception.AuthException;
import com.hscoderadar.common.exception.SmsException;
import com.hscoderadar.common.response.ApiResponseMessage;
import com.hscoderadar.config.oauth.PrincipalDetails;
import com.hscoderadar.domain.auth.service.AuthService;
import com.hscoderadar.domain.sms.service.SmsService;
import com.hscoderadar.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/sms")
@RequiredArgsConstructor
public class SmsController {

    private final SmsService smsService;
    private final AuthService authService;

    @PostMapping("/send-verification")
    @ApiResponseMessage("인증 코드가 발송되었습니다.")
    public ResponseEntity<String> sendVerificationCode(@RequestBody Map<String, String> payload) {
        String phoneNumber = payload.get("phoneNumber");
        log.info("인증 코드 발송 요청: phoneNumber={}", phoneNumber);
        smsService.sendVerificationCode(phoneNumber);
        return ResponseEntity.ok("Verification code sent.");
    }

    @PostMapping("/verify")
    @ApiResponseMessage("휴대폰 인증이 완료되었습니다.")
    public ResponseEntity<String> verifyCode(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @RequestBody Map<String, String> payload) {

        if (principalDetails == null) {
            throw AuthException.accessDenied();
        }

        String phoneNumber = payload.get("phoneNumber");
        String code = payload.get("code");

        boolean isVerified = smsService.verifyCode(phoneNumber, code);

        if (isVerified) {
            User user = principalDetails.getUser();
            authService.completePhoneVerification(user.getId(), phoneNumber);
            log.info("휴대폰 인증 성공: userId={}, phoneNumber={}", user.getId(), phoneNumber);
            return ResponseEntity.ok("Phone number verified successfully.");
        } else {
            log.warn("휴대폰 인증 실패: userId={}", principalDetails.getUser().getId());
            throw SmsException.invalidVerificationCode();
        }
    }
}