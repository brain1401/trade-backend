package com.hscoderadar.domain.auth.controller;

import com.hscoderadar.common.exception.AuthException;
import com.hscoderadar.common.exception.RateLimitException;
import com.hscoderadar.common.response.ApiResponseMessage;
import com.hscoderadar.config.oauth.PrincipalDetails;
import com.hscoderadar.domain.auth.dto.request.LoginRequest;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Arrays;
import java.util.List;

/**
 * v6.1 리팩터링된 인증 시스템 컨트롤러
 *
 * <p>
 * <strong>리팩터링 핵심:</strong>
 * <ul>
 * <li><strong>단일 책임 원칙(SRP) 적용:</strong> 컨트롤러는 HTTP 요청/응답 처리 및 라우팅에만 집중</li>
 * <li><strong>AuthCookieService 도입:</strong> 복잡한 쿠키 생성/삭제 로직을 별도 서비스로 분리</li>
 * <li><strong>전용 응답 DTO 도입:</strong> Map 대신 타입-세이프한 DTO를 사용하여 응답의 명확성 및 안정성
 * 향상</li>
 * <li><strong>AuthService 역할 강화:</strong> 비즈니스 로직을 서비스 계층에 완전히 위임</li>
 * </ul>
 * </p>
 *
 * @author HsCodeRadar Team
 * @since 6.1.0
 * @see AuthService
 * @see AuthCookieService
 * @see LoginResponse
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final AuthCookieService authCookieService;

    /**
     * 새로운 사용자 계정 생성 (v6.1 명세 기준)
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @ApiResponseMessage("계정이 생성되었습니다")
    public RegisterResponse register(@RequestBody SignUpRequest request) {
        log.info("회원가입 요청: email={}", request.getEmail());
        User savedUser = authService.signUp(request);
        log.info("회원가입 완료: email={}", savedUser.getEmail());
        return RegisterResponse.from(savedUser);
    }

    /**
     * 사용자 로그인 처리 및 v6.1 변경된 JWT 토큰 발급
     */
    @PostMapping("/login")
    @ApiResponseMessage("인증되었습니다")
    public LoginResponse login(
            @RequestBody LoginRequest request,
            HttpServletResponse response,
            HttpServletRequest httpRequest) {

        log.info("로그인 요청: email={}, rememberMe={}", request.getEmail(), request.isRememberMe());

        try {
            authService.checkLoginRateLimit(httpRequest.getRemoteAddr());

            LoginResult result = authService.loginWithToken(request);

            Cookie refreshTokenCookie = authCookieService.createRefreshTokenCookie(
                    result.tokenInfo().refreshToken(), result.rememberMe());
            response.addCookie(refreshTokenCookie);

            log.info("로그인 성공: email={}, rememberMe={}", result.user().getEmail(), result.rememberMe());
            return LoginResponse.of(result.tokenInfo().accessToken(), result.user());

        } catch (RateLimitException e) {
            log.warn("로그인 시도 한도 초과: ip={}, email={}", httpRequest.getRemoteAddr(), request.getEmail());
            throw e;
        } catch (Exception e) {
            log.warn("로그인 실패: email={}, reason={}", request.getEmail(), e.getMessage());
            throw AuthException.invalidCredentials();
        }
    }

    /**
     * 현재 JWT 토큰 상태 확인 및 사용자 정보 반환 (v6.1 명세 기준)
     */
    @GetMapping("/verify")
    @ApiResponseMessage("인증 상태 확인됨")
    public VerifyResponse verify(@AuthenticationPrincipal PrincipalDetails principalDetails) {
        if (principalDetails == null) {
            log.debug("인증 정보 없음");
            throw AuthException.invalidToken();
        }
        User user = principalDetails.getUser();
        log.debug("인증 상태 확인: email={}", user.getEmail());
        return VerifyResponse.from(user);
    }

    /**
     * Refresh Token을 사용하여 새로운 Access Token 발급
     */
    @PostMapping("/refresh")
    @ApiResponseMessage("토큰이 갱신되었습니다")
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
            log.error("토큰 갱신 실패 - AuthException: code={}, message={}",
                    e.getErrorCode() != null ? e.getErrorCode().name() : "UNKNOWN",
                    e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("토큰 갱신 중 예상치 못한 오류", e);
            throw AuthException.tokenExpired();
        }
    }

    /**
     * 사용자 로그아웃 처리 및 v6.1 변경된 토큰 정리 정책
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            HttpServletResponse response) {

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

    /**
     * OAuth2 소셜 로그인 시작 (v6.1 명세서 준수)
     */
    @GetMapping("/oauth2/authorization/{provider}")
    public RedirectView startOAuth2Login(
            @PathVariable String provider,
            @RequestParam(defaultValue = "false") boolean rememberMe,
            HttpServletRequest request) {

        log.info("OAuth2 로그인 시작: provider={}, rememberMe={}", provider, rememberMe);

        List<String> supportedProviders = Arrays.asList("google", "naver", "kakao");
        if (!supportedProviders.contains(provider.toLowerCase())) {
            throw new IllegalArgumentException("지원하지 않는 OAuth 제공자입니다");
        }

        request.getSession().setAttribute("rememberMe", rememberMe);

        String redirectUrl = "/oauth2/authorization/" + provider.toLowerCase();
        log.debug("OAuth2 리디렉션: {}", redirectUrl);
        return new RedirectView(redirectUrl, true);
    }
}