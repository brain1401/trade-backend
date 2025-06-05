package com.tradegenie.platform.tradegenie_backend_api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(authz -> authz
            .requestMatchers("/", "/api/status").permitAll() // 공개 경로
            .anyRequest().permitAll() // 개발용 : 모든 요청 허용
        )
        .csrf(csrf -> csrf.disable()); // CSRF 비활성화

    return http.build();
  }
}