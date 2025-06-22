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

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // CORS 설정을 명시적으로 추가 (Best Practice)
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // 실제 프론트엔드 주소를 정확히 명시하는 것이 더 안전합니다.
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000", "http://localhost:8081"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CORS 설정 적용
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                
                // 기본 설정 비활성화
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                
                // formLogin을 비활성화하는 대신, 로그인 페이지를 명시적으로 지정
                .formLogin(form -> form.loginPage("/login-page").permitAll())
                
                // 세션을 사용하지 않음 (STATELESS)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                
                // 요청 경로별 권한 설정
                .authorizeHttpRequests(authz -> authz
                // API 경로와 실제 HTML 파일 경로를 모두 허용합니다.
                .requestMatchers(
                        "/", "/error",
                        "/login-page", "/signup-page", "/main-page",
                        "/*.html", // static 폴더의 모든 html 파일 허용
                        "/status"
                ).permitAll()
                .requestMatchers("/login/oauth2/code/**", "/oauth2/**").permitAll()
                .requestMatchers("/auth/login", "/auth/register", "/auth/refresh").permitAll()
                .anyRequest().authenticated()
            )
                
                // OAuth2 로그인 설정
                .oauth2Login(oauth2 -> oauth2
                        // 로그인 성공 후 처리할 핸들러 등록
                        .successHandler(oAuth2LoginSuccessHandler)
                        // 사용자 정보를 가져올 때 사용할 서비스 등록
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                )
                
                // JWT 필터를 UsernamePasswordAuthenticationFilter 앞에 추가
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}