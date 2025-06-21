package com.hscoderadar.config.oauth;

import java.util.Map;

public class NaverUserInfo implements OAuth2UserInfo {
    private final Map<String, Object> attributes;

    public NaverUserInfo(Map<String, Object> attributes) {
        // Naver의 응답은 'response' 키 안에 실제 정보가 들어있습니다.
        this.attributes = (Map<String, Object>) attributes.get("response");
    }

    @Override public Map<String, Object> getAttributes() { return attributes; }
    @Override public String getProviderId() { return (String) attributes.get("id"); }
    @Override public String getProvider() { return "naver"; }
    @Override public String getEmail() { return (String) attributes.get("email"); }
    @Override public String getName() { return (String) attributes.get("name"); }
}