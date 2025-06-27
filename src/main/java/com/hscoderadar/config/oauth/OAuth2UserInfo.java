package com.hscoderadar.config.oauth;

import java.util.Map;

/**
 * OAuth2 제공업체별 사용자 정보를 통일된 인터페이스로 처리하기 위한 어댑터 패턴 구현
 *
 * <p>Google, Naver, Kakao 등 각 OAuth2 제공업체마다 다른 JSON 응답 구조를 애플리케이션 내부에서 일관된 방식으로 처리할 수 있도록 추상화
 *
 * <h3>지원 OAuth2 제공업체:</h3>
 *
 * <ul>
 *   <li>{@link GoogleUserInfo} - Google OAuth2 사용자 정보
 *   <li>{@link NaverUserInfo} - Naver OAuth2 사용자 정보
 *   <li>{@link KakaoUserInfo} - Kakao OAuth2 사용자 정보
 * </ul>
 *
 * <h3>설계 원칙:</h3>
 *
 * <ul>
 *   <li>어댑터 패턴을 통한 제공업체별 응답 구조 통일
 *   <li>null 안전성 보장 및 기본값 제공
 *   <li>확장 가능한 구조로 새로운 OAuth2 제공업체 추가 용이
 * </ul>
 *
 * @author HsCodeRadar Team
 * @since 1.0.0
 * @see CustomOAuth2UserService
 * @see OAuth2MapUtils
 */
public interface OAuth2UserInfo {

  /**
   * OAuth2 제공업체에서 반환한 원본 사용자 속성 정보
   *
   * @return 제공업체별 원본 속성 Map
   */
  Map<String, Object> getAttributes();

  /**
   * OAuth2 제공업체 내에서의 고유 사용자 식별자
   *
   * <p>각 제공업체마다 다른 형태로 제공되는 고유 ID:
   *
   * <ul>
   *   <li>Google: sub 필드 (숫자 문자열)
   *   <li>Naver: id 필드 (숫자 문자열)
   *   <li>Kakao: id 필드 (숫자)
   * </ul>
   *
   * @return 제공업체별 고유 사용자 ID
   */
  String getProviderId();

  /**
   * OAuth2 제공업체 식별 문자열
   *
   * @return "google", "naver", "kakao" 등의 제공업체 식별자
   */
  String getProvider();

  /**
   * 사용자 이메일 주소
   *
   * <p>제공업체별 특징:
   *
   * <ul>
   *   <li>Google: 기본 제공, 인증된 이메일
   *   <li>Naver: 사용자 동의 시 제공
   *   <li>Kakao: 선택 동의 항목, 미동의 시 가상 이메일 생성
   * </ul>
   *
   * @return 사용자 이메일 주소 또는 가상 이메일
   */
  String getEmail();

  /**
   * 사용자 이름 또는 닉네임
   *
   * <p>제공업체별 특징:
   *
   * <ul>
   *   <li>Google: 계정 표시 이름
   *   <li>Naver: 실명 (실명 인증 기반)
   *   <li>Kakao: 카카오톡 프로필 닉네임
   * </ul>
   *
   * @return 사용자 이름 또는 닉네임
   */
  String getName();

  /**
   * 사용자 프로필 이미지 URL
   *
   * <p>제공업체별 특징:
   *
   * <ul>
   *   <li>Google: picture 필드에서 프로필 이미지 URL 획득
   *   <li>Naver: profile_image 필드에서 프로필 이미지 URL 획득
   *   <li>Kakao: thumbnail_image 필드에서 프로필 이미지 URL 획득
   * </ul>
   *
   * @return 프로필 이미지 URL 또는 null (이미지가 없는 경우)
   */
  String getProfileImage();
}
