package com.hscoderadar.config.oauth;

import java.util.Map;

public class KakaoUserInfo implements OAuth2UserInfo {
    private final Map<String, Object> attributes;
    private final Map<String, Object> kakaoAccountAttributes;
    private final Map<String, Object> profileAttributes;

    public KakaoUserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
        this.kakaoAccountAttributes = (Map<String, Object>) attributes.get("kakao_account");
        this.profileAttributes = (Map<String, Object>) kakaoAccountAttributes.get("profile");
    }

    @Override public Map<String, Object> getAttributes() { return attributes; }
    @Override public String getProviderId() { return attributes.get("id").toString(); }
    @Override public String getProvider() { return "kakao"; }
    @Override
    public String getEmail() {
    // 이메일 동의 항목이 선택 사항(필수 아님)이므로, 사용자가 동의하지 않으면 null이 반환됩니다.
        Object email = kakaoAccountAttributes.get("email");
        
        // 이메일 정보가 없으면, 고유 ID를 기반으로 가상의 이메일을 생성합니다.
        if (email == null) {
            return getProviderId() + "@kakao.com";
        }
        
            return email.toString();
    }
    @Override public String getName() { return (String) profileAttributes.get("nickname"); }
}