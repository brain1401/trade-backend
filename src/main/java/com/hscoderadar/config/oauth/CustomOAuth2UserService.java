package com.hscoderadar.config.oauth;

import com.hscoderadar.domain.users.entity.User;
import com.hscoderadar.domain.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

/**
 * OAuth2 로그인 시 사용자 정보를 처리하는 커스텀 서비스
 * 
 * <p>
 * Spring Security OAuth2 클라이언트의 {@link DefaultOAuth2UserService}를 상속받아
 * Google, Naver, Kakao 등의 OAuth2 제공업체별 사용자 정보를 처리하고
 * 데이터베이스와 연동하여 사용자 등록/업데이트를 수행
 * 
 * <h3>주요 기능:</h3>
 * <ul>
 * <li>OAuth2 제공업체별 사용자 정보 파싱 및 표준화</li>
 * <li>신규 사용자 자동 회원가입 처리</li>
 * <li>기존 사용자 정보 업데이트 (필요 시)</li>
 * <li>Spring Security 인증 컨텍스트용 Principal 객체 생성</li>
 * </ul>
 * 
 * <h3>지원 OAuth2 제공업체:</h3>
 * <ul>
 * <li><strong>Google:</strong> Google 계정 로그인</li>
 * <li><strong>Naver:</strong> 네이버 계정 로그인</li>
 * <li><strong>Kakao:</strong> 카카오 계정 로그인</li>
 * </ul>
 * 
 * <h3>처리 흐름:</h3>
 * <ol>
 * <li>OAuth2 제공업체에서 사용자 정보 수신</li>
 * <li>제공업체별 응답 구조에 따른 정보 파싱</li>
 * <li>이메일을 기준으로 기존 사용자 조회</li>
 * <li>신규 사용자인 경우 DB에 등록, 기존 사용자인 경우 정보 확인</li>
 * <li>Spring Security용 PrincipalDetails 객체 생성 및 반환</li>
 * </ol>
 * 
 * <h3>보안 고려사항:</h3>
 * <ul>
 * <li>사용자 정보는 OAuth2 제공업체에서 인증된 상태로 수신</li>
 * <li>이메일 중복 검사를 통한 계정 통합 관리</li>
 * <li>트랜잭션 처리로 데이터 일관성 보장</li>
 * </ul>
 * 
 * @author HsCodeRadar Team
 * @since 1.0.0
 * @see DefaultOAuth2UserService
 * @see OAuth2UserInfo
 * @see PrincipalDetails
 * @see OAuth2LoginSuccessHandler
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    /**
     * OAuth2 사용자 정보를 로드하고 애플리케이션 사용자로 변환
     * 
     * <p>
     * Spring Security OAuth2 클라이언트에 의해 자동으로 호출되며,
     * OAuth2 제공업체별 사용자 정보를 파싱하여 데이터베이스 연동 처리 수행
     * 
     * <h3>처리 단계:</h3>
     * <ol>
     * <li>부모 클래스를 통한 기본 OAuth2 사용자 정보 로드</li>
     * <li>OAuth2 제공업체 식별 (registrationId 기반)</li>
     * <li>제공업체별 사용자 정보 어댑터 생성</li>
     * <li>이메일 기반 기존 사용자 조회</li>
     * <li>신규 사용자 등록 또는 기존 사용자 확인</li>
     * <li>Spring Security용 Principal 객체 생성</li>
     * </ol>
     * 
     * <h3>사용자 등록 정책:</h3>
     * <ul>
     * <li><strong>신규 사용자:</strong> 자동 회원가입 후 SNS 등록 타입으로 설정</li>
     * <li><strong>기존 사용자:</strong> 현재는 정보 업데이트 없이 유지 (필요 시 확장 가능)</li>
     * </ul>
     * 
     * @param userRequest OAuth2 사용자 요청 정보 (제공업체, 토큰 등 포함)
     * @return 인증 컨텍스트에서 사용될 OAuth2User 구현체
     * @throws OAuth2AuthenticationException 지원하지 않는 OAuth2 제공업체이거나 처리 중 오류 발생 시
     * 
     * @see OAuth2UserRequest
     * @see OAuth2User
     * @see PrincipalDetails
     */
    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // 부모 클래스를 통해 OAuth2 제공업체에서 사용자 정보 로드
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        log.info("OAuth2 사용자 정보 로드 시작: provider={}", registrationId);

        // 제공업체별 사용자 정보 어댑터 생성
        OAuth2UserInfo oAuth2UserInfo = createOAuth2UserInfo(registrationId, oAuth2User.getAttributes());
        log.debug("OAuth2 사용자 정보 파싱 완료: email={}, name={}",
                oAuth2UserInfo.getEmail(), oAuth2UserInfo.getName());

        // 이메일 기반으로 기존 사용자 조회
        Optional<User> userOptional = userRepository.findByEmail(oAuth2UserInfo.getEmail());

        User user;
        if (userOptional.isPresent()) {
            // 기존 사용자 - 프로필 이미지 업데이트 처리
            user = userOptional.get();

            // 프로필 이미지가 새로 제공된 경우 업데이트
            String newProfileImage = oAuth2UserInfo.getProfileImage();
            if (newProfileImage != null && !newProfileImage.equals(user.getProfileImage())) {
                user.setProfileImage(newProfileImage);
                userRepository.save(user);
                log.debug("OAuth2 사용자 프로필 이미지 업데이트: userId={}, profileImage={}",
                        user.getId(), newProfileImage);
            }

            log.info("기존 OAuth2 사용자 로그인: userId={}, email={}", user.getId(), user.getEmail());
        } else {
            // 신규 사용자 - 자동 회원가입 처리 (프로필 이미지 포함)
            user = User.builder()
                    .email(oAuth2UserInfo.getEmail())
                    .name(oAuth2UserInfo.getName())
                    .profileImage(oAuth2UserInfo.getProfileImage()) // 프로필 이미지 설정
                    .registrationType(User.RegistrationType.SNS) // SNS 가입 유형으로 설정
                    .build();
            userRepository.save(user);
            log.info("신규 OAuth2 사용자 등록 완료: userId={}, email={}, provider={}, profileImage={}",
                    user.getId(), user.getEmail(), oAuth2UserInfo.getProvider(), user.getProfileImage());
        }

        // Spring Security의 SecurityContext에 저장될 Principal 객체 반환
        return new PrincipalDetails(user, oAuth2User.getAttributes());
    }

    /**
     * OAuth2 제공업체별 사용자 정보 어댑터 팩토리 메서드
     * 
     * <p>
     * registrationId를 기반으로 적절한 {@link OAuth2UserInfo} 구현체를 생성하여
     * 제공업체별로 다른 JSON 응답 구조를 통일된 인터페이스로 처리
     * 
     * <h3>지원 제공업체:</h3>
     * <ul>
     * <li><code>google</code> → {@link GoogleUserInfo}</li>
     * <li><code>naver</code> → {@link NaverUserInfo}</li>
     * <li><code>kakao</code> → {@link KakaoUserInfo}</li>
     * </ul>
     * 
     * @param registrationId OAuth2 제공업체 식별자 (application.yml에서 설정)
     * @param attributes     OAuth2 제공업체에서 받은 사용자 속성
     * @return 제공업체별 사용자 정보 어댑터
     * @throws OAuth2AuthenticationException 지원하지 않는 제공업체인 경우
     * 
     * @see GoogleUserInfo
     * @see NaverUserInfo
     * @see KakaoUserInfo
     */
    private OAuth2UserInfo createOAuth2UserInfo(String registrationId, Map<String, Object> attributes) {
        if (registrationId.equalsIgnoreCase("google")) {
            return new GoogleUserInfo(attributes);
        } else if (registrationId.equalsIgnoreCase("naver")) {
            return new NaverUserInfo(attributes);
        } else if (registrationId.equalsIgnoreCase("kakao")) {
            return new KakaoUserInfo(attributes);
        }

        log.error("지원하지 않는 OAuth2 제공업체: {}", registrationId);
        throw new OAuth2AuthenticationException("Unsupported provider: " + registrationId);
    }
}