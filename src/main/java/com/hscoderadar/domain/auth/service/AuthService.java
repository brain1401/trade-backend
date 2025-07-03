package com.hscoderadar.domain.auth.service;

import com.hscoderadar.common.exception.AuthException;
import com.hscoderadar.common.exception.ErrorCode;
import com.hscoderadar.common.exception.RateLimitException;
import com.hscoderadar.config.jwt.JwtTokenProvider;
import com.hscoderadar.config.jwt.JwtTokenProvider.ProviderTokenRefreshResult;
import com.hscoderadar.config.jwt.JwtTokenProvider.TokenInfo;
import com.hscoderadar.domain.auth.dto.request.LoginRequest;
import com.hscoderadar.domain.auth.dto.request.SignUpRequest;
import com.hscoderadar.domain.user.entity.User;
import com.hscoderadar.domain.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * v6.1 변경된 JWT 토큰 정책을 적용한 인증 서비스
 *
 * <p>
 * v6.1 JWT 토큰 정책에 따른 인증 관련 기능 제공:
 *
 * <ul>
 * <li>회원가입 처리 및 비밀번호 정책 검증
 * <li>로그인 인증 및 JWT 토큰 발급
 * <li>Rate Limiting 기반 브루트 포스 공격 방지
 * <li>사용자 열거 공격 방지를 위한 통합 에러 처리
 * <li>Token Rotation 방식 로그아웃 처리 및 토큰 무효화
 * <li>PostgreSQL 검증 기반 Refresh Token 갱신
 * </ul>
 *
 * v6.1 보안 정책:
 *
 * <ul>
 * <li>모든 인증 실패를 AUTH_001로 통일 처리
 * <li>IP 기반 Rate Limiting (5회/15분)
 * <li>비밀번호는 BCrypt 알고리즘으로 암호화
 * <li>Access Token (30분): Authorization Bearer 헤더로 전송
 * <li>Refresh Token (1일/30일): HttpOnly 쿠키로 관리
 * <li>Token Rotation 방식으로 보안 강화
 * <li>remember me 기반 차별화된 토큰 수명 관리
 * </ul>
 *
 * @author HsCodeRadar Team
 * @since 6.1.0
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

  /** 서비스 계층에서 컨트롤러로 전달할 토큰 갱신 결과 DTO */
  public record TokenRefreshResult(TokenInfo tokenInfo, boolean rememberMe) {
  }

  /** 서비스 계층에서 컨트롤러로 전달할 로그인 결과 DTO */
  public record LoginResult(TokenInfo tokenInfo, User user, boolean rememberMe) {
  }

  /** 로그인 시도 기록을 위한 내부 클래스 */
  private static class AttemptRecord {
    private final AtomicInteger count = new AtomicInteger(0);
    private LocalDateTime lastAttempt = LocalDateTime.now();

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
   * <p>
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
   * 새로운 사용자 계정 생성 (API 명세서 v2.4 기준)
   *
   * <p>
   * HTTP 상태 코드 매핑: - 성공 시: 201 Created - 이메일 중복: 409 Conflict (USER_001) - 비밀번호
   * 정책 위반: 422
   * Unprocessable Entity (USER_004) - 입력 데이터 오류: 400 Bad Request (USER_002)
   *
   * @param request 회원가입 요청 정보 (이메일, 비밀번호, 이름)
   * @return 생성된 사용자 엔티티 (ID 포함)
   * @throws IllegalArgumentException 이메일 중복, 비밀번호 정책 위반 등
   */
  @Transactional
  public User signUp(SignUpRequest request) {
    log.info("회원가입 처리 시작: email={}", request.email());

    // 이메일 중복 확인
    if (userRepository.existsByEmail(request.email())) {
      log.warn("이메일 중복 시도: email={}", request.email());
      throw new IllegalArgumentException("이미 사용 중인 이메일임");
    }

    // 비밀번호 정책 검증
    validatePasswordPolicy(request.password());

    // DTO를 Entity로 변환 (비밀번호 암호화 포함)
    User newUser = request.toEntity(passwordEncoder);

    // 추가 검증: 비밀번호 해시가 올바르게 생성되었는지 확인
    if (newUser.getPasswordHash() == null || newUser.getPasswordHash().trim().isEmpty()) {
      throw new IllegalStateException("비밀번호 암호화 처리 실패");
    }

    User savedUser = userRepository.save(newUser);

    log.info("회원가입 완료: userId={}, email={}", savedUser.getId(), savedUser.getEmail());
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
      throw new IllegalArgumentException("비밀번호는 필수임");
    }

    if (password.length() < 8) {
      throw new IllegalArgumentException("비밀번호는 8자 이상이어야 함");
    }

    // 추가 정책들 (필요에 따라 확장)
    // if (!password.matches(".*[A-Z].*")) {
    // throw new IllegalArgumentException("비밀번호에 대문자가 포함되어야 함");
    // }
  }

  /**
   * 사용자 로그인 처리 및 JWT 토큰 발급 (API 명세서 v2.4 기준)
   *
   * <p>
   * v2.4 보안 정책: 모든 인증 실패를 AUTH_001로 통일하여 사용자 열거 공격 방지
   *
   * @param request 로그인 요청 정보 (이메일, 비밀번호)
   * @return JWT 토큰 정보 (Access Token, Refresh Token 포함)
   * @throws AuthException 인증 실패 시
   */
  @Transactional
  public TokenInfo login(LoginRequest request) {
    log.info("로그인 처리 시작: email={}", request.email());

    try {
      // 1. Login ID/PW 를 기반으로 Authentication 객체 생성
      UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(request.email(),
          request.password());

      // 2. 실제 검증 (사용자 비밀번호 체크)
      Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);

      // 3. 인증 정보를 기반으로 JWT 토큰 생성 (기본값: remember me false)
      TokenInfo tokenInfo = jwtTokenProvider.generateToken(authentication, false);

      // 4. DB에 Refresh Token 저장
      User user = userRepository
          .findByEmail(authentication.getName())
          .orElseThrow(() -> AuthException.invalidCredentials());
      // v6.1: User 엔티티의 updateRefreshToken 메서드 사용
      user.updateRefreshToken(
          tokenInfo.refreshToken(),
          LocalDateTime.now().plusDays(1), // 기본 1일
          false); // remember me 미사용

      log.info("로그인 완료: userId={}, email={}", user.getId(), user.getEmail());
      return tokenInfo;

    } catch (BadCredentialsException e) {
      log.warn("인증 실패: email={}", request.email());
      // v2.4 보안 정책: 모든 인증 실패를 AUTH_001로 통일
      throw AuthException.invalidCredentials();
    } catch (Exception e) {
      log.error("로그인 처리 중 오류: email={}", request.email(), e);
      // 예상치 못한 오류도 AUTH_001로 통일
      throw AuthException.invalidCredentials();
    }
  }

  /**
   * v6.1 요구사항: remember me 옵션을 고려한 토큰 생성 로그인 처리
   *
   * <p>
   * Access Token과 Refresh Token을 모두 반환하되, remember me 옵션에 따라 Refresh Token의 만료
   * 시간을 차별화하여 생성함. -
   * remember me 체크시: 30일 - remember me 미체크시: 1일
   *
   * <p>
   * v6.1 보안 정책: 모든 인증 실패를 AUTH_001로 통일
   *
   * @param request 로그인 요청 정보 (이메일, 비밀번호, Remember Me)
   * @return JWT 토큰 정보와 사용자 엔티티를 포함한 결과 객체
   * @throws AuthException 인증 실패 시
   */
  @Transactional
  public LoginResult loginWithToken(LoginRequest request) {
    log.info("v6.1 토큰 로그인 처리 시작: email={}, rememberMe={}", request.email(), request.rememberMe());

    try {
      // 1. 사용자 인증
      UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(request.email(),
          request.password());

      Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);

      boolean rememberMe = request.rememberMe();

      // 2. v6.1 JWT 세부화: remember me 옵션을 고려한 JWT 토큰 생성
      TokenInfo tokenInfo = jwtTokenProvider.generateToken(authentication, rememberMe);

      // 3. DB에 Refresh Token 저장
      User user = userRepository
          .findByEmail(authentication.getName())
          .orElseThrow(AuthException::invalidCredentials);
      // v6.1: remember me 옵션에 따른 토큰 수명 설정
      LocalDateTime expiresAt = rememberMe ? LocalDateTime.now().plusDays(30) : LocalDateTime.now().plusDays(1);
      user.updateRefreshToken(tokenInfo.refreshToken(), expiresAt, rememberMe);
      userRepository.save(user);

      log.info(
          "v6.1 토큰 로그인 완료: userId={}, email={}, rememberMe={}",
          user.getId(),
          user.getEmail(),
          rememberMe);
      return new LoginResult(tokenInfo, user, rememberMe);

    } catch (BadCredentialsException e) {
      log.warn("v6.1 토큰 로그인 인증 실패: email={}", request.email());
      // v6.1 보안 정책: 모든 인증 실패를 AUTH_001로 통일
      throw AuthException.invalidCredentials();
    } catch (Exception e) {
      log.error("v6.1 토큰 로그인 처리 중 오류: email={}", request.email(), e);
      throw AuthException.invalidCredentials();
    }
  }

  /**
   * HttpOnly 쿠키 기반 로그인 처리 (API 명세서 v2.4 기준)
   *
   * <p>
   * JWT Access Token만 반환하여 HttpOnly 쿠키에 저장하도록 설계 v2.4 보안 정책: 모든 인증 실패를 AUTH_001로
   * 통일
   *
   * @param request 로그인 요청 정보 (이메일, 비밀번호, Remember Me)
   * @return JWT Access Token (HttpOnly 쿠키용)
   * @throws AuthException 인증 실패 시
   */
  @Transactional
  public String loginWithCookie(LoginRequest request) {
    log.info("쿠키 로그인 처리 시작: email={}, rememberMe={}", request.email(), request.rememberMe());

    try {
      // 1. 사용자 인증
      UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(request.email(),
          request.password());

      Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);

      // 2. JWT 토큰 생성 (remember me 옵션 포함)
      TokenInfo tokenInfo = jwtTokenProvider.generateToken(authentication, request.rememberMe());

      // 3. DB에 Refresh Token 저장 (옵션)
      User user = userRepository
          .findByEmail(authentication.getName())
          .orElseThrow(() -> AuthException.invalidCredentials());
      // v6.1: 쿠키 로그인은 기본 1일 설정
      user.updateRefreshToken(
          tokenInfo.refreshToken(), LocalDateTime.now().plusDays(1), request.rememberMe());

      log.info("쿠키 로그인 완료: userId={}, email={}", user.getId(), user.getEmail());
      return tokenInfo.accessToken();

    } catch (BadCredentialsException e) {
      log.warn("쿠키 로그인 인증 실패: email={}", request.email());
      // v2.4 보안 정책: 모든 인증 실패를 AUTH_001로 통일
      throw AuthException.invalidCredentials();
    } catch (Exception e) {
      log.error("쿠키 로그인 처리 중 오류: email={}", request.email(), e);
      throw AuthException.invalidCredentials();
    }
  }

  /**
   * 사용자의 Refresh Token을 데이터베이스에서 삭제하여 로그아웃 처리함.
   *
   * <p>
   * 로그아웃 처리는 서버 측에서 Refresh Token을 무효화하는 것으로 이루어짐. Access Token은 클라이언트 측에서 삭제하고,
   * 서버에서는 만료 시까지
   * 유효함.
   *
   * <h3>처리 과정:</h3>
   *
   * <ol>
   * <li>사용자 이메일로 계정 조회
   * <li>해당 사용자의 Refresh Token을 null로 설정
   * <li>데이터베이스에 변경사항 반영
   * </ol>
   *
   * <h3>보안 고려사항:</h3>
   *
   * <ul>
   * <li>Refresh Token 무효화로 토큰 갱신 차단
   * <li>Access Token은 만료 시까지 유효하므로 클라이언트에서 삭제 필요
   * <li>완전한 보안을 위해서는 토큰 블랙리스트 구현 고려
   * </ul>
   *
   * @param email 로그아웃할 사용자의 이메일 주소
   * @throws AuthException 해당 이메일의 사용자를 찾을 수 없는 경우
   */
  @Transactional
  public void logout(String email) {
    log.info("로그아웃 처리 시작: email={}", email);

    User user = userRepository
        .findByEmail(email)
        .orElseThrow(
            () -> {
              log.warn("로그아웃 시 사용자 없음: email={}", email);
              return AuthException.invalidCredentials(); // v2.4: 사용자 열거 공격 방지
            });

    // Refresh Token 무효화
    user.clearRefreshToken();
    userRepository.save(user);

    log.info("로그아웃 완료: userId={}, email={}", user.getId(), user.getEmail());
  }

  /**
   * Refresh Token을 이용한 토큰 갱신 (API 명세서 v2.4 기준)
   *
   * @param refreshToken 갱신용 Refresh Token
   * @return 새로운 토큰 정보와 rememberMe 상태를 포함한 결과 객체
   * @throws AuthException 토큰이 유효하지 않은 경우
   */
  @Transactional
  public TokenRefreshResult refreshTokens(String refreshToken) {
    log.info("토큰 갱신 요청");

    try {
      // 1. 입력값 검증
      if (refreshToken == null || refreshToken.trim().isEmpty()) {
        log.warn("Refresh Token이 비어있음");
        throw AuthException.invalidToken();
      }

      // 2. JwtTokenProvider의 refreshAccessToken 메서드 사용
      // 이 메서드는 Refresh Token을 올바르게 처리하여 새로운 토큰과 부가 정보를 생성
      ProviderTokenRefreshResult providerResult = jwtTokenProvider.refreshAccessToken(refreshToken);

      log.info("토큰 갱신 완료");
      return new TokenRefreshResult(providerResult.tokenInfo(), providerResult.rememberMe());

    } catch (AuthException e) {
      // AuthException은 그대로 다시 던짐
      throw e;
    } catch (Exception e) {
      log.error("토큰 갱신 중 예상치 못한 오류 발생", e);
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
    return userRepository
        .findByEmail(email)
        .orElseThrow(
            () -> {
              log.debug("사용자 조회 실패: email={}", email);
              // v2.4 보안 정책: 사용자 존재 여부 노출 방지
              return AuthException.invalidCredentials();
            });
  }

  /**
   * JWT 토큰에서 이메일 추출 (v6.1)
   *
   * @param token JWT 토큰
   * @return 사용자 이메일
   * @throws AuthException 토큰이 유효하지 않은 경우
   */
  public String getEmailFromToken(String token) {
    try {
      Authentication authentication = jwtTokenProvider.getAuthentication(token);
      return authentication.getName();
    } catch (Exception e) {
      log.warn("토큰에서 이메일 추출 실패", e);
      throw AuthException.invalidToken();
    }
  }

  @Transactional
  public void completePhoneVerification(Long userId, String phoneNumber) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new AuthException(ErrorCode.USER_003));
    user.completePhoneVerification(phoneNumber);
    userRepository.save(user);
  }

  /**
   * 휴대폰 번호를 마스킹
   */
  public String maskPhoneNumber(String phoneNumber) {
    if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
      return "";
    }

    // 입력된 번호에서 모든 하이픈(-)을 제거
    String cleanedNumber = phoneNumber.replaceAll("-", "");

    if (cleanedNumber.length() != 11) {
      log.warn("Invalid phone number format after cleaning: {}", cleanedNumber);
      return "";
    }

    // 하이픈이 제거된 번호를 기준으로 마스킹을 수행
    return cleanedNumber.substring(0, 3) + "-"
        + cleanedNumber.substring(3, 4) + "***-***"
        + cleanedNumber.substring(10, 11);
  }

  /**
   * 비밀번호 재설정을 위한 임시 토큰을 생성
   */
  public String generatePasswordResetToken(String email) {

    User user = findUserByEmail(email);
    Authentication authentication = new UsernamePasswordAuthenticationToken(user.getEmail(), null, null);

    return jwtTokenProvider.generateToken(authentication, false).accessToken();
  }

  /**
   * 토큰을 검증하고 새로운 비밀번호로 재설정
   */
  @Transactional
  public void resetPassword(String resetToken, String newPassword) {
    // 토큰 유효성 검증
    if (!jwtTokenProvider.validateToken(resetToken)) {
      throw AuthException.invalidToken();
    }

    // 토큰에서 이메일 정보 추출
    String email = getEmailFromToken(resetToken);
    User user = findUserByEmail(email);

    // 새 비밀번호 암호화 및 저장
    user.updatePasswordHash(passwordEncoder.encode(newPassword));
    userRepository.save(user);

    log.info("사용자 비밀번호가 재설정되었습니다. userId={}", user.getId());
  }

}
