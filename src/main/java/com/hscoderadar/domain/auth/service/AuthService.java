package com.hscoderadar.domain.auth.service;

import com.hscoderadar.common.exception.AuthException;
import com.hscoderadar.common.exception.RateLimitException;
import com.hscoderadar.config.jwt.JwtTokenProvider;
import com.hscoderadar.config.jwt.JwtTokenProvider.TokenInfo;
import com.hscoderadar.domain.auth.dto.request.LoginRequest;
import com.hscoderadar.domain.auth.dto.request.SignUpRequest;
import com.hscoderadar.domain.users.entity.User;
import com.hscoderadar.domain.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * API 명세서 v2.4 기준 사용자 인증 관련 비즈니스 로직을 처리하는 서비스 클래스
 * 
 * v2.4 주요 개선사항을 포함하여 다음과 같은 인증 관련 기능을 제공:
 * <ul>
 * <li>회원가입 처리 및 비밀번호 정책 검증</li>
 * <li>로그인 인증 및 JWT 토큰 발급</li>
 * <li>Rate Limiting 기반 브루트 포스 공격 방지</li>
 * <li>사용자 열거 공격 방지를 위한 통합 에러 처리</li>
 * <li>로그아웃 처리 및 토큰 무효화</li>
 * <li>Refresh Token을 이용한 토큰 갱신</li>
 * </ul>
 * 
 * <h3>v2.4 보안 정책:</h3>
 * <ul>
 * <li>모든 인증 실패를 AUTH_001로 통일 처리</li>
 * <li>IP 기반 Rate Limiting (5회/15분)</li>
 * <li>비밀번호는 BCrypt 알고리즘으로 암호화</li>
 * <li>JWT Access Token (1시간) + Refresh Token (14일) 사용</li>
 * <li>Token Rotation 방식으로 보안 강화</li>
 * </ul>
 * 
 * @author HsCodeRadar Team
 * @since 2.4.0
 * @see UserRepository
 * @see JwtTokenProvider
 * @see CustomUserDetailsService
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;

    // Rate Limiting을 위한 메모리 기반 저장소 (실제 운영 환경에서는 Redis 사용 권장)
    private final ConcurrentHashMap<String, AttemptRecord> loginAttempts = new ConcurrentHashMap<>();

    /**
     * 로그인 시도 기록을 위한 내부 클래스
     */
    private static class AttemptRecord {
        private final AtomicInteger count = new AtomicInteger(0);
        private LocalDateTime lastAttempt = LocalDateTime.now();

        void recordAttempt() {
            count.incrementAndGet();
            lastAttempt = LocalDateTime.now();
        }

        int getCount() {
            return count.get();
        }

        LocalDateTime getLastAttempt() {
            return lastAttempt;
        }

        void reset() {
            count.set(0);
            lastAttempt = LocalDateTime.now();
        }
    }

    /**
     * IP 기반 로그인 시도 제한 검사 (API 명세서 v2.4 기준)
     * 
     * 브루트 포스 공격 방지를 위해 동일 IP에서 15분 내 5회 이상 로그인 실패 시 차단
     * 
     * @param clientIp 클라이언트 IP 주소
     * @throws RateLimitException 로그인 시도 한도 초과 시
     */
    public void checkLoginRateLimit(String clientIp) {
        AttemptRecord record = loginAttempts.get(clientIp);

        if (record != null) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime window = now.minusMinutes(15); // 15분 윈도우

            // 15분이 지나면 리셋
            if (record.getLastAttempt().isBefore(window)) {
                record.reset();
                return;
            }

            // 5회 이상 실패 시 차단
            if (record.getCount() >= 5) {
                log.warn("로그인 시도 한도 초과: ip={}, attempts={}", clientIp, record.getCount());
                throw RateLimitException.loginAttemptsExceeded();
            }
        }
    }

    /**
     * 로그인 실패 기록 (Rate Limiting용)
     * 
     * @param clientIp 클라이언트 IP 주소
     */
    private void recordFailedLogin(String clientIp) {
        loginAttempts.computeIfAbsent(clientIp, k -> new AttemptRecord()).recordAttempt();
    }

    /**
     * 로그인 성공 시 실패 기록 초기화
     * 
     * @param clientIp 클라이언트 IP 주소
     */
    private void clearFailedLogins(String clientIp) {
        loginAttempts.remove(clientIp);
    }

    /**
     * 새로운 사용자 계정 생성 (API 명세서 v2.4 기준)
     * 
     * HTTP 상태 코드 매핑:
     * - 성공 시: 201 Created
     * - 이메일 중복: 409 Conflict (USER_001)
     * - 비밀번호 정책 위반: 422 Unprocessable Entity (USER_004)
     * - 입력 데이터 오류: 400 Bad Request (USER_002)
     * 
     * @param request 회원가입 요청 정보 (이메일, 비밀번호, 이름)
     * @return 생성된 사용자 엔티티 (ID 포함)
     * @throws IllegalArgumentException 이메일 중복, 비밀번호 정책 위반 등
     */
    @Transactional
    public User signUp(SignUpRequest request) {
        log.info("회원가입 처리 시작: email={}", request.getEmail());

        // 이메일 중복 확인
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("이메일 중복 시도: email={}", request.getEmail());
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다");
        }

        // 비밀번호 정책 검증
        validatePasswordPolicy(request.getPassword());

        // DTO를 Entity로 변환 (비밀번호 암호화 포함)
        User newUser = request.toEntity(passwordEncoder);

        // 추가 검증: 비밀번호 해시가 올바르게 생성되었는지 확인
        if (newUser.getPasswordHash() == null || newUser.getPasswordHash().trim().isEmpty()) {
            throw new IllegalStateException("비밀번호 암호화 처리 실패");
        }

        User savedUser = userRepository.save(newUser);

        log.info("회원가입 완료: userId={}, email={}",
                savedUser.getId(), savedUser.getEmail());
        return savedUser;
    }

    /**
     * 비밀번호 정책 검증 (v2.4 강화된 정책)
     * 
     * @param password 검증할 비밀번호
     * @throws IllegalArgumentException 정책 위반 시
     */
    private void validatePasswordPolicy(String password) {
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("비밀번호는 필수입니다");
        }

        if (password.length() < 8) {
            throw new IllegalArgumentException("비밀번호는 8자 이상이어야 합니다");
        }

        // 추가 정책들 (필요에 따라 확장)
        // if (!password.matches(".*[A-Z].*")) {
        // throw new IllegalArgumentException("비밀번호에 대문자가 포함되어야 합니다");
        // }
    }

    /**
     * 사용자 로그인 처리 및 JWT 토큰 발급 (API 명세서 v2.4 기준)
     * 
     * v2.4 보안 정책: 모든 인증 실패를 AUTH_001로 통일하여 사용자 열거 공격 방지
     * 
     * @param request 로그인 요청 정보 (이메일, 비밀번호)
     * @return JWT 토큰 정보 (Access Token, Refresh Token 포함)
     * @throws AuthException 인증 실패 시
     */
    @Transactional
    public TokenInfo login(LoginRequest request) {
        log.info("로그인 처리 시작: email={}", request.getEmail());

        try {
            // 1. Login ID/PW 를 기반으로 Authentication 객체 생성
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                    request.getEmail(), request.getPassword());

            // 2. 실제 검증 (사용자 비밀번호 체크)
            Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);

            // 3. 인증 정보를 기반으로 JWT 토큰 생성
            TokenInfo tokenInfo = jwtTokenProvider.generateToken(authentication);

            // 4. DB에 Refresh Token 저장
            User user = userRepository.findByEmail(authentication.getName())
                    .orElseThrow(() -> AuthException.invalidCredentials());
            user.setRefreshToken(tokenInfo.refreshToken());

            log.info("로그인 완료: userId={}, email={}", user.getId(), user.getEmail());
            return tokenInfo;

        } catch (BadCredentialsException e) {
            log.warn("인증 실패: email={}", request.getEmail());
            // v2.4 보안 정책: 모든 인증 실패를 AUTH_001로 통일
            throw AuthException.invalidCredentials();
        } catch (Exception e) {
            log.error("로그인 처리 중 오류: email={}", request.getEmail(), e);
            // 예상치 못한 오류도 AUTH_001로 통일
            throw AuthException.invalidCredentials();
        }
    }

    /**
     * HttpOnly 쿠키 기반 로그인 처리 (API 명세서 v2.4 기준)
     * 
     * JWT Access Token만 반환하여 HttpOnly 쿠키에 저장하도록 설계됨
     * v2.4 보안 정책: 모든 인증 실패를 AUTH_001로 통일
     * 
     * @param request 로그인 요청 정보 (이메일, 비밀번호, Remember Me)
     * @return JWT Access Token (HttpOnly 쿠키용)
     * @throws AuthException 인증 실패 시
     */
    @Transactional
    public String loginWithCookie(LoginRequest request) {
        log.info("쿠키 로그인 처리 시작: email={}, rememberMe={}", request.getEmail(), request.isRememberMe());

        try {
            // 1. 사용자 인증
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                    request.getEmail(), request.getPassword());

            Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);

            // 2. JWT 토큰 생성
            TokenInfo tokenInfo = jwtTokenProvider.generateToken(authentication);

            // 3. DB에 Refresh Token 저장 (옵션)
            User user = userRepository.findByEmail(authentication.getName())
                    .orElseThrow(() -> AuthException.invalidCredentials());
            user.setRefreshToken(tokenInfo.refreshToken());

            log.info("쿠키 로그인 완료: userId={}, email={}", user.getId(), user.getEmail());
            return tokenInfo.accessToken();

        } catch (BadCredentialsException e) {
            log.warn("쿠키 로그인 인증 실패: email={}", request.getEmail());
            // v2.4 보안 정책: 모든 인증 실패를 AUTH_001로 통일
            throw AuthException.invalidCredentials();
        } catch (Exception e) {
            log.error("쿠키 로그인 처리 중 오류: email={}", request.getEmail(), e);
            throw AuthException.invalidCredentials();
        }
    }

    /**
     * 사용자의 Refresh Token을 데이터베이스에서 삭제하여 로그아웃 처리합니다.
     * 
     * <p>
     * 로그아웃 처리는 서버 측에서 Refresh Token을 무효화하는 것으로 이루어집니다.
     * Access Token은 클라이언트 측에서 삭제하고, 서버에서는 만료 시까지 유효합니다.
     * 
     * <h3>처리 과정:</h3>
     * <ol>
     * <li>사용자 이메일로 계정 조회</li>
     * <li>해당 사용자의 Refresh Token을 null로 설정</li>
     * <li>데이터베이스에 변경사항 반영</li>
     * </ol>
     * 
     * <h3>보안 고려사항:</h3>
     * <ul>
     * <li>Refresh Token 무효화로 토큰 갱신 차단</li>
     * <li>Access Token은 만료 시까지 유효하므로 클라이언트에서 삭제 필요</li>
     * <li>완전한 보안을 위해서는 토큰 블랙리스트 구현 고려</li>
     * </ul>
     * 
     * @param email 로그아웃할 사용자의 이메일 주소
     * @throws AuthException 해당 이메일의 사용자를 찾을 수 없는 경우
     */
    @Transactional
    public void logout(String email) {
        log.info("로그아웃 처리 시작: email={}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("로그아웃 시 사용자 없음: email={}", email);
                    return AuthException.invalidCredentials(); // v2.4: 사용자 열거 공격 방지
                });

        // Refresh Token 무효화
        user.setRefreshToken(null);
        userRepository.save(user);

        log.info("로그아웃 완료: userId={}, email={}", user.getId(), user.getEmail());
    }

    /**
     * Refresh Token을 이용한 토큰 갱신 (API 명세서 v2.4 기준)
     * 
     * @param refreshToken 갱신용 Refresh Token
     * @return 새로운 토큰 정보
     * @throws AuthException 토큰이 유효하지 않은 경우
     */
    @Transactional
    public TokenInfo refreshTokens(String refreshToken) {
        log.info("토큰 갱신 요청");

        try {
            // 1. Refresh Token 검증
            if (!jwtTokenProvider.validateToken(refreshToken)) {
                log.warn("유효하지 않은 Refresh Token");
                throw AuthException.tokenExpired();
            }

            // 2. 토큰에서 사용자 정보 추출
            Authentication authentication = jwtTokenProvider.getAuthentication(refreshToken);
            String email = authentication.getName();

            // 3. DB에 저장된 Refresh Token과 비교
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> AuthException.invalidCredentials());

            if (!refreshToken.equals(user.getRefreshToken())) {
                log.warn("DB의 Refresh Token과 불일치: email={}", email);
                throw AuthException.invalidToken();
            }

            // 4. 새로운 토큰 생성
            TokenInfo newTokenInfo = jwtTokenProvider.generateToken(authentication);

            // 5. 새로운 Refresh Token 저장 (Token Rotation)
            user.setRefreshToken(newTokenInfo.refreshToken());

            log.info("토큰 갱신 완료: email={}", email);
            return newTokenInfo;

        } catch (Exception e) {
            log.error("토큰 갱신 실패", e);
            throw AuthException.tokenExpired();
        }
    }

    /**
     * 이메일로 사용자 조회 (API 명세서 v2.4 기준)
     * 
     * @param email 조회할 사용자 이메일
     * @return 사용자 엔티티
     * @throws AuthException 사용자를 찾을 수 없는 경우
     */
    public User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.debug("사용자 조회 실패: email={}", email);
                    // v2.4 보안 정책: 사용자 존재 여부 노출 방지
                    return AuthException.invalidCredentials();
                });
    }
}