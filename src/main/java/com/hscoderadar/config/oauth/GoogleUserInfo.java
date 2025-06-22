package com.hscoderadar.config.oauth;

import lombok.extern.slf4j.Slf4j;
import java.util.HashMap;
import java.util.Map;

/**
 * Google OAuth2 사용자 정보를 처리하는 구현체
 * 
 * <p>
 * Google OAuth2 로그인 시 Google에서 제공하는 사용자 정보를
 * 애플리케이션 내부에서 사용할 수 있는 형태로 변환하는 어댑터 클래스
 * 
 * <h3>Google OAuth2 사용자 정보 구조:</h3>
 * 
 * <pre>{@code
 * {
 *   "sub": "108204268033311374519",           // Google 고유 사용자 ID
 *   "name": "홍길동",                         // 사용자 이름
 *   "email": "user@gmail.com",               // 이메일 주소
 *   "picture": "https://...",                // 프로필 이미지 URL
 *   "email_verified": true,                  // 이메일 인증 여부
 *   "locale": "ko"                           // 사용자 로케일
 * }
 * }</pre>
 * 
 * <h3>보안 고려사항:</h3>
 * <ul>
 * <li>Google의 sub 필드를 providerId로 사용하여 고유성 보장</li>
 * <li>이메일은 Google에서 인증된 상태로 제공</li>
 * <li>null 또는 예상치 못한 데이터에 대한 안전한 처리</li>
 * </ul>
 * 
 * @author HsCodeRadar Team
 * @since 1.0.0
 * @see OAuth2UserInfo
 * @see OAuth2MapUtils
 * @see com.hscoderadar.config.oauth.CustomOAuth2UserService
 */
@Slf4j
public class GoogleUserInfo implements OAuth2UserInfo {
    private final Map<String, Object> attributes;

    /**
     * Google OAuth2 사용자 정보로 객체 초기화
     * 
     * <p>
     * Google에서 제공받은 사용자 속성 정보를 저장하고,
     * null인 경우 빈 Map으로 초기화하여 NullPointerException 방지
     * 
     * @param attributes Google OAuth2에서 제공받은 사용자 속성 정보 (null 허용)
     */
    public GoogleUserInfo(Map<String, Object> attributes) {
        // null 체크를 통한 안전한 초기화
        this.attributes = attributes != null ? attributes : new HashMap<>();

        log.debug("GoogleUserInfo 생성 완료: email={}, name={}", getEmail(), getName());
    }

    /**
     * Google OAuth2에서 제공받은 전체 사용자 속성 정보 반환
     * 
     * @return Google 사용자 속성 정보 Map (읽기 전용)
     */
    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    /**
     * Google 고유 사용자 식별자 반환
     * 
     * <p>
     * Google OAuth2에서 제공하는 'sub' 필드는 Google 내에서
     * 사용자를 고유하게 식별하는 불변의 식별자
     * 
     * <h3>특징:</h3>
     * <ul>
     * <li>Google 계정마다 고유한 값</li>
     * <li>변경되지 않는 영구적 식별자</li>
     * <li>이메일 변경과 무관하게 동일 사용자 식별 가능</li>
     * </ul>
     * 
     * @return Google 사용자 고유 ID (sub 필드), 없을 경우 "unknown"
     */
    @Override
    public String getProviderId() {
        return OAuth2MapUtils.extractStringSafely(attributes, "sub", "unknown");
    }

    /**
     * OAuth2 제공자 식별자 반환
     * 
     * @return "google" 고정값
     */
    @Override
    public String getProvider() {
        return "google";
    }

    /**
     * Google 계정의 이메일 주소 반환
     * 
     * <p>
     * Google OAuth2에서는 사용자 이메일이 기본적으로 제공되며,
     * Google에서 이미 인증된 상태의 이메일 주소
     * 
     * <h3>특징:</h3>
     * <ul>
     * <li>Google에서 인증된 유효한 이메일 주소</li>
     * <li>기본 로그인 식별자로 사용</li>
     * <li>이메일 인증 절차 불필요</li>
     * </ul>
     * 
     * @return Google 계정 이메일 주소, 없을 경우 "unknown@gmail.com"
     */
    @Override
    public String getEmail() {
        return OAuth2MapUtils.extractStringSafely(attributes, "email", "unknown@gmail.com");
    }

    /**
     * Google 계정의 사용자 이름 반환
     * 
     * <p>
     * Google OAuth2에서 제공하는 'name' 필드는 사용자가 Google 계정에
     * 설정한 표시 이름으로, 실명이거나 사용자가 선택한 닉네임일 수 있음
     * 
     * <h3>특징:</h3>
     * <ul>
     * <li>사용자가 Google 계정에 설정한 표시 이름</li>
     * <li>실명 또는 닉네임 형태</li>
     * <li>사용자에 의해 변경 가능</li>
     * </ul>
     * 
     * @return Google 계정 사용자 이름, 없을 경우 "Unknown User"
     */
    @Override
    public String getName() {
        return OAuth2MapUtils.extractStringSafely(attributes, "name", "Unknown User");
    }

    /**
     * Google 계정의 프로필 이미지 URL 반환
     * 
     * <p>
     * Google OAuth2에서 제공하는 'picture' 필드는 사용자가 Google 계정에
     * 설정한 프로필 이미지 URL로, 일반적으로 Google 서버에 호스팅되는 이미지
     * 
     * <h3>특징:</h3>
     * <ul>
     * <li>Google 서버에서 제공되는 프로필 이미지 URL</li>
     * <li>사용자가 프로필 이미지를 설정하지 않은 경우 null 반환</li>
     * <li>이미지 크기는 Google에서 자동 조정</li>
     * <li>HTTPS 프로토콜로 제공</li>
     * </ul>
     * 
     * @return Google 계정 프로필 이미지 URL, 없을 경우 null
     */
    @Override
    public String getProfileImage() {
        return OAuth2MapUtils.extractStringSafely(attributes, "picture", null);
    }
}