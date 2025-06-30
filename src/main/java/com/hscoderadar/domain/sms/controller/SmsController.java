package com.hscoderadar.domain.sms.controller;

import com.hscoderadar.common.exception.AuthException;
import com.hscoderadar.common.response.ApiResponseMessage;
import com.hscoderadar.config.oauth.PrincipalDetails;
import com.hscoderadar.domain.auth.service.AuthService;
import com.hscoderadar.domain.sms.dto.request.SmsSendRequest;
import com.hscoderadar.domain.sms.dto.request.SmsVerifyRequest;
import com.hscoderadar.domain.sms.service.SmsService;
import com.hscoderadar.domain.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/sms")
@RequiredArgsConstructor
public class SmsController {

  private final SmsService smsService;
  private final AuthService authService;

  /**
   * 인증번호 발송
   *
   * @param request "to" 키에 수신자 전화번호 포함
   * @return 발송 성공 메시지
   */
  @PostMapping("/send-verification")
  @ApiResponseMessage("인증 코드가 발송되었습니다.")
  public String sendVerificationCode(@Valid @RequestBody SmsSendRequest request) {
    log.info("인증 코드 발송 요청: phoneNumber={}", request.to());
    smsService.sendVerificationCode(request.to());
    return "인증 코드가 발송되었습니다.";
  }

  /**
   * 인증번호 확인
   *
   * @param request "to", "code" 키에 전화번호와 인증번호 포함
   * @return 인증 성공 여부
   */
  @PostMapping("/verify")
  @ApiResponseMessage("휴대폰 인증이 완료되었습니다.")
  public String verifyCode(
      @AuthenticationPrincipal PrincipalDetails principalDetails,
      @Valid @RequestBody SmsVerifyRequest request) {

    if (principalDetails == null) {
      throw AuthException.accessDenied();
    }

    boolean isVerified = smsService.verifyCode(request.to(), request.code());

    if (isVerified) {
      User user = principalDetails.getUser();
      authService.completePhoneVerification(user.getId(), request.to());
      log.info("휴대폰 인증 성공: userId={}, phoneNumber={}", user.getId(), request.to());
      return "휴대폰 인증이 성공적으로 완료되었습니다.";
    } else {
      log.warn("휴대폰 인증 실패: userId={}", principalDetails.getUser().getId());
      // SmsException.invalidVerificationCode(); 를 호출해야 하지만,
      // GlobalExceptionHandler가 처리하도록 예외를 던지는 것이 더 적절합니다.
      // 하지만 이전 코드에서는 응답을 직접 반환했으므로 일단 이전 방식을 따르겠습니다.
      // 이 부분은 추후 예외 처리 정책에 따라 개선될 수 있습니다.
      return "휴대폰 인증에 실패했습니다."; // 또는 예외를 던집니다.
    }
  }
}