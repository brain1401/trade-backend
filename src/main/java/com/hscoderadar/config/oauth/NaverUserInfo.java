package com.hscoderadar.config.oauth;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * Naver OAuth2 사용자 정보를 처리하는 구현체
 *
 * <p>Naver OAuth2 로그인 시 네이버에서 제공하는 사용자 정보를 애플리케이션 내부에서 사용할 수 있는 형태로 변환하는 어댑터 클래스
 *
 * <h3>Naver OAuth2 사용자 정보 구조:</h3>
 *
 * <pre>{@code
 * {
 *   "resultcode": "00",                            // 응답 코드
 *   "message": "success",                          // 응답 메시지
 *   "response": {                                  // 실제 사용자 정보
 *     "id": "32742776",                            // 네이버 회원 고유 ID
 *     "email": "user@naver.com",                   // 이메일 주소
 *     "name": "홍길동",                            // 이름
 *     "nickname": "길동이",                        // 닉네임
 *     "profile_image": "https://...",              // 프로필 이미지 URL
 *     "age": "30-39",                              // 연령대
 *     "gender": "M",                               // 성별 (M/F)
 *     "birthday": "03-21",                         // 생일 (MM-dd)
 *     "birthyear": "1990"                          // 출생년도
 *   }
 * }
 * }</pre>
 *
 * <h3>네이버 특이사항:</h3>
 *
 * <ul>
 *   <li>실제 사용자 정보는 'response' 키 안에 중첩되어 제공
 *   <li>외부 래퍼에는 API 호출 결과 코드와 메시지 포함
 *   <li>사용자 동의에 따라 일부 정보는 제공되지 않을 수 있음
 *   <li>고유 ID는 문자열 형태의 숫자로 제공
 * </ul>
 *
 * <h3>보안 고려사항:</h3>
 *
 * <ul>
 *   <li>네이버 회원 고유 ID를 providerId로 사용하여 고유성 보장
 *   <li>이메일은 네이버에서 인증된 상태로 제공
 *   <li>response 구조에서 안전한 데이터 추출
 * </ul>
 *
 * @author HsCodeRadar Team
 * @since 1.0.0
 * @see OAuth2UserInfo
 * @see OAuth2MapUtils
 * @see com.hscoderadar.config.oauth.CustomOAuth2UserService
 */
@Slf4j
public class NaverUserInfo implements OAuth2UserInfo {
  private final Map<String, Object> attributes;

  /**
   * Naver OAuth2 사용자 정보로 객체 초기화
   *
   * <p>네이버 OAuth2 응답의 특수한 구조를 처리하여 실제 사용자 정보만 추출함 네이버는 다른 OAuth2 제공자와 달리 'response' 키 안에 실제 사용자 정보를
   * 중첩하여 제공
   *
   * <h3>처리 과정:</h3>
   *
   * <ol>
   *   <li>전체 응답 데이터에서 'response' 키 추출
   *   <li>OAuth2MapUtils를 사용한 안전한 타입 변환
   *   <li>null 체크 및 예외 상황 처리
   * </ol>
   *
   * <h3>네이버 응답 구조 특징:</h3>
   *
   * <ul>
   *   <li>외부 래퍼: resultcode, message (API 호출 결과)
   *   <li>실제 데이터: response 객체 내부
   *   <li>다른 OAuth2 제공자와 구조가 상이함
   * </ul>
   *
   * @param attributes Naver OAuth2에서 제공받은 전체 응답 데이터 (null 허용)
   */
  public NaverUserInfo(Map<String, Object> attributes) {
    // Naver의 응답은 'response' 키 안에 실제 정보가 들어있음
    // OAuth2MapUtils를 사용한 안전한 타입 변환
    this.attributes = OAuth2MapUtils.extractMapSafely(attributes, "response");

    log.debug("NaverUserInfo 생성 완료: email={}, name={}", getEmail(), getName());
  }

  /**
   * Naver OAuth2 응답에서 추출한 사용자 속성 정보 반환
   *
   * <p>'response' 키에서 추출된 실제 사용자 정보만 포함됨
   *
   * @return Naver 사용자 속성 정보 Map (읽기 전용)
   */
  @Override
  public Map<String, Object> getAttributes() {
    return attributes;
  }

  /**
   * 네이버 고유 사용자 식별자 반환
   *
   * <p>네이버 OAuth2에서 제공하는 'id' 필드는 네이버 회원의 고유 식별자로, 네이버 내에서 사용자를 영구적으로 식별하는 문자열 형태의 숫자
   *
   * <h3>특징:</h3>
   *
   * <ul>
   *   <li>네이버 계정마다 고유한 문자열 숫자
   *   <li>변경되지 않는 영구적 식별자
   *   <li>이메일이나 닉네임 변경과 무관하게 동일 사용자 식별 가능
   *   <li>다른 사용자와 절대 중복되지 않음
   * </ul>
   *
   * @return 네이버 회원 고유 ID (id 필드), 없을 경우 "unknown"
   */
  @Override
  public String getProviderId() {
    return OAuth2MapUtils.extractStringSafely(attributes, "id", "unknown");
  }

  /**
   * OAuth2 제공자 식별자 반환
   *
   * @return "naver" 고정값
   */
  @Override
  public String getProvider() {
    return "naver";
  }

  /**
   * 네이버 계정의 이메일 주소 반환
   *
   * <p>네이버 OAuth2에서는 사용자가 동의한 경우에만 이메일 정보가 제공됨 네이버에서 이미 인증된 상태의 이메일 주소이므로 별도 인증 불필요함
   *
   * <h3>특징:</h3>
   *
   * <ul>
   *   <li>네이버에서 인증된 유효한 이메일 주소
   *   <li>사용자 동의 시에만 제공 (필수 항목 아님)
   *   <li>기본 로그인 식별자로 사용
   *   <li>이메일 인증 절차 불필요
   * </ul>
   *
   * @return 네이버 계정 이메일 주소, 없을 경우 "unknown@naver.com"
   */
  @Override
  public String getEmail() {
    return OAuth2MapUtils.extractStringSafely(attributes, "email", "unknown@naver.com");
  }

  /**
   * 네이버 계정의 사용자 이름 반환
   *
   * <p>네이버 OAuth2에서 제공하는 'name' 필드는 사용자가 네이버 계정에 등록한 실명 정보로, 대부분 실명으로 제공
   *
   * <h3>특징:</h3>
   *
   * <ul>
   *   <li>사용자가 네이버 계정에 등록한 실명
   *   <li>네이버 실명 인증 정책에 따라 검증된 이름
   *   <li>사용자 동의 시에만 제공
   *   <li>닉네임과는 별도의 필드
   * </ul>
   *
   * @return 네이버 계정 사용자 이름, 없을 경우 "Unknown User"
   */
  @Override
  public String getName() {
    return OAuth2MapUtils.extractStringSafely(attributes, "name", "Unknown User");
  }

  /**
   * 네이버 계정의 프로필 이미지 URL 반환
   *
   * <p>네이버 OAuth2에서 제공하는 'profile_image' 필드는 사용자가 네이버 계정에 설정한 프로필 이미지 URL로, 네이버 서버에 호스팅되는 이미지
   *
   * <h3>특징:</h3>
   *
   * <ul>
   *   <li>네이버 서버에서 제공되는 프로필 이미지 URL
   *   <li>사용자가 프로필 이미지를 설정하지 않은 경우 null 반환
   *   <li>사용자 동의 시에만 제공
   *   <li>HTTPS 프로토콜로 제공
   * </ul>
   *
   * @return 네이버 계정 프로필 이미지 URL, 없을 경우 null
   */
  @Override
  public String getProfileImage() {
    return OAuth2MapUtils.extractStringSafely(attributes, "profile_image", null);
  }
}
