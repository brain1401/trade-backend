package com.hscoderadar.config.oauth;

import com.hscoderadar.config.jwt.JwtTokenProvider;
import com.hscoderadar.config.jwt.JwtTokenProvider.TokenInfo;
import com.hscoderadar.domain.users.entity.User;
import com.hscoderadar.domain.users.repository.UserRepository;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        log.info("OAuth2 Login 성공!");
        
        // JWT 토큰 발급
        TokenInfo tokenInfo = jwtTokenProvider.generateToken(authentication);

        // DB에 Refresh Token 저장
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("OAuth2 인증 후 사용자를 찾을 수 없습니다."));
        user.setRefreshToken(tokenInfo.refreshToken());

        // 테스트를 위해 토큰을 쿼리 파라미터로 전달
        String targetUrl = UriComponentsBuilder.fromUriString("/api/main-page") 
            .queryParam("accessToken", tokenInfo.accessToken())
            .queryParam("refreshToken", tokenInfo.refreshToken())
            .build().toUriString();

         response.sendRedirect(targetUrl);
    }
}