package com.hscoderadar.config;

import com.hscoderadar.config.jwt.JwtAuthenticationFilter;
import com.hscoderadar.config.jwt.JwtTokenProvider;
import com.hscoderadar.config.oauth.CustomOAuth2UserService;
import com.hscoderadar.config.oauth.OAuth2LoginSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * JWT 기반 통합 인증 시스템을 위한 Spring Security 보안 설정
 * 
 * <p>
 * 이 클래스는 Public API와 Private API를 구분하여 차별화된 보안 정책을 적용하는
 * JWT 기반 인증 시스템을 구현합니다:
 * <ul>
 * <li>HttpOnly 쿠키 기반 JWT 토큰 관리</li>
 * <li>Public API (인증 선택) + Private API (인증 필수) 구분</li>
 * <li>OAuth2 소셜 로그인 통합 (Google, Naver, Kakao)</li>
 * <li>XSS/CSRF 완전 차단 보안 정책</li>
 * <li>세션리스(Stateless) 아키텍처</li>
 * </ul>
 * 
 * <p>
 * <strong>인증 시스템 설계 원칙:</strong>
 * <ul>
 * <li>검색/분석 API: 로그인 없이 사용 가능, 로그인 시 개인화 기능 추가</li>
 * <li>북마크/대시보드 API: 로그인 필수</li>
 * <li>JWT 토큰: HttpOnly 쿠키로만 관리 (JavaScript 접근 불가)</li>
 * <li>프론트엔드 분리형 SPA 아키텍처 지원</li>
 * </ul>
 * 
 * @author HsCodeRadar Team
 * @since 2.1.0
 * @see JwtAuthenticationFilter
 * @see OAuth2LoginSuccessHandler
 * @see CustomOAuth2UserService
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final JwtTokenProvider jwtTokenProvider;
  private final CustomOAuth2UserService customOAuth2UserService;
  private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

  /**
   * Spring Security의 인증 관리자를 Bean으로 등록
   * 
   * <p>
   * AuthenticationManager는 사용자 인증 처리의 핵심 컴포넌트로,
   * 로그인 시 사용자 자격증명(이메일/비밀번호)을 검증하는 역할을 담당합니다.
   * 
   * @param authenticationConfiguration Spring Security 자동 구성 설정
   * @return 구성된 AuthenticationManager Bean
   * @throws Exception 인증 관리자 구성 실패 시
   */
  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
      throws Exception {
    return authenticationConfiguration.getAuthenticationManager();
  }

  /**
   * 비밀번호 암호화를 위한 BCrypt 인코더 Bean 등록
   * 
   * <p>
   * BCryptPasswordEncoder는 단방향 해시 함수를 사용하여 비밀번호를 안전하게 암호화합니다.
   * Salt를 자동으로 생성하여 동일한 비밀번호라도 서로 다른 해시값을 생성합니다.
   * 
   * @return BCryptPasswordEncoder 인스턴스
   */
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  /**
   * 개발 환경을 위한 간단한 CORS 설정
   * 
   * <p>
   * 개발 단계에서는 복잡한 CORS 정책보다는 간단하고 명확한 설정을 사용합니다.
   * 모든 API에 대해 localhost:3000에서의 접근을 허용하며, HttpOnly 쿠키를 지원합니다.
   * 
   * @return 구성된 CORS 설정 소스
   */
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

    // 개발 환경용 단일 CORS 설정
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(Arrays.asList(
        "http://localhost:3000", // React 개발 서버
        "http://localhost:3001", // 추가 개발 포트
        "http://127.0.0.1:3000" // 로컬 IP 접근
    ));
    config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
    config.setAllowedHeaders(Arrays.asList("*"));
    config.setAllowCredentials(true); // HttpOnly 쿠키 지원
    config.setMaxAge(3600L); // preflight 캐시 1시간
    config.setExposedHeaders(Arrays.asList("Authorization", "Set-Cookie"));

    // 모든 API 경로에 동일한 CORS 정책 적용
    source.registerCorsConfiguration("/**", config);

    return source;
  }

  /**
   * JWT 기반 인증 시스템을 위한 메인 보안 필터 체인 구성
   * 
   * <p>
   * Public API와 Private API를 구분하여 차별화된 보안 정책을 적용하며,
   * HttpOnly 쿠키 기반 JWT 인증과 OAuth2 로그인을 통합 지원합니다.
   * 
   * <h3>URL별 접근 정책:</h3>
   * <ul>
   * <li><strong>완전 공개:</strong> 홈페이지, 정적 파일, 헬스체크</li>
   * <li><strong>Public API:</strong> 검색/분석 기능 (로그인 선택적)</li>
   * <li><strong>인증 API:</strong> 로그인/회원가입/OAuth (공개)</li>
   * <li><strong>Private API:</strong> 북마크/대시보드 (로그인 필수)</li>
   * </ul>
   * 
   * <h3>보안 특징:</h3>
   * <ul>
   * <li>CSRF 방지: SameSite=Strict 쿠키 정책</li>
   * <li>XSS 방지: HttpOnly 쿠키로 토큰 관리</li>
   * <li>세션리스: JWT 기반 무상태 인증</li>
   * <li>OAuth2: 소셜 로그인 통합 지원</li>
   * </ul>
   * 
   * @param http HttpSecurity 구성 객체
   * @return 구성된 SecurityFilterChain
   * @throws Exception 보안 설정 구성 실패 시
   */
  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        // CORS 설정 적용
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))

        // 기본 보안 기능 조정
        .csrf(AbstractHttpConfigurer::disable) // API 서버에서는 CSRF 비활성화 (SameSite로 대체)
        .httpBasic(AbstractHttpConfigurer::disable) // HTTP Basic 인증 비활성화
        .formLogin(AbstractHttpConfigurer::disable) // Form 로그인 비활성화 (JWT 기반)

        // 세션리스 정책: JWT 토큰 기반 무상태 인증
        .sessionManagement(session -> session
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

        // JWT 기반 인증을 위한 URL별 접근 권한 설정
        .authorizeHttpRequests(authz -> authz
            // 완전 공개 접근 (인증 불필요)
            .requestMatchers(
                "/", "/error", // 기본 페이지
                "/favicon.ico", "/*.html", // 정적 파일
                "/status", "/health", // 헬스체크
                "/h2-console/**" // 개발용 DB 콘솔
            ).permitAll()

            // CORS preflight 요청 (OPTIONS) 허용
            .requestMatchers("OPTIONS", "/**").permitAll()

            // Public API: 검색/분석 기능 (로그인 선택적)
            .requestMatchers(
                "/api/search/**", // 모든 검색 및 분석 API
                "/api/chat/**" // ChatGPT 스타일 통합 채팅 API
            ).permitAll()

            // 인증 관련 API (공개) - CORS 설정에서 Private API로 분류되어 있음
            .requestMatchers(
                "/api/auth/register", // 회원가입
                "/api/auth/login", // 로그인
                "/api/auth/verify", // 인증 상태 확인
                "/api/auth/logout", // 로그아웃
                "/api/auth/refresh" // 토큰 갱신
            ).permitAll()

            // OAuth2 관련 경로 (Spring Security 자동 처리)
            .requestMatchers(
                "/oauth2/**", // OAuth2 인증 과정
                "/login/oauth2/code/**" // OAuth2 콜백
            ).permitAll()

            // Private API: 인증 필수
            .requestMatchers(
                "/api/bookmarks/**", // 북마크 관리
                "/api/dashboard/**", // 대시보드
                "/api/notifications/**", // 알림
                "/api/sms/**", // SMS 알림 시스템
                "/api/admin/**" // 관리자 기능
            ).authenticated()

            // 나머지 모든 요청은 인증 필요
            .anyRequest().authenticated())

        // OAuth2 로그인 설정
        .oauth2Login(oauth2 -> oauth2
            // 로그인 성공 시 HttpOnly 쿠키 설정 및 프론트엔드 콜백
            .successHandler(oAuth2LoginSuccessHandler)
            // 소셜 로그인 사용자 정보 처리
            .userInfoEndpoint(userInfo -> userInfo
                .userService(customOAuth2UserService))
            // OAuth2 인증 URL 패턴: /oauth2/authorization/{provider}
            .authorizationEndpoint(auth -> auth.baseUri("/oauth2/authorization")))

        // H2 콘솔을 위한 프레임 옵션 설정 (개발 환경에서만)
        .headers(headers -> headers
            .frameOptions(frameOptions -> frameOptions.sameOrigin())) // H2 콘솔이 iframe에서 실행될 수 있도록 허용

        // JWT 인증 필터 추가
        .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider),
            UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }
}