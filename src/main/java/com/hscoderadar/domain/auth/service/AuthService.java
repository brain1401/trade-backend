package com.hscoderadar.domain.auth.service;

import com.hscoderadar.config.jwt.JwtTokenProvider;
import com.hscoderadar.config.jwt.JwtTokenProvider.TokenInfo;
import com.hscoderadar.config.oauth.PrincipalDetails;
import com.hscoderadar.domain.auth.dto.request.LoginRequest;
import com.hscoderadar.domain.auth.dto.request.SignUpRequest;
import com.hscoderadar.domain.users.entity.User;
import com.hscoderadar.domain.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;

    /**
     * 회원가입을 처리합니다.
     * @param request 회원가입 요청 DTO
     * @return 저장된 User 엔티티
     */
    @Transactional
    public User signUp(SignUpRequest request) {
        // 이메일 중복 확인
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }
        
        // DTO를 Entity로 변환 (비밀번호 암호화 포함)
        User newUser = request.toEntity(passwordEncoder);
        
        return userRepository.save(newUser);
    }

    /**
     * 로그인을 처리하고 JWT를 발급합니다.
     * @param request 로그인 요청 DTO
     * @return 발급된 토큰 정보
     */
    @Transactional
    public TokenInfo login(LoginRequest request) {
        // 1. Login ID/PW 를 기반으로 Authentication 객체 생성
        // 이때 authentication은 인증 여부를 확인하는 authenticated 값이 false
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword());

        // 2. 실제 검증 (사용자 비밀번호 체크)이 이루어지는 부분
        // authenticate 매서드가 실행될 때 CustomUserDetailsService 에서 만든 loadUserByUsername 메서드가 실행
        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);

        // 3. 인증 정보를 기반으로 JWT 토큰 생성
        TokenInfo tokenInfo = jwtTokenProvider.generateToken(authentication);

        // 4. DB에 Refresh Token 저장
        User user = userRepository.findByEmail(authentication.getName())
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        user.setRefreshToken(tokenInfo.refreshToken()); 
        

        return tokenInfo;
    }

    /**
     * 사용자의 리프레시 토큰을 DB에서 삭제하여 로그아웃 처리합니다.
     * @param email 로그아웃할 사용자의 이메일
     */
    @Transactional
    public void logout(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("로그아웃 처리 중 사용자를 찾을 수 없습니다."));
        user.setRefreshToken(null); // 리프레시 토큰을 null로 업데이트
    }

    /**
     * 전달받은 Refresh Token을 사용하여 새로운 Access Token과 Refresh Token을 발급합니다. (토큰 회전)
     * @param refreshToken 유효한 Refresh Token
     * @return 새로운 토큰 정보
     */
    @Transactional
    public TokenInfo refreshTokens(String refreshToken) {
        // 1. Refresh Token 유효성 검증
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않은 리프레시 토큰입니다.");
        }

        // 2. DB에서 해당 리프레시 토큰을 가진 사용자 정보 조회
        User user = userRepository.findByRefreshToken(refreshToken) // 이 메서드는 UserRepository에 추가해야 합니다.
                .orElseThrow(() -> new IllegalArgumentException("리프레시 토큰에 해당하는 사용자를 찾을 수 없습니다."));

        // 3. 새로운 토큰 생성
        // 사용자 정보로 Authentication 객체를 다시 만들어 토큰을 생성합니다.
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user.getEmail(), null, new PrincipalDetails(user).getAuthorities());
        TokenInfo newTokenInfo = jwtTokenProvider.generateToken(authentication);

        // 4. DB에 새로운 Refresh Token으로 업데이트 (토큰 회전)
        user.setRefreshToken(newTokenInfo.refreshToken());

        return newTokenInfo;
    }
}