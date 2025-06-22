package com.hscoderadar.domain.auth.controller;

import com.hscoderadar.common.response.ApiResponseMessage;
import com.hscoderadar.config.oauth.PrincipalDetails;
import com.hscoderadar.domain.auth.dto.request.LoginRequest;
import com.hscoderadar.domain.auth.dto.request.SignUpRequest;
import com.hscoderadar.domain.auth.service.AuthService;
import com.hscoderadar.domain.users.entity.User;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.HashMap;

/**
 * JWT 기반 인증 시스템을 위한 인증 REST API 컨트롤러
 * 
 * <p>
 * 이 컨트롤러는 Public API와 Private API를 구분하여 차별화된 보안 정책을 적용하는
 * JWT 기반 인증 시스템을 구현합니다:
 * <ul>
 * <li>HttpOnly 쿠키 기반 JWT 토큰 관리 (XSS 완전 차단)</li>
 * <li>인증 상태 확인 API (/api/auth/verify)</li>
 * <li>자체 회원가입 및 로그인</li>
 * <li>안전한 로그아웃 처리</li>
 * </ul>
 * 
 * <p>
 * <strong>보안 특징:</strong>
 * <ul>
 * <li>JWT 토큰을 HttpOnly 쿠키에 저장 (JavaScript 접근 불가)</li>
 * <li>CSRF 방지를 위한 SameSite=Strict 설정</li>
 * <li>HTTPS에서만 쿠키 전송 (Secure 속성)</li>
 * <li>Remember Me 기능 지원 (7일/세션 쿠키 선택)</li>
 * </ul>
 * 
 * @author HsCodeRadar Team
 * @since 2.1.0
 * @see AuthService
 * @see ApiResponseMessage
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    /**
     * 새로운 사용자 계정 생성
     * 
     * <p>
     * 이메일, 비밀번호, 이름을 받아 신규 사용자 계정 생성
     * v2.2 보안 정책에 따라 내부 식별자 및 시스템 정보는 클라이언트에 노출하지 않음
     * 
     * <h3>요청 예시:</h3>
     * 
     * <pre>{@code
     * POST /api/auth/register
     * Content-Type: application/json
     * 
     * {
     *   "email": "user@example.com",
     *   "password": "password123",
     *   "name": "홍길동"
     * }
     * }</pre>
     * 
     * <h3>응답 예시 (v2.2):</h3>
     * 
     * <pre>{@code
     * {
     *   "success": "SUCCESS",
     *   "message": "계정이 생성되었습니다",
     *   "data": {
     *     "email": "user@example.com",
     *     "name": "홍길동",
     *     "profileImage": null
     *   }
     * }
     * }</pre>
     * 
     * @param request 회원가입 요청 정보 (이메일, 비밀번호, 이름 포함)
     * @return v2.2 보안 정책에 따른 최소 사용자 정보 (이메일, 이름, 프로필이미지만)
     */
    @PostMapping("/register")
    @ApiResponseMessage("계정이 생성되었습니다")
    public Map<String, Object> register(@RequestBody SignUpRequest request) {
        log.info("회원가입 요청: email={}", request.getEmail());

        User savedUser = authService.signUp(request);

        // v2.2 보안 정책: 클라이언트에 최소 정보만 제공
        Map<String, Object> userData = new HashMap<>();
        userData.put("email", savedUser.getEmail());
        userData.put("name", savedUser.getName());
        userData.put("profileImage", savedUser.getProfileImage()); // 회원가입 시 일반적으로 null

        log.info("회원가입 완료: email={}", savedUser.getEmail());
        return userData;
    }

    /**
     * 사용자 로그인 처리 및 JWT 토큰을 HttpOnly 쿠키에 설정
     * 
     * <p>
     * 이메일과 비밀번호로 사용자 인증 후, JWT 토큰을 HttpOnly 쿠키에 설정
     * v2.2 보안 정책에 따라 최소 사용자 정보만 클라이언트에 제공
     * 
     * <h3>요청 예시:</h3>
     * 
     * <pre>{@code
     * POST /api/auth/login
     * Content-Type: application/json
     * 
     * {
     *   "email": "user@example.com",
     *   "password": "password123",
     *   "rememberMe": true
     * }
     * }</pre>
     * 
     * <h3>응답 예시 (v2.2):</h3>
     * 
     * <pre>{@code
     * {
     *   "success": "SUCCESS",
     *   "message": "인증되었습니다",
     *   "data": {
     *     "user": {
     *       "email": "user@example.com",
     *       "name": "홍길동",
     *       "profileImage": "https://example.com/profile.jpg"
     *     }
     *   }
     * }
     * }</pre>
     * 
     * @param request  로그인 요청 정보 (이메일, 비밀번호, Remember Me)
     * @param response HTTP 응답 객체 (쿠키 설정용)
     * @return v2.2 보안 정책에 따른 최소 사용자 정보 (토큰은 쿠키에 설정)
     */
    @PostMapping("/login")
    @ApiResponseMessage("인증되었습니다")
    public Map<String, Object> login(
            @RequestBody LoginRequest request,
            HttpServletResponse response) {
        log.info("로그인 요청: email={}, rememberMe={}", request.getEmail(), request.isRememberMe());

        String jwtToken = authService.loginWithCookie(request);
        User user = authService.findUserByEmail(request.getEmail());

        // JWT를 HttpOnly 쿠키에 설정
        Cookie jwtCookie = new Cookie("token", jwtToken);
        jwtCookie.setHttpOnly(true); // JavaScript 접근 불가
        jwtCookie.setSecure(true); // HTTPS에서만 전송
        jwtCookie.setPath("/"); // 전체 경로에서 사용
        jwtCookie.setAttribute("SameSite", "Strict"); // CSRF 방지

        // Remember Me 설정에 따라 쿠키 수명 결정
        if (request.isRememberMe()) {
            jwtCookie.setMaxAge(7 * 24 * 60 * 60); // 7일
        }
        // else: 세션 쿠키 (브라우저 종료 시 삭제)

        response.addCookie(jwtCookie);

        // v2.2 보안 정책: 클라이언트에 최소 정보만 제공
        Map<String, Object> userData = new HashMap<>();
        userData.put("email", user.getEmail());
        userData.put("name", user.getName());
        userData.put("profileImage", user.getProfileImage()); // OAuth 프로필 이미지 지원

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("user", userData);

        log.info("로그인 성공: email={}", user.getEmail());
        return responseData;
    }

    /**
     * HttpOnly 쿠키의 JWT 토큰 검증 및 사용자 정보 반환
     * 
     * <p>
     * 앱 초기화 시 인증 상태 확인 또는 인증이 필요한 API 호출 전
     * 현재 로그인 상태 검증용
     * v2.2 보안 정책에 따라 최소 사용자 정보만 제공
     * 
     * <h3>요청 예시:</h3>
     * 
     * <pre>{@code
     * GET /api/auth/verify
     * Cookie: token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
     * }</pre>
     * 
     * <h3>응답 예시 (v2.2):</h3>
     * 
     * <pre>{@code
     * {
     *   "success": "SUCCESS",
     *   "message": "인증 상태 확인됨",
     *   "data": {
     *     "email": "user@example.com",
     *     "name": "홍길동",
     *     "profileImage": "https://example.com/profile.jpg"
     *   }
     * }
     * }</pre>
     * 
     * @param principalDetails 인증된 사용자 정보 (Spring Security Context에서 주입)
     * @return v2.2 보안 정책에 따른 최소 사용자 정보
     */
    @GetMapping("/verify")
    @ApiResponseMessage("인증 상태 확인됨")
    public Map<String, Object> verify(@AuthenticationPrincipal PrincipalDetails principalDetails) {
        if (principalDetails == null) {
            throw new IllegalArgumentException("인증 정보가 없습니다.");
        }

        User user = principalDetails.getUser();
        log.debug("인증 상태 확인: email={}", user.getEmail());

        // v2.2 보안 정책: 클라이언트에 최소 정보만 제공
        Map<String, Object> userData = new HashMap<>();
        userData.put("email", user.getEmail());
        userData.put("name", user.getName());
        userData.put("profileImage", user.getProfileImage()); // OAuth 프로필 이미지 지원

        return userData;
    }

    /**
     * 사용자 로그아웃 처리 및 HttpOnly 쿠키 삭제
     * 
     * <p>
     * 서버에서 쿠키 무효화 및 필요시 Refresh Token도 데이터베이스에서 제거
     * 
     * <h3>요청 예시:</h3>
     * 
     * <pre>{@code
     * POST /api/auth/logout
     * Cookie: token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
     * }</pre>
     * 
     * <h3>응답 예시:</h3>
     * 
     * <pre>{@code
     * {
     *   "success": "SUCCESS",
     *   "message": "세션이 종료되었습니다",
     *   "data": null
     * }
     * }</pre>
     * 
     * @param principalDetails 인증된 사용자 정보
     * @param response         HTTP 응답 객체 (쿠키 삭제용)
     * @return 성공 메시지
     */
    @PostMapping("/logout")
    @ApiResponseMessage("세션이 종료되었습니다")
    public String logout(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            HttpServletResponse response) {

        if (principalDetails != null) {
            String userEmail = principalDetails.getUser().getEmail();
            log.info("로그아웃 요청: email={}", userEmail);

            // 데이터베이스에서 Refresh Token 제거
            authService.logout(userEmail);
        }

        // HttpOnly 쿠키 삭제
        Cookie jwtCookie = new Cookie("token", null);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setSecure(true);
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(0); // 즉시 만료
        jwtCookie.setAttribute("SameSite", "Strict");

        response.addCookie(jwtCookie);

        log.info("로그아웃 완료");
        return "로그아웃 성공";
    }
}