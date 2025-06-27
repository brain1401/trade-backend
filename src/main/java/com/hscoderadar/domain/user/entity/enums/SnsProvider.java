package com.hscoderadar.domain.user.entity.enums;

/**
 * SNS 로그인 제공업체 열거형
 *
 * <p>지원 제공업체: - GOOGLE: 구글 OAuth 로그인 - KAKAO: 카카오 OAuth 로그인 - NAVER: 네이버 OAuth 로그인
 */
public enum SnsProvider {
  GOOGLE("Google"),
  KAKAO("Kakao"),
  NAVER("Naver");

  private final String displayName;

  SnsProvider(String displayName) {
    this.displayName = displayName;
  }

  public String getDisplayName() {
    return displayName;
  }

  /** 문자열로부터 SnsProvider 조회 */
  public static SnsProvider fromString(String value) {
    for (SnsProvider provider : SnsProvider.values()) {
      if (provider.name().equalsIgnoreCase(value)) {
        return provider;
      }
    }
    throw new IllegalArgumentException("지원하지 않는 SNS 제공업체: " + value);
  }
}
