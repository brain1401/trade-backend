package com.hscoderadar.domain.auth.controller;

import com.hscoderadar.common.response.ApiResponseMessage;
import com.hscoderadar.config.jwt.JwtTokenProvider.TokenInfo;
import com.hscoderadar.domain.auth.dto.request.LoginRequest;
import com.hscoderadar.domain.auth.dto.request.SignUpRequest;
import com.hscoderadar.domain.auth.service.AuthService;
import com.hscoderadar.domain.users.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 자체 회원가입 API
     * @param request 회원가입 정보
     * @return 성공 메시지
     */
    @PostMapping("/auth/register")
    @ApiResponseMessage("회원가입이 성공적으로 완료되었습니다.")
    public String signUp(@RequestBody SignUpRequest request) {
        User savedUser = authService.signUp(request);
        // 회원가입 성공 후 별도의 데이터를 반환할 필요가 없다면, 메시지만으로도 충분합니다.
        // 여기서는 간단히 이메일만 반환하도록 처리했습니다.
        return "가입된 이메일: " + savedUser.getEmail();
    }

    /**
     * 자체 로그인 API
     * @param request 로그인 정보
     * @return JWT 토큰 정보
     */
    @PostMapping("/auth/login")
    @ApiResponseMessage("로그인에 성공했습니다.")
    public TokenInfo login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }
}