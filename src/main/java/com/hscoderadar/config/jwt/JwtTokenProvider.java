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
import com.hscoderadar.domain.auth.service.CustomUserDetailsService;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtTokenProvider {

    private final CustomUserDetailsService customUserDetailsService;
    private final JwtProperties jwtProperties;
    
    private SecretKey key;

    @PostConstruct
    public void init() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.getSecretKey());
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 인증 정보를 기반으로 액세스 토큰과 리프레시 토큰을 포함하는 TokenInfo를 생성합니다.
     *
     * @param authentication Spring Security의 인증 정보
     * @return 생성된 토큰 정보를 담은 DTO
     */
    public TokenInfo generateToken(Authentication authentication) {
        // 권한 가져오기
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        long now = (new Date()).getTime();

        // Access Token 생성
        Date accessTokenExpiresIn = new Date(now + jwtProperties.getAccessTokenExpirationMs());
        String accessToken = Jwts.builder()
                .subject(authentication.getName())
                .claim("auth", authorities)
                .expiration(accessTokenExpiresIn)
                .signWith(key, Jwts.SIG.HS256)
                .compact();

        // Refresh Token 생성
        String refreshToken = Jwts.builder()
                .expiration(new Date(now + jwtProperties.getRefreshTokenExpirationMs()))
                .signWith(key, Jwts.SIG.HS256)
                .compact();

        return new TokenInfo("Bearer", accessToken, refreshToken);
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