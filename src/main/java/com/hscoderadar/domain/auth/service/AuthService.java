package com.hscoderadar.domain.auth.service;

import com.hscoderadar.config.jwt.JwtTokenProvider;
import com.hscoderadar.config.jwt.JwtTokenProvider.TokenInfo;
import com.hscoderadar.config.oauth.PrincipalDetails;
import com.hscoderadar.domain.auth.dto.request.LoginRequest;
import com.hscoderadar.domain.auth.dto.request.SignUpRequest;
import com.hscoderadar.domain.users.entity.User;
import com.hscoderadar.domain.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 인증 관련 비즈니스 로직을 처리하는 서비스 클래스입니다.
 * 
 * <p>
 * 이 서비스는 다음과 같은 인증 관련 기능을 제공합니다:
 * <ul>
 * <li>회원가입 처리 및 비밀번호 암호화</li>
 * <li>로그인 인증 및 JWT 토큰 발급</li>
 * <li>로그아웃 처리 및 토큰 무효화</li>
 * <li>Refresh Token을 이용한 토큰 갱신</li>
 * <li>사용자 조회 및 검증</li>
 * </ul>
 * 
 * <p>
 * <strong>보안 특징:</strong>
 * <ul>
 * <li>비밀번호는 BCrypt 알고리즘으로 암호화</li>
 * <li>JWT Access Token (1시간 유효) + Refresh Token (14일 유효) 사용</li>
 * <li>Token Rotation 방식으로 보안 강화</li>
 * <li>데이터베이스 기반 Refresh Token 관리</li>
 * </ul>
 * 
 * <p>
 * <strong>트랜잭션 처리:</strong>
 * <ul>
 * <li>클래스 레벨에서 읽기 전용 트랜잭션이 기본 설정</li>
 * <li>데이터 변경이 필요한 메서드는 {@code @Transactional} 어노테이션으로 개별 설정</li>
 * </ul>
 * 
 * @author HsCodeRadar Team
 * @since 1.0.0
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

    /**
     * 새로운 사용자 계정을 생성하고 데이터베이스에 저장합니다.
     * 
     * <p>
     * 회원가입 과정에서 다음과 같은 처리를 수행합니다:
     * <ol>
     * <li>이메일 중복 검사</li>
     * <li>비밀번호 BCrypt 암호화</li>
     * <li>사용자 엔티티 생성 및 저장</li>
     * </ol>
     * 
     * <h3>보안 고려사항:</h3>
     * <ul>
     * <li>비밀번호는 BCrypt로 단방향 암호화되어 저장</li>
     * <li>이메일 중복 시 즉시 예외 발생</li>
     * <li>가입 유형은 자동으로 {@code SELF}로 설정</li>
     * </ul>
     * 
     * @param request 회원가입 요청 정보 (이메일, 비밀번호, 이름)
     * @return 생성된 사용자 엔티티 (ID 포함)
     * @throws IllegalArgumentException                                이메일이 이미 사용 중인
     *                                                                 경우
     * @throws org.springframework.dao.DataIntegrityViolationException 데이터 무결성 위반 시
     * 
     * @see SignUpRequest#toEntity(PasswordEncoder)
     * @see User.RegistrationType#SELF
     */
    @Transactional
    public User signUp(SignUpRequest request) {
        log.info("회원가입 처리 시작: email={}", request.getEmail());

        // 이메일 중복 확인
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("이메일 중복 시도: email={}", request.getEmail());
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        // 비밀번호 검증 (자체 회원가입 시 필수)
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("비밀번호는 필수입니다.");
        }

        // DTO를 Entity로 변환 (비밀번호 암호화 포함)
        User newUser = request.toEntity(passwordEncoder);

        // 추가 검증: 비밀번호 해시가 올바르게 생성되었는지 확인
        if (newUser.getPasswordHash() == null || newUser.getPasswordHash().trim().isEmpty()) {
            throw new IllegalStateException("비밀번호 암호화 처리에 실패했습니다.");
        }

        User savedUser = userRepository.save(newUser);

        log.info("회원가입 완료: userId={}, email={}, registrationType={}",
                savedUser.getId(), savedUser.getEmail(), savedUser.getRegistrationType());
        return savedUser;
    }

    /**
     * 사용자 로그인을 처리하고 JWT 토큰을 발급합니다.
     * 
     * <p>
     * 로그인 과정은 다음 단계로 진행됩니다:
     * <ol>
     * <li>사용자 인증 정보 검증 (이메일/비밀번호)</li>
     * <li>Spring Security 인증 처리</li>
     * <li>JWT Access Token 및 Refresh Token 생성</li>
     * <li>Refresh Token을 데이터베이스에 저장</li>
     * </ol>
     * 
     * <h3>토큰 정보:</h3>
     * <ul>
     * <li><strong>Access Token:</strong> API 호출 시 사용, 1시간 유효</li>
     * <li><strong>Refresh Token:</strong> Access Token 갱신용, 14일 유효</li>
     * </ul>
     * 
     * <h3>보안 특징:</h3>
     * <ul>
     * <li>Spring Security의 {@code AuthenticationManager}를 통한 인증</li>
     * <li>비밀번호는 BCrypt로 검증</li>
     * <li>인증 실패 시 상세 정보 노출 방지</li>
     * </ul>
     * 
     * @param request 로그인 요청 정보 (이메일, 비밀번호)
     * @return JWT 토큰 정보 (Access Token, Refresh Token 포함)
     * @throws org.springframework.security.authentication.BadCredentialsException 인증
     *                                                                             실패
     *                                                                             시
     * @throws IllegalArgumentException                                            사용자를
     *                                                                             찾을
     *                                                                             수
     *                                                                             없는
     *                                                                             경우
     * 
     * @see LoginRequest
     * @see TokenInfo
     * @see CustomUserDetailsService#loadUserByUsername(String)
     */
    @Transactional
    public TokenInfo login(LoginRequest request) {
        log.info("로그인 처리 시작: email={}", request.getEmail());

        // 1. Login ID/PW 를 기반으로 Authentication 객체 생성
        // 이때 authentication은 인증 여부를 확인하는 authenticated 값이 false
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                request.getEmail(), request.getPassword());

        // 2. 실제 검증 (사용자 비밀번호 체크)이 이루어지는 부분
        // authenticate 매서드가 실행될 때 CustomUserDetailsService 에서 만든 loadUserByUsername
        // 메서드가 실행
        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);

        // 3. 인증 정보를 기반으로 JWT 토큰 생성
        TokenInfo tokenInfo = jwtTokenProvider.generateToken(authentication);

        // 4. DB에 Refresh Token 저장
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        user.setRefreshToken(tokenInfo.refreshToken());

        log.info("로그인 완료: userId={}, email={}", user.getId(), user.getEmail());
        return tokenInfo;
    }

    /**
     * HttpOnly 쿠키 기반 로그인을 처리하고 JWT 토큰을 반환합니다.
     * 
     * <p>
     * JWT 기반 인증 시스템을 위한 쿠키 기반 로그인 처리입니다.
     * 기존 login 메서드와 동일한 인증 과정을 거치지만,
     * Access Token만 반환하여 HttpOnly 쿠키에 저장하도록 설계되었습니다.
     * 
     * <h3>처리 과정:</h3>
     * <ol>
     * <li>사용자 인증 정보 검증 (이메일/비밀번호)</li>
     * <li>Spring Security 인증 처리</li>
     * <li>JWT 토큰 생성</li>
     * <li>Refresh Token을 데이터베이스에 저장</li>
     * <li>Access Token만 반환 (쿠키 설정용)</li>
     * </ol>
     * 
     * <h3>보안 특징:</h3>
     * <ul>
     * <li>JWT는 HttpOnly 쿠키에 저장되어 XSS 공격 방지</li>
     * <li>Remember Me 기능 지원</li>
     * <li>CSRF 방지를 위한 SameSite 정책 적용</li>
     * </ul>
     * 
     * @param request 로그인 요청 정보 (이메일, 비밀번호, Remember Me)
     * @return JWT Access Token (HttpOnly 쿠키에 저장용)
     * @throws org.springframework.security.authentication.BadCredentialsException 인증
     *                                                                             실패
     *                                                                             시
     * @throws IllegalArgumentException                                            사용자를
     *                                                                             찾을
     *                                                                             수
     *                                                                             없는
     *                                                                             경우
     * 
     * @since 2.1.0
     * @see LoginRequest#isRememberMe()
     */
    @Transactional
    public String loginWithCookie(LoginRequest request) {
        log.info("쿠키 기반 로그인 처리 시작: email={}, rememberMe={}",
                request.getEmail(), request.isRememberMe());

        // 1. 기존 로그인 로직과 동일한 인증 처리
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                request.getEmail(), request.getPassword());

        Authentication authentication = authenticationManagerBuilder.getObject()
                .authenticate(authenticationToken);

        // 2. JWT 토큰 생성
        TokenInfo tokenInfo = jwtTokenProvider.generateToken(authentication);

        // 3. DB에 Refresh Token 저장
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        user.setRefreshToken(tokenInfo.refreshToken());

        log.info("쿠키 기반 로그인 완료: userId={}, email={}", user.getId(), user.getEmail());

        // 4. Access Token만 반환 (쿠키 설정용)
        return tokenInfo.accessToken();
    }

    /**
     * 사용자의 Refresh Token을 데이터베이스에서 삭제하여 로그아웃 처리합니다.
     * 
     * <p>
     * 로그아웃 처리는 서버 측에서 Refresh Token을 무효화하는 것으로 이루어집니다.
     * Access Token은 클라이언트 측에서 삭제하고, 서버에서는 만료 시까지 유효합니다.
     * 
     * <h3>처리 과정:</h3>
     * <ol>
     * <li>사용자 이메일로 계정 조회</li>
     * <li>해당 사용자의 Refresh Token을 null로 설정</li>
     * <li>데이터베이스에 변경사항 반영</li>
     * </ol>
     * 
     * <h3>보안 고려사항:</h3>
     * <ul>
     * <li>Refresh Token 무효화로 토큰 갱신 차단</li>
     * <li>Access Token은 만료 시까지 유효하므로 클라이언트에서 삭제 필요</li>
     * <li>완전한 보안을 위해서는 토큰 블랙리스트 구현 고려</li>
     * </ul>
     * 
     * @param email 로그아웃할 사용자의 이메일 주소
     * @throws IllegalArgumentException 해당 이메일의 사용자를 찾을 수 없는 경우
     * 
     * @see User#setRefreshToken(String)
     */
    @Transactional
    public void logout(String email) {
        log.info("로그아웃 처리 시작: email={}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("로그아웃 처리 중 사용자를 찾을 수 없습니다."));
        user.setRefreshToken(null); // 리프레시 토큰을 null로 업데이트

        log.info("로그아웃 처리 완료: email={}", email);
    }

    /**
     * Refresh Token을 사용하여 새로운 Access Token과 Refresh Token을 발급합니다.
     * 
     * <p>
     * 토큰 갱신은 Token Rotation 방식을 사용하여 보안을 강화합니다:
     * <ol>
     * <li>제출된 Refresh Token의 유효성 검증</li>
     * <li>데이터베이스에서 해당 토큰을 가진 사용자 조회</li>
     * <li>새로운 Access Token과 Refresh Token 생성</li>
     * <li>기존 Refresh Token을 새 토큰으로 교체</li>
     * </ol>
     * 
     * <h3>Token Rotation의 보안 이점:</h3>
     * <ul>
     * <li>사용된 Refresh Token은 즉시 무효화</li>
     * <li>토큰 탈취 시 피해 범위 최소화</li>
     * <li>토큰 재사용 공격 방지</li>
     * </ul>
     * 
     * <h3>오류 상황:</h3>
     * <ul>
     * <li>Refresh Token이 만료된 경우</li>
     * <li>Refresh Token이 데이터베이스에 존재하지 않는 경우</li>
     * <li>토큰 형식이 잘못된 경우</li>
     * </ul>
     * 
     * @param refreshToken 갱신에 사용할 Refresh Token
     * @return 새로 발급된 토큰 정보 (Access Token, Refresh Token 포함)
     * @throws IllegalArgumentException     Refresh Token이 유효하지 않거나 해당하는 사용자를 찾을 수
     *                                      없는 경우
     * @throws io.jsonwebtoken.JwtException JWT 토큰 처리 중 오류가 발생한 경우
     * 
     * @see JwtTokenProvider#validateToken(String)
     * @see TokenInfo
     */
    @Transactional
    public TokenInfo refreshTokens(String refreshToken) {
        log.info("토큰 갱신 처리 시작");

        // 1. Refresh Token 유효성 검증
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            log.warn("유효하지 않은 리프레시 토큰 시도");
            throw new IllegalArgumentException("유효하지 않은 리프레시 토큰입니다.");
        }

        // 2. DB에서 해당 리프레시 토큰을 가진 사용자 정보 조회
        User user = userRepository.findByRefreshToken(refreshToken) // 이 메서드는 UserRepository에 추가해야 합니다.
                .orElseThrow(() -> {
                    log.warn("데이터베이스에 존재하지 않는 리프레시 토큰 시도");
                    return new IllegalArgumentException("리프레시 토큰에 해당하는 사용자를 찾을 수 없습니다.");
                });

        // 3. 새로운 토큰 생성
        // 사용자 정보로 Authentication 객체를 다시 만들어 토큰을 생성합니다.
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user.getEmail(), null, new PrincipalDetails(user).getAuthorities());
        TokenInfo newTokenInfo = jwtTokenProvider.generateToken(authentication);

        // 4. DB에 새로운 Refresh Token으로 업데이트 (토큰 회전)
        user.setRefreshToken(newTokenInfo.refreshToken());

        log.info("토큰 갱신 완료: userId={}", user.getId());
        return newTokenInfo;
    }

    /**
     * 이메일로 사용자 정보를 조회합니다.
     * 
     * <p>
     * 이 메서드는 주로 로그인 후 사용자 정보를 응답에 포함시킬 때 사용됩니다.
     * 읽기 전용 트랜잭션 내에서 실행되어 성능을 최적화합니다.
     * 
     * <h3>사용 사례:</h3>
     * <ul>
     * <li>로그인 응답에 사용자 기본 정보 포함</li>
     * <li>사용자 권한 확인</li>
     * <li>프로필 정보 조회</li>
     * </ul>
     * 
     * @param email 조회할 사용자의 이메일 주소
     * @return 해당 이메일의 사용자 엔티티
     * @throws IllegalArgumentException 해당 이메일의 사용자를 찾을 수 없는 경우
     * 
     * @see User
     */
    public User findUserByEmail(String email) {
        log.debug("사용자 조회: email={}", email);

        return userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 사용자 조회 시도: email={}", email);
                    return new IllegalArgumentException("해당 이메일의 사용자를 찾을 수 없습니다: " + email);
                });
    }
}