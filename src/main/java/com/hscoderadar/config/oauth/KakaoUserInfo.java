package com.hscoderadar.config.oauth;

import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * Kakao OAuth2 사용자 정보를 처리하는 구현체
 *
 * <p>Kakao OAuth2 로그인 시 카카오에서 제공하는 사용자 정보를 애플리케이션 내부에서 사용할 수 있는 형태로 변환하는 어댑터 클래스
 *
 * <h3>Kakao OAuth2 사용자 정보 구조:</h3>
 *
 * <pre>{@code
 * {
 *   "id": 123456789,                               // 카카오 회원번호 (숫자)
 *   "kakao_account": {                             // 카카오계정 정보
 *     "email": "user@kakao.com",                   // 이메일 (선택동의)
 *     "email_needs_agreement": false,              // 이메일 동의 필요 여부
 *     "profile": {                                 // 프로필 정보
 *       "nickname": "홍길동",                      // 닉네임
 *       "profile_image_url": "https://...",        // 프로필 이미지
 *       "thumbnail_image_url": "https://..."       // 프로필 섬네일
 *     }
 *   }
 * }
 * }</pre>
 *
 * <h3>카카오 특이사항:</h3>
 *
 * <ul>
 *   <li>이메일은 선택 동의 항목으로 null일 수 있음
 *   <li>중첩된 JSON 구조로 데이터 접근 복잡
 *   <li>회원번호(id)는 숫자 타입으로 제공
 *   <li>이메일 없을 경우 가상 이메일 생성 필요
 * </ul>
 *
 * <h3>보안 고려사항:</h3>
 *
 * <ul>
 *   <li>카카오 회원번호를 providerId로 사용하여 고유성 보장
 *   <li>이메일 동의하지 않은 사용자도 가입 가능하도록 처리
 *   <li>중첩 구조에서 안전한 데이터 추출
 * </ul>
 *
 * @author HsCodeRadar Team
 * @since 1.0.0
 * @see OAuth2UserInfo
 * @see OAuth2MapUtils
 * @see com.hscoderadar.config.oauth.CustomOAuth2UserService
 */
@Slf4j
public class KakaoUserInfo implements OAuth2UserInfo {
  private final Map<String, Object> attributes;

  /**
   * Kakao OAuth2 사용자 정보로 객체 초기화
   *
   * <p>카카오에서 제공받은 사용자 속성 정보를 저장하고, null인 경우 빈 Map으로 초기화하여 NullPointerException 방지
   *
   * @param attributes Kakao OAuth2에서 제공받은 사용자 속성 정보 (null 허용)
   */
  public KakaoUserInfo(Map<String, Object> attributes) {
    // null 체크를 통한 안전한 초기화
    this.attributes = attributes != null ? attributes : new HashMap<>();

    log.debug("KakaoUserInfo 생성 완료: email={}, name={}", getEmail(), getName());
  }

  /**
   * Kakao OAuth2에서 제공받은 전체 사용자 속성 정보 반환
   *
   * @return Kakao 사용자 속성 정보 Map (읽기 전용)
   */
  @Override
  public Map<String, Object> getAttributes() {
    return attributes;
  }

  /**
   * 카카오 고유 사용자 식별자 반환
   *
   * <p>카카오 OAuth2에서 제공하는 'id' 필드는 카카오 회원번호로, 카카오 내에서 사용자를 고유하게 식별하는 숫자형 식별자
   *
   * <h3>특징:</h3>
   *
   * <ul>
   *   <li>카카오 계정마다 고유한 숫자 값
   *   <li>변경되지 않는 영구적 식별자
   *   <li>이메일이나 닉네임 변경과 무관하게 동일 사용자 식별 가능
   * </ul>
   *
   * @return 카카오 회원번호 (id 필드), 없을 경우 "unknown"
   */
  @Override
  public String getProviderId() {
    return OAuth2MapUtils.extractStringSafely(attributes, "id", "unknown");
  }

  /**
   * OAuth2 제공자 식별자 반환
   *
   * @return "kakao" 고정값
   */
  @Override
  public String getProvider() {
    return "kakao";
  }

  /**
   * 카카오 계정의 이메일 주소 반환
   *
   * <p>카카오 OAuth2에서 이메일은 선택 동의 항목이므로 사용자가 동의하지 않으면 null이 반환됨 이 경우 카카오 회원번호를 기반으로 가상의 이메일 주소를 생성하여
   * 시스템 호환성 보장
   *
   * <h3>처리 로직:</h3>
   *
   * <ol>
   *   <li>kakao_account.email 경로에서 이메일 추출 시도
   *   <li>이메일이 없거나 "unknown"인 경우 가상 이메일 생성
   *   <li>가상 이메일 형식: {providerId}@kakao.com
   * </ol>
   *
   * <h3>카카오 이메일 특징:</h3>
   *
   * <ul>
   *   <li>선택 동의 항목으로 null 가능성 존재
   *   <li>사용자가 나중에 동의 철회 가능
   *   <li>이메일 없이도 서비스 이용 가능하도록 설계 필요
   * </ul>
   *
   * @return 카카오 계정 이메일 주소 또는 가상 이메일 ({providerId}@kakao.com)
   */
  @Override
  public String getEmail() {
    // 이메일 동의 항목이 선택 사항(필수 아님)이므로, 사용자가 동의하지 않으면 null이 반환
    String email =
        OAuth2MapUtils.extractNestedStringSafely(
            attributes, new String[] {"kakao_account", "email"}, null);

    // 이메일 정보가 없으면, 고유 ID를 기반으로 가상의 이메일을 생성
    if (email == null || "unknown".equals(email)) {
      return getProviderId() + "@kakao.com";
    }

    return email;
  }

  /**
   * 카카오 계정의 사용자 닉네임 반환
   *
   * <p>카카오 OAuth2에서 사용자 닉네임은 중첩된 경로(kakao_account.profile.nickname)에 위치함 OAuth2MapUtils의 중첩 경로 추출
   * 기능을 활용하여 안전하게 접근
   *
   * <h3>처리 로직:</h3>
   *
   * <ol>
   *   <li>kakao_account 객체 추출
   *   <li>profile 객체 추출
   *   <li>nickname 필드 추출
   *   <li>각 단계에서 null 체크 및 안전한 처리
   * </ol>
   *
   * <h3>카카오 닉네임 특징:</h3>
   *
   * <ul>
   *   <li>사용자가 카카오톡에서 설정한 프로필 닉네임
   *   <li>실명이 아닌 경우가 많음
   *   <li>사용자에 의해 언제든 변경 가능
   * </ul>
   *
   * @return 카카오 계정 닉네임, 없을 경우 "Unknown User"
   */
  @Override
  public String getName() {
    // 중첩 경로를 통한 간결한 접근
    return OAuth2MapUtils.extractNestedStringSafely(
        attributes, new String[] {"kakao_account", "profile", "nickname"}, "Unknown User");
  }

  /**
   * 카카오 계정의 프로필 이미지 URL 반환
   *
   * <p>카카오 OAuth2에서 사용자 프로필 이미지는 중첩된 경로(kakao_account.profile.thumbnail_image_url)에 위치함 카카오는 프로필
   * 이미지와 썸네일 이미지를 별도로 제공하며, 썸네일 이미지를 사용
   *
   * <h3>처리 로직:</h3>
   *
   * <ol>
   *   <li>kakao_account 객체 추출
   *   <li>profile 객체 추출
   *   <li>thumbnail_image_url 필드 추출
   *   <li>각 단계에서 null 체크 및 안전한 처리
   * </ol>
   *
   * <h3>카카오 프로필 이미지 특징:</h3>
   *
   * <ul>
   *   <li>사용자가 카카오톡에서 설정한 프로필 이미지
   *   <li>thumbnail_image_url은 작은 크기의 썸네일 이미지
   *   <li>사용자가 프로필 이미지를 설정하지 않은 경우 null 반환
   *   <li>HTTPS 프로토콜로 제공
   * </ul>
   *
   * @return 카카오 계정 프로필 이미지 URL, 없을 경우 null
   */
  @Override
  public String getProfileImage() {
    // 중첩 경로를 통한 프로필 이미지 추출
    return OAuth2MapUtils.extractNestedStringSafely(
        attributes, new String[] {"kakao_account", "profile", "thumbnail_image_url"}, null);
  }
}
