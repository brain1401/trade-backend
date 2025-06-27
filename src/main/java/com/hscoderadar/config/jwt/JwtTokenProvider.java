package com.hscoderadar.config.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.hscoderadar.domain.auth.service.CustomUserDetailsService;
import com.hscoderadar.domain.user.entity.User;
import com.hscoderadar.domain.user.repository.UserRepository;
import com.hscoderadar.common.exception.AuthException;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * JWT 토큰 생성 및 검증을 담당하는 Provider 클래스
 * 
 * v6.1 요구사항:
 * - Access Token: 30분
 * - Refresh Token: remember me 체크시 30일, 미체크시 1일
 * - Access Token은 JSON 반환, Refresh Token은 HttpOnly 쿠키 저장
 * - users 테이블의 JWT 관련 컬럼 연동 완료
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtTokenProvider {

    private final CustomUserDetailsService customUserDetailsService;
    private final JwtProperties jwtProperties;
    private final UserRepository userRepository;
    private final JwtRedisService jwtRedisService;

    private SecretKey key;

    @PostConstruct
    public void init() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.getSecretKey());
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Provider 내부에서 사용할 토큰 갱신 결과 DTO
     */
    public record ProviderTokenRefreshResult(TokenInfo tokenInfo, boolean rememberMe) {
    }

    /**
     * 인증 정보를 기반으로 액세스 토큰과 리프레시 토큰을 포함하는 TokenInfo를 생성합니다.
     * (기본값: remember me = false)
     *
     * @param authentication Spring Security의 인증 정보
     * @return 생성된 토큰 정보를 담은 DTO
     */
    @Transactional
    public TokenInfo generateToken(Authentication authentication) {
        return generateToken(authentication, false);
    }

    /**
     * 인증 정보와 remember me 옵션을 기반으로 토큰을 생성합니다.
     * 
     * v6.1 요구사항에 따른 차별화된 토큰 생성:
     * - Access Token: 항상 30분
     * - Refresh Token: remember me 체크시 30일, 미체크시 1일
     * - users 테이블의 refresh_token, refresh_token_expires_at 업데이트
     *
     * @param authentication Spring Security의 인증 정보
     * @param rememberMe     remember me 체크 여부
     * @return 생성된 토큰 정보를 담은 DTO
     */
    @Transactional
    public TokenInfo generateToken(Authentication authentication, boolean rememberMe) {
        log.debug("토큰 생성 시작: user={}, rememberMe={}", authentication.getName(), rememberMe);

        // 권한 가져오기
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        long now = (new Date()).getTime();

        // Access Token 생성 (항상 30분, JTI 포함)
        Date accessTokenExpiresIn = new Date(now + jwtProperties.getAccessTokenExpirationMs());
        String accessTokenJti = UUID.randomUUID().toString();
        String accessToken = Jwts.builder()
                .subject(authentication.getName())
                .claim("auth", authorities)
                .claim("jti", accessTokenJti)
                .expiration(accessTokenExpiresIn)
                .signWith(key, Jwts.SIG.HS256)
                .compact();

        // Refresh Token 생성 (remember me에 따라 차별화, JTI 포함)
        long refreshTokenExpiration = rememberMe ? jwtProperties.getRefreshTokenLongExpirationMs()
                : jwtProperties.getRefreshTokenShortExpirationMs();

        String refreshTokenJti = UUID.randomUUID().toString();
        String refreshToken = Jwts.builder()
                .subject(authentication.getName())
                .claim("jti", refreshTokenJti)
                .expiration(new Date(now + refreshTokenExpiration))
                .signWith(key, Jwts.SIG.HS256)
                .compact();

        // users 테이블의 JWT 관련 컬럼 업데이트
        updateUserRefreshTokenInfo(authentication.getName(), refreshToken,
                new Date(now + refreshTokenExpiration), rememberMe);

        // Redis에 토큰 발급 기록
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> AuthException.userNotFound());
        jwtRedisService.logTokenIssue(user.getId(), true); // Access Token
        jwtRedisService.logTokenIssue(user.getId(), false); // Refresh Token

        log.debug("토큰 생성 완료: AccessToken 만료={}분, RefreshToken 만료={}일",
                jwtProperties.getAccessTokenExpirationMs() / (1000 * 60),
                refreshTokenExpiration / (1000 * 60 * 60 * 24));

        return new TokenInfo("Bearer", accessToken, refreshToken);
    }

    /**
     * v6.1 요구사항: users 테이블의 JWT 관련 컬럼 업데이트
     * 
     * @param email        사용자 이메일
     * @param refreshToken 새로운 리프레시 토큰
     * @param expiresAt    토큰 만료 시간
     * @param rememberMe   remember me 설정
     */
    private void updateUserRefreshTokenInfo(String email, String refreshToken,
            Date expiresAt, boolean rememberMe) {
        try {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + email));

            user.updateRefreshToken(refreshToken,
                    expiresAt.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime(),
                    rememberMe);

            userRepository.save(user);

            log.debug("users 테이블 JWT 정보 업데이트 완료: email={}, rememberMe={}", email, rememberMe);
        } catch (Exception e) {
            log.error("users 테이블 JWT 정보 업데이트 실패: email={}, error={}", email, e.getMessage());
            throw new RuntimeException("JWT 정보 업데이트 실패", e);
        }
    }

    /**
     * v6.1 요구사항: Refresh Token을 이용한 Access Token 갱신
     * 
     * @param refreshToken 리프레시 토큰
     * @return 새로운 Access Token 정보
     */
    @Transactional
    public ProviderTokenRefreshResult refreshAccessToken(String refreshToken) {
        log.debug("Access Token 갱신 시작");

        try {
            // 1. Refresh Token 유효성 검증
            if (!validateToken(refreshToken)) {
                log.warn("유효하지 않은 Refresh Token");
                throw AuthException.invalidToken();
            }

            // 2. Refresh Token에서 사용자 정보 추출
            Claims claims = parseClaims(refreshToken);
            String userEmail = claims.getSubject();

            // 3. users 테이블에서 refresh token 일치 확인
            User user = userRepository.findByValidRefreshToken(refreshToken, LocalDateTime.now())
                    .orElseThrow(() -> {
                        log.warn("DB에 저장된 Refresh Token과 불일치하거나 만료됨: email={}", userEmail);
                        return AuthException.invalidToken();
                    });

            // 3.1 사용자의 실제 권한 정보 가져오기
            UserDetails userDetails = customUserDetailsService.loadUserByUsername(user.getEmail());
            String authorities = userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.joining(","));

            // 4. Redis에 갱신 진행 상태 저장
            jwtRedisService.setRefreshInProgress(user.getId(), refreshToken, null, null, user.getRememberMeEnabled());

            // 5. 새로운 Access Token 생성
            long now = System.currentTimeMillis();
            Date accessTokenExpiresIn = new Date(now + jwtProperties.getAccessTokenExpirationMs());
            String accessTokenJti = UUID.randomUUID().toString();

            String newAccessToken = Jwts.builder()
                    .subject(userEmail)
                    .claim("auth", authorities) // 하드코딩된 ROLE_USER 대신 실제 권한 사용
                    .claim("jti", accessTokenJti)
                    .expiration(accessTokenExpiresIn)
                    .signWith(key, Jwts.SIG.HS256)
                    .compact();

            // 6. 새로운 Refresh Token 생성 (기존 설정 유지)
            boolean rememberMe = user.getRememberMeEnabled();
            long refreshTokenExpiration = rememberMe
                    ? jwtProperties.getRefreshTokenLongExpirationMs()
                    : jwtProperties.getRefreshTokenShortExpirationMs();

            String refreshTokenJti = UUID.randomUUID().toString();
            String newRefreshToken = Jwts.builder()
                    .subject(userEmail)
                    .claim("jti", refreshTokenJti)
                    .expiration(new Date(now + refreshTokenExpiration))
                    .signWith(key, Jwts.SIG.HS256)
                    .compact();

            // 7. users 테이블 업데이트
            updateUserRefreshTokenInfo(userEmail, newRefreshToken,
                    new Date(now + refreshTokenExpiration), user.getRememberMeEnabled());

            // 8. 기존 토큰들을 블랙리스트에 추가
            String oldAccessTokenJti = extractJti(refreshToken);
            if (oldAccessTokenJti != null) {
                jwtRedisService.addToBlacklist(oldAccessTokenJti, "TOKEN_REFRESH", user.getId(),
                        jwtProperties.getAccessTokenExpirationMs() / 1000);
            }

            String oldRefreshTokenJti = extractJti(refreshToken);
            if (oldRefreshTokenJti != null) {
                jwtRedisService.addToBlacklist(oldRefreshTokenJti, "TOKEN_REFRESH", user.getId(),
                        refreshTokenExpiration / 1000);
            }

            // 9. Redis 갱신 상태 업데이트
            jwtRedisService.setRefreshInProgress(user.getId(), refreshToken, newRefreshToken, newAccessToken,
                    user.getRememberMeEnabled());

            // 10. 토큰 발급 기록
            jwtRedisService.logTokenIssue(user.getId(), true); // 새로운 Access Token

            // 11. Redis 갱신 진행 상태 정리
            jwtRedisService.clearRefreshInProgress(user.getId());

            log.info("Access Token 갱신 완료: email={}, rememberMe={}", userEmail, user.getRememberMeEnabled());

            TokenInfo newTokenInfo = new TokenInfo("Bearer", newAccessToken, newRefreshToken);
            return new ProviderTokenRefreshResult(newTokenInfo, rememberMe);

        } catch (Exception e) {
            log.error("Access Token 갱신 실패", e);
            throw AuthException.tokenRefreshFailed();
        }
    }

    /**
     * JWT 토큰을 복호화하여 토큰에 포함된 인증 정보를 반환합니다.
     *
     * @param accessToken 인증 정보를 추출할 액세스 토큰
     * @return Spring Security의 인증 정보(Authentication)
     */
    public Authentication getAuthentication(String accessToken) {
        // 토큰 복호화
        Claims claims = parseClaims(accessToken);

        if (claims.get("auth") == null) {
            throw new RuntimeException("권한 정보가 없는 토큰입니다.");
        }

        // 클레임에서 권한 정보 가져오기
        // 클레임에서 유저 정보(이메일)를 꺼내 CustomUserDetailsService를 통해 UserDetails를 로드합니다.
        // 이렇게 하면 PrincipalDetails 타입의 객체가 반환됩니다.
        UserDetails principal = customUserDetailsService.loadUserByUsername(claims.getSubject());

        // 로드된 UserDetails(PrincipalDetails)를 사용하여 Authentication 객체를 생성합니다.
        return new UsernamePasswordAuthenticationToken(principal, "", principal.getAuthorities());
    }

    /**
     * 토큰 정보를 검증합니다.
     *
     * @param token 검증할 JWT
     * @return 토큰이 유효하면 true, 아니면 false
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);

            // 블랙리스트 검증
            String jti = extractJti(token);
            if (jti != null && jwtRedisService.isTokenBlacklisted(jti)) {
                log.warn("블랙리스트된 토큰 사용 시도: jti={}", jti);
                return false;
            }

            return true;
        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
            log.info("Invalid JWT Token", e);
        } catch (ExpiredJwtException e) {
            log.info("Expired JWT Token", e);
        } catch (UnsupportedJwtException e) {
            log.info("Unsupported JWT Token", e);
        } catch (IllegalArgumentException e) {
            log.info("JWT claims string is empty.", e);
        }
        return false;
    }

    /**
     * v6.1 요구사항: 토큰 블랙리스트 검증
     * 
     * @param token 검증할 JWT 토큰
     * @return 블랙리스트에 있으면 true, 없으면 false
     */
    public boolean isTokenBlacklisted(String token) {
        String jti = extractJti(token);
        return jti != null && jwtRedisService.isTokenBlacklisted(jti);
    }

    /**
     * v6.1 요구사항: 토큰 블랙리스트 추가
     * 
     * @param token  블랙리스트에 추가할 토큰
     * @param reason 블랙리스트 사유
     */
    @Transactional
    public void addToBlacklist(String token, String reason) {
        try {
            String jti = extractJti(token);
            if (jti == null) {
                log.warn("JTI가 없는 토큰을 블랙리스트에 추가할 수 없음");
                return;
            }

            Claims claims = parseClaims(token);
            String userEmail = claims.getSubject();
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> AuthException.userNotFound());

            // 토큰 만료 시간까지의 TTL 계산
            Date expiration = claims.getExpiration();
            long ttlSeconds = Math.max(0, (expiration.getTime() - System.currentTimeMillis()) / 1000);

            jwtRedisService.addToBlacklist(jti, reason, user.getId(), ttlSeconds);

            log.info("토큰 블랙리스트 추가 완료: jti={}, reason={}, userId={}", jti, reason, user.getId());
        } catch (Exception e) {
            log.error("토큰 블랙리스트 추가 실패: reason={}, error={}", reason, e.getMessage());
        }
    }

    /**
     * 토큰에서 JTI(JWT ID) 추출
     * 
     * @param token JWT 토큰
     * @return JTI 값 또는 null
     */
    private String extractJti(String token) {
        try {
            Claims claims = parseClaims(token);
            return claims.get("jti", String.class);
        } catch (Exception e) {
            log.warn("JTI 추출 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 액세스 토큰을 파싱하여 클레임(Payload)을 추출합니다.
     *
     * @param accessToken 파싱할 액세스 토큰
     * @return 토큰의 클레임 정보
     */
    private Claims parseClaims(String accessToken) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(accessToken)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            // 토큰이 만료되었더라도 클레임 정보는 필요할 수 있으므로 반환
            return e.getClaims();
        }
    }

    /**
     * 토큰 정보를 담을 DTO 클래스
     */
    public record TokenInfo(String grantType, String accessToken, String refreshToken) {
    }
}