package com.hscoderadar.config.oauth;

import com.hscoderadar.config.jwt.JwtTokenProvider;
import com.hscoderadar.config.jwt.JwtTokenProvider.TokenInfo;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        log.info("OAuth2 Login 성공!");
        
        // JWT 토큰 발급
        TokenInfo tokenInfo = jwtTokenProvider.generateToken(authentication);

        // 테스트를 위해 토큰을 쿼리 파라미터로 전달
        String targetUrl = UriComponentsBuilder.fromUriString("/api/main-page") 
            .queryParam("accessToken", tokenInfo.accessToken())
            .queryParam("refreshToken", tokenInfo.refreshToken())
            .build().toUriString();

         response.sendRedirect(targetUrl);
    }
}