package com.hscoderadar.domain.auth.controller;

import com.hscoderadar.common.exception.AuthException;
import com.hscoderadar.common.exception.RateLimitException;
import com.hscoderadar.common.response.ApiResponseMessage;
import com.hscoderadar.config.oauth.PrincipalDetails;
import com.hscoderadar.domain.auth.dto.request.LoginRequest;
import com.hscoderadar.domain.auth.dto.request.OAuth2LoginRequest;
import com.hscoderadar.domain.auth.dto.request.SignUpRequest;
import com.hscoderadar.domain.auth.dto.response.LoginResponse;
import com.hscoderadar.domain.auth.dto.response.RefreshResponse;
import com.hscoderadar.domain.auth.dto.response.RegisterResponse;
import com.hscoderadar.domain.auth.dto.response.VerifyResponse;
import com.hscoderadar.domain.auth.service.AuthCookieService;
import com.hscoderadar.domain.auth.service.AuthService;
import com.hscoderadar.domain.auth.service.AuthService.LoginResult;
import com.hscoderadar.domain.auth.service.AuthService.TokenRefreshResult;
import com.hscoderadar.domain.user.entity.User;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * v6.1 리팩터링된 인증 시스템 컨트롤러
 *
 * <p>
 * <strong>리팩터링 핵심:</strong>
 *
 * <ul>
 * <li><strong>단일 책임 원칙(SRP) 적용:</strong> 컨트롤러는 HTTP 요청/응답 처리 및 라우팅에만 집중
 * <li><strong>AuthCookieService 도입:</strong> 복잡한 쿠키 생성/삭제 로직을 별도 서비스로 분리
 * <li><strong>전용 응답 DTO 도입:</strong> Map 대신 타입-세이프한 DTO를 사용하여 응답의 명확성 및 안정성 향상
 * <li><strong>AuthService 역할 강화:</strong> 비즈니스 로직을 서비스 계층에 완전히 위임
 * </ul>
 *
 * @author HsCodeRadar Team
 * @since 6.1.0
 * @see AuthService
 * @see AuthCookieService
 * @see LoginResponse
 */
@Tag(name = "인증 API", description = "사용자 회원가입, 로그인, 로그아웃, 토큰 관리 API")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

  private final AuthService authService;
  private final AuthCookieService authCookieService;

  @Operation(summary = "신규 사용자 회원가입", description = "이메일, 비밀번호, 이름으로 신규 계정을 생성합니다.")
  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  @ApiResponseMessage("계정 생성됨")
  public RegisterResponse register(@RequestBody SignUpRequest request) {
    log.info("회원가입 요청: email={}", request.email());
    User savedUser = authService.signUp(request);
    log.info("회원가입 완료: email={}", savedUser.getEmail());
    return RegisterResponse.from(savedUser);
  }

  @Operation(summary = "사용자 로그인", description = "이메일, 비밀번호로 로그인하고 JWT 토큰(Access, Refresh)을 발급받습니다.")
  @PostMapping("/login")
  @ApiResponseMessage("인증됨")
  public LoginResponse login(
      @RequestBody LoginRequest request,
      HttpServletResponse response,
      HttpServletRequest httpRequest) {

    log.info("로그인 요청: email={}, rememberMe={}", request.email(), request.rememberMe());

    try {
      authService.checkLoginRateLimit(httpRequest.getRemoteAddr());

      LoginResult result = authService.loginWithToken(request);

      Cookie refreshTokenCookie = authCookieService.createRefreshTokenCookie(
          result.tokenInfo().refreshToken(), result.rememberMe());
      response.addCookie(refreshTokenCookie);

      log.info("로그인 성공: email={}, rememberMe={}", result.user().getEmail(), result.rememberMe());
      return LoginResponse.of(result.tokenInfo().accessToken(), result.user());

    } catch (RateLimitException e) {
      log.warn("로그인 시도 한도 초과: ip={}, email={}", httpRequest.getRemoteAddr(), request.email());
      throw e;
    } catch (Exception e) {
      log.warn("로그인 실패: email={}, reason={}", request.email(), e.getMessage());
      throw AuthException.invalidCredentials();
    }
  }

  @Operation(summary = "현재 로그인된 사용자 정보 확인", description = "유효한 Access Token을 헤더에 담아 요청하면, 해당 토큰의 사용자 정보를 반환합니다.")
  @GetMapping("/verify")
  @ApiResponseMessage("인증 상태 확인")
  public VerifyResponse verify(@AuthenticationPrincipal PrincipalDetails principalDetails) {
    if (principalDetails == null) {
      log.debug("인증 정보 없음");
      throw AuthException.invalidToken();
    }
    User user = principalDetails.getUser();
    log.debug("인증 상태 확인: email={}", user.getEmail());
    return VerifyResponse.from(user);
  }

  @Operation(summary = "Access Token 갱신", description = "HttpOnly 쿠키에 담긴 Refresh Token을 사용하여 만료된 Access Token을 새로 발급받습니다.")
  @PostMapping("/refresh")
  @ApiResponseMessage("토큰 갱신됨")
  public RefreshResponse refresh(HttpServletRequest httpRequest, HttpServletResponse response) {
    log.info("토큰 갱신 요청");

    String refreshToken = authCookieService.getRefreshTokenFromCookie(httpRequest);
    if (refreshToken == null) {
      log.warn("Refresh Token 쿠키 누락");
      throw AuthException.invalidToken();
    }

    try {
      log.debug("토큰 갱신 시작 - refreshToken 길이: {}", refreshToken.length());

      TokenRefreshResult result = authService.refreshTokens(refreshToken);

      Cookie newRefreshTokenCookie = authCookieService.createRefreshTokenCookie(
          result.tokenInfo().refreshToken(), result.rememberMe());
      response.addCookie(newRefreshTokenCookie);

      log.info("토큰 갱신 완료: rememberMe={}", result.rememberMe());
      return RefreshResponse.from(result);

    } catch (AuthException e) {
      log.error(
          "토큰 갱신 실패 - AuthException: code={}, message={}",
          e.getErrorCode() != null ? e.getErrorCode().name() : "UNKNOWN",
          e.getMessage());
      throw e;
    } catch (Exception e) {
      log.error("토큰 갱신 중 예상치 못한 오류", e);
      throw AuthException.tokenExpired();
    }
  }

  @Operation(summary = "사용자 로그아웃", description = "서버에서 토큰을 만료시키고, 클라이언트의 Refresh Token 쿠키를 삭제합니다.")
  @PostMapping("/logout")
  public ResponseEntity<Void> logout(
      @AuthenticationPrincipal PrincipalDetails principalDetails, HttpServletResponse response) {

    if (principalDetails != null) {
      String userEmail = principalDetails.getUser().getEmail();
      log.info("로그아웃 요청: email={}", userEmail);
      authService.logout(userEmail);
      log.info("로그아웃 완료: email={}", userEmail);
    } else {
      log.debug("이미 로그아웃 상태");
    }

    for (Cookie cookie : authCookieService.clearRefreshTokenCookies()) {
      response.addCookie(cookie);
    }

    log.debug("Refresh Token 쿠키 삭제 시도");

    return ResponseEntity.noContent().build();
  }

  @Operation(summary = "OAuth2 소셜 로그인 시작", description = "지정된 소셜 로그인 제공자(google, naver, kakao)의 인증 페이지로 리디렉션합니다.")
  @GetMapping("/oauth2/authorization/{provider}")
  public RedirectView startOAuth2Login(
      @PathVariable String provider,
      @ModelAttribute OAuth2LoginRequest loginRequest,
      HttpServletRequest request) {

    log.info("OAuth2 로그인 시작: provider={}, rememberMe={}", provider, loginRequest.rememberMe());

    List<String> supportedProviders = Arrays.asList("google", "naver", "kakao");
    if (!supportedProviders.contains(provider.toLowerCase())) {
      throw new IllegalArgumentException("지원하지 않는 OAuth 제공자임");
    }

    request.getSession().setAttribute("rememberMe", loginRequest.rememberMe());

    String redirectUrl = "/oauth2/authorization/" + provider.toLowerCase();
    log.debug("OAuth2 리디렉션: {}", redirectUrl);
    return new RedirectView(redirectUrl, true);
  }
}
