package com.hscoderadar.config.oauth;

import com.hscoderadar.domain.user.entity.SnsAccount;
import com.hscoderadar.domain.user.entity.User;
import com.hscoderadar.domain.user.entity.enums.SnsProvider;
import com.hscoderadar.domain.user.repository.SnsAccountRepository;
import com.hscoderadar.domain.user.repository.UserRepository;
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
 * v4.2 OAuth2 로그인 시 사용자 정보를 처리하는 커스텀 서비스
 * 
 * <p>
 * Spring Security OAuth2 클라이언트의 {@link DefaultOAuth2UserService}를 상속받아
 * Google, Naver, Kakao 등의 OAuth2 제공업체별 사용자 정보를 처리하고
 * 데이터베이스와 연동하여 사용자 등록/업데이트 및 SNS 계정 연동 관리 수행
 * 
 * <h3>v4.2 주요 개선사항:</h3>
 * <ul>
 * <li>SNS 계정 연동 테이블(sns_accounts) 활용</li>
 * <li>Spring Session 기반 세션 관리 호환성</li>
 * <li>프로필 이미지 처리 개선</li>
 * <li>계정 통합 관리 로직 강화</li>
 * </ul>
 * 
 * <h3>처리 흐름:</h3>
 * <ol>
 * <li>OAuth2 제공업체에서 사용자 정보 수신</li>
 * <li>제공업체별 응답 구조에 따른 정보 파싱</li>
 * <li>이메일을 기준으로 기존 사용자 조회</li>
 * <li>SNS 계정 연동 정보 확인 및 업데이트</li>
 * <li>신규 사용자인 경우 DB에 등록, 기존 사용자인 경우 정보 업데이트</li>
 * <li>Spring Security용 PrincipalDetails 객체 생성 및 반환</li>
 * </ol>
 * 
 * @author HsCodeRadar Team
 * @since 4.2.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final SnsAccountRepository snsAccountRepository;

    /**
     * OAuth2 사용자 정보를 로드하고 애플리케이션 사용자로 변환 (v4.2 개선)
     * 
     * @param userRequest OAuth2 사용자 요청 정보 (제공업체, 토큰 등 포함)
     * @return 인증 컨텍스트에서 사용될 OAuth2User 구현체
     * @throws OAuth2AuthenticationException 지원하지 않는 OAuth2 제공업체이거나 처리 중 오류 발생 시
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
        log.debug("OAuth2 사용자 정보 파싱 완료: email={}, name={}, provider={}",
                oAuth2UserInfo.getEmail(), oAuth2UserInfo.getName(), oAuth2UserInfo.getProvider());

        // 사용자 처리 (신규 등록 또는 기존 사용자 업데이트)
        User user = processUser(oAuth2UserInfo, registrationId);

        // Spring Security의 SecurityContext에 저장될 Principal 객체 반환
        return new PrincipalDetails(user, oAuth2User.getAttributes());
    }

    /**
     * 사용자 처리 로직 (신규 등록 또는 기존 사용자 업데이트)
     * 
     * @param oAuth2UserInfo OAuth2 사용자 정보
     * @param registrationId OAuth2 제공업체 식별자
     * @return 처리된 사용자 엔티티
     */
    private User processUser(OAuth2UserInfo oAuth2UserInfo, String registrationId) {
        String email = oAuth2UserInfo.getEmail();

        // 1. 이메일로 기존 사용자 조회
        Optional<User> existingUser = userRepository.findByEmail(email);

        if (existingUser.isPresent()) {
            User user = existingUser.get();

            // 기존 사용자 - SNS 계정 연동 확인 및 업데이트
            processSnsAccountForExistingUser(user, oAuth2UserInfo, registrationId);

            // 프로필 이미지 업데이트 (최신 정보로)
            updateProfileImageIfNeeded(user, oAuth2UserInfo);

            log.info("기존 OAuth2 사용자 로그인: userId={}, email={}, provider={}",
                    user.getId(), user.getEmail(), registrationId);

            return user;
        } else {
            // 신규 사용자 - 자동 회원가입 및 SNS 계정 연동
            return createNewUserWithSnsAccount(oAuth2UserInfo, registrationId);
        }
    }

    /**
     * 기존 사용자의 SNS 계정 연동 처리
     * 
     * @param user           기존 사용자
     * @param oAuth2UserInfo OAuth2 사용자 정보
     * @param registrationId OAuth2 제공업체 식별자
     */
    private void processSnsAccountForExistingUser(User user, OAuth2UserInfo oAuth2UserInfo, String registrationId) {
        SnsProvider provider = mapToProvider(registrationId);
        String providerId = oAuth2UserInfo.getProviderId();

        // 해당 제공업체의 SNS 계정 연동 정보 확인
        Optional<SnsAccount> existingSnsAccount = snsAccountRepository
                .findByUserAndProvider(user, provider);

        if (existingSnsAccount.isPresent()) {
            // 기존 SNS 계정 연동 정보 업데이트
            SnsAccount snsAccount = existingSnsAccount.get();
            boolean needsUpdate = false;

            if (!providerId.equals(snsAccount.getProviderId())) {
                snsAccount.updateProviderInfo(providerId, snsAccount.getProviderEmail());
                needsUpdate = true;
            }

            if (!oAuth2UserInfo.getEmail().equals(snsAccount.getProviderEmail())) {
                snsAccount.updateProviderInfo(snsAccount.getProviderId(), oAuth2UserInfo.getEmail());
                needsUpdate = true;
            }

            if (needsUpdate) {
                snsAccountRepository.save(snsAccount);
                log.debug("기존 SNS 계정 연동 정보 업데이트: userId={}, provider={}",
                        user.getId(), provider);
            }
        } else {
            // 새로운 SNS 계정 연동 추가
            SnsAccount newSnsAccount = SnsAccount.builder()
                    .user(user)
                    .provider(provider)
                    .providerId(providerId)
                    .providerEmail(oAuth2UserInfo.getEmail())
                    .build();

            snsAccountRepository.save(newSnsAccount);
            log.info("새로운 SNS 계정 연동 추가: userId={}, provider={}",
                    user.getId(), provider);
        }
    }

    /**
     * 신규 사용자 생성 및 SNS 계정 연동
     * 
     * @param oAuth2UserInfo OAuth2 사용자 정보
     * @param registrationId OAuth2 제공업체 식별자
     * @return 생성된 사용자 엔티티
     */
    private User createNewUserWithSnsAccount(OAuth2UserInfo oAuth2UserInfo, String registrationId) {
        // 신규 사용자 생성
        User newUser = User.builder()
                .email(oAuth2UserInfo.getEmail())
                .name(oAuth2UserInfo.getName())
                .profileImage(oAuth2UserInfo.getProfileImage())
                .build();

        User savedUser = userRepository.save(newUser);

        // SNS 계정 연동 정보 생성
        SnsAccount snsAccount = SnsAccount.builder()
                .user(savedUser)
                .provider(mapToProvider(registrationId))
                .providerId(oAuth2UserInfo.getProviderId())
                .providerEmail(oAuth2UserInfo.getEmail())
                .build();

        snsAccountRepository.save(snsAccount);

        log.info("신규 OAuth2 사용자 등록 완료: userId={}, email={}, provider={}, profileImage={}",
                savedUser.getId(), savedUser.getEmail(), registrationId, savedUser.getProfileImage());

        return savedUser;
    }

    /**
     * 프로필 이미지 업데이트 처리
     * 
     * @param user           사용자 엔티티
     * @param oAuth2UserInfo OAuth2 사용자 정보
     */
    private void updateProfileImageIfNeeded(User user, OAuth2UserInfo oAuth2UserInfo) {
        String newProfileImage = oAuth2UserInfo.getProfileImage();
        String currentProfileImage = user.getProfileImage();

        // 프로필 이미지가 변경된 경우에만 업데이트
        if (newProfileImage != null && !newProfileImage.equals(currentProfileImage)) {
            if (isValidProfileImageUrl(newProfileImage)) {
                user.updateProfileImage(newProfileImage);
                userRepository.save(user);
                log.debug("OAuth2 사용자 프로필 이미지 업데이트: userId={}, profileImage={}",
                        user.getId(), newProfileImage);
            } else {
                log.warn("유효하지 않은 프로필 이미지 URL: userId={}, url={}",
                        user.getId(), newProfileImage);
            }
        }
    }

    /**
     * 프로필 이미지 URL 유효성 검증
     * 
     * @param imageUrl 프로필 이미지 URL
     * @return 유효성 검증 결과
     */
    private boolean isValidProfileImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            return false;
        }

        // 기본 URL 형식 검증
        return imageUrl.startsWith("http://") || imageUrl.startsWith("https://");
    }

    /**
     * registrationId를 SnsProvider로 변환
     * 
     * @param registrationId OAuth2 제공업체 식별자
     * @return SnsProvider enum
     */
    private SnsProvider mapToProvider(String registrationId) {
        return switch (registrationId.toLowerCase()) {
            case "google" -> SnsProvider.GOOGLE;
            case "naver" -> SnsProvider.NAVER;
            case "kakao" -> SnsProvider.KAKAO;
            default -> throw new OAuth2AuthenticationException("Unsupported provider: " + registrationId);
        };
    }

    /**
     * OAuth2 제공업체별 사용자 정보 어댑터 팩토리 메서드
     * 
     * @param registrationId OAuth2 제공업체 식별자
     * @param attributes     OAuth2 제공업체에서 받은 사용자 속성
     * @return 제공업체별 사용자 정보 어댑터
     * @throws OAuth2AuthenticationException 지원하지 않는 제공업체인 경우
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