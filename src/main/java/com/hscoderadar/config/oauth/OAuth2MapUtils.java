package com.hscoderadar.config.oauth;

import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * OAuth2 관련 Map 처리를 위한 유틸리티 클래스
 *
 * <p>OAuth2 제공업체들의 응답 데이터를 안전하게 처리하기 위한 헬퍼 메서드 제공 각 제공업체마다 다른 JSON 구조와 데이터 타입에 대응하여 null 안전성과 타입
 * 안전성을 보장하는 데이터 추출 기능 구현
 *
 * <h3>주요 기능:</h3>
 *
 * <ul>
 *   <li>중첩된 Map 구조에서 안전한 데이터 추출
 *   <li>타입 변환 실패 시 기본값 반환
 *   <li>null 안전성 보장 및 예외 상황 처리
 *   <li>다양한 데이터 타입 지원 (Map, String)
 * </ul>
 *
 * <h3>사용 사례:</h3>
 *
 * <ul>
 *   <li>Google OAuth2: 단순 구조의 사용자 정보 추출
 *   <li>Naver OAuth2: response 키 하위의 중첩 구조 처리
 *   <li>Kakao OAuth2: kakao_account.profile 등 깊은 중첩 구조 처리
 * </ul>
 *
 * @author HsCodeRadar Team
 * @since 1.0.0
 * @see OAuth2UserInfo
 * @see GoogleUserInfo
 * @see NaverUserInfo
 * @see KakaoUserInfo
 */
@Slf4j
public final class OAuth2MapUtils {

  /** 유틸리티 클래스 인스턴스화 방지 */
  private OAuth2MapUtils() {
    throw new AssertionError("OAuth2MapUtils는 인스턴스화할 수 없음");
  }

  /**
   * Map에서 안전하게 중첩된 Map을 추출하는 메서드
   *
   * <p>OAuth2 제공업체의 응답 데이터는 중첩된 JSON 구조를 가지고 있어, 안전한 타입 변환과 null 체크가 필요함
   *
   * <h3>처리 과정:</h3>
   *
   * <ol>
   *   <li>sourceMap과 key의 null 여부 검증
   *   <li>키에 해당하는 값 추출
   *   <li>Map 타입 여부 확인 (instanceof 사용)
   *   <li>타입 변환 및 ClassCastException 처리
   *   <li>실패 시 빈 HashMap 반환
   * </ol>
   *
   * @param sourceMap 원본 Map (null 허용)
   * @param key 추출할 키 (null 허용)
   * @return 안전하게 추출된 Map (실패시 빈 Map 반환)
   * @example ```java Map<String, Object> attributes = getOAuth2Attributes(); Map<String, Object>
   *     profile = OAuth2MapUtils.extractMapSafely(attributes, "profile"); ```
   */
  @SuppressWarnings("unchecked")
  public static Map<String, Object> extractMapSafely(Map<String, Object> sourceMap, String key) {
    try {
      if (sourceMap == null || key == null) {
        log.warn("OAuth2MapUtils.extractMapSafely: sourceMap 또는 key가 null. key={}", key);
        return new HashMap<>();
      }

      Object value = sourceMap.get(key);
      if (value == null) {
        log.debug("OAuth2MapUtils.extractMapSafely: key '{}'에 대한 값이 null", key);
        return new HashMap<>();
      }

      // instanceof 체크로 타입 안전성 확보
      if (value instanceof Map) {
        return (Map<String, Object>) value;
      }

      // 예상치 못한 타입인 경우 빈 Map 반환
      log.warn(
          "OAuth2MapUtils.extractMapSafely: 예상치 못한 타입. key={}, type={}",
          key,
          value.getClass().getSimpleName());
      return new HashMap<>();

    } catch (ClassCastException e) {
      // 타입 변환 실패시 빈 Map 반환
      log.error("OAuth2MapUtils.extractMapSafely: 타입 변환 실패. key={}, error={}", key, e.getMessage());
      return new HashMap<>();
    }
  }

  /**
   * Map에서 안전하게 String 값을 추출하는 메서드
   *
   * <p>다양한 데이터 타입(String, Number, Boolean 등)을 문자열로 변환하여 반환 null이거나 변환에 실패한 경우 지정된 기본값 반환
   *
   * <h3>변환 지원 타입:</h3>
   *
   * <ul>
   *   <li>String: 그대로 반환
   *   <li>Number: toString() 메서드 사용
   *   <li>Boolean: "true" 또는 "false"
   *   <li>기타: toString() 메서드 사용
   * </ul>
   *
   * @param sourceMap 원본 Map (null 허용)
   * @param key 추출할 키 (null 허용)
   * @param defaultValue 기본값 (null일 경우 반환될 값)
   * @return 안전하게 추출된 String (실패시 기본값 반환)
   * @example ```java String email = OAuth2MapUtils.extractStringSafely(attributes, "email",
   *     "unknown@example.com"); ```
   */
  public static String extractStringSafely(
      Map<String, Object> sourceMap, String key, String defaultValue) {
    try {
      if (sourceMap == null || key == null) {
        log.debug("OAuth2MapUtils.extractStringSafely: sourceMap 또는 key가 null. key={}", key);
        return defaultValue;
      }

      Object value = sourceMap.get(key);
      if (value == null) {
        log.debug("OAuth2MapUtils.extractStringSafely: key '{}'에 대한 값이 null", key);
        return defaultValue;
      }

      // toString() 메서드를 사용하여 문자열 변환
      return value.toString();

    } catch (Exception e) {
      log.error(
          "OAuth2MapUtils.extractStringSafely: 값 추출 실패. key={}, error={}", key, e.getMessage());
      return defaultValue;
    }
  }

  /**
   * Map에서 안전하게 String 값을 추출하는 메서드 (기본값: "unknown")
   *
   * <p>{@link #extractStringSafely(Map, String, String)} 메서드의 편의 오버로드로 기본값을 "unknown"으로 설정하여 호출
   *
   * @param sourceMap 원본 Map (null 허용)
   * @param key 추출할 키 (null 허용)
   * @return 안전하게 추출된 String (실패시 "unknown" 반환)
   */
  public static String extractStringSafely(Map<String, Object> sourceMap, String key) {
    return extractStringSafely(sourceMap, key, "unknown");
  }

  /**
   * 중첩된 경로를 통해 안전하게 String 값을 추출하는 메서드
   *
   * <p>여러 단계의 중첩된 Map 구조에서 깊이 있는 데이터를 안전하게 추출 각 단계마다 null 체크와 타입 검증을 수행하여 안전성 보장
   *
   * <h3>처리 과정:</h3>
   *
   * <ol>
   *   <li>경로 배열과 sourceMap 유효성 검증
   *   <li>마지막 키를 제외한 중간 경로들을 순회
   *   <li>각 단계에서 Map 추출 및 검증
   *   <li>최종 키로 String 값 추출
   *   <li>실패 시 기본값 반환
   * </ol>
   *
   * <h3>사용 사례:</h3>
   *
   * <ul>
   *   <li>Kakao: kakao_account.profile.nickname
   *   <li>복합 구조: user.profile.social.platform
   * </ul>
   *
   * @param sourceMap 원본 Map (null 허용)
   * @param path 중첩된 키 경로 (예: ["kakao_account", "profile", "nickname"])
   * @param defaultValue 기본값
   * @return 안전하게 추출된 String (실패시 기본값 반환)
   * @example ```java String nickname = OAuth2MapUtils.extractNestedStringSafely( attributes, new
   *     String[]{"kakao_account", "profile", "nickname"}, "Unknown User" ); ```
   */
  public static String extractNestedStringSafely(
      Map<String, Object> sourceMap, String[] path, String defaultValue) {
    try {
      if (sourceMap == null || path == null || path.length == 0) {
        log.debug("OAuth2MapUtils.extractNestedStringSafely: 잘못된 매개변수");
        return defaultValue;
      }

      Map<String, Object> currentMap = sourceMap;

      // 마지막 키를 제외하고 중첩된 Map들을 순회
      for (int i = 0; i < path.length - 1; i++) {
        currentMap = extractMapSafely(currentMap, path[i]);
        if (currentMap.isEmpty()) {
          log.debug(
              "OAuth2MapUtils.extractNestedStringSafely: 중간 경로에서 빈 Map 발견. path={}",
              String.join(".", path));
          return defaultValue;
        }
      }

      // 마지막 키로 String 값 추출
      return extractStringSafely(currentMap, path[path.length - 1], defaultValue);

    } catch (Exception e) {
      log.error(
          "OAuth2MapUtils.extractNestedStringSafely: 중첩 값 추출 실패. path={}, error={}",
          String.join(".", path),
          e.getMessage());
      return defaultValue;
    }
  }
}
