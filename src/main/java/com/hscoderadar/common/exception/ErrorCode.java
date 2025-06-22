package com.hscoderadar.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * API 명세서 v2.4 기준 통합 에러 코드 체계
 * 
 * 보안 정책에 따라 모든 에러 메시지는 시스템 내부 정보 노출을 방지하기 위해
 * 일반적인 형태로 제공됨
 * 
 * @author HsCodeRadar Team
 * @since 2.4.0
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

  // 인증 관련 (AUTH_xxx)
  AUTH_001("이메일 또는 비밀번호가 올바르지 않습니다", HttpStatus.UNAUTHORIZED),
  AUTH_002("현재 계정에 일시적인 접근 제한이 적용되었습니다", HttpStatus.LOCKED),
  AUTH_003("인증이 만료되었습니다", HttpStatus.UNAUTHORIZED),
  AUTH_004("인증 정보가 올바르지 않습니다", HttpStatus.UNAUTHORIZED),
  AUTH_005("해당 리소스에 접근할 권한이 없습니다", HttpStatus.FORBIDDEN),

  // 사용자 관련 (USER_xxx)
  USER_001("이미 사용 중인 이메일입니다", HttpStatus.CONFLICT),
  USER_002("입력 정보가 올바르지 않습니다", HttpStatus.BAD_REQUEST),
  USER_003("사용자 정보를 찾을 수 없습니다", HttpStatus.NOT_FOUND),
  USER_004("비밀번호가 정책에 맞지 않습니다", HttpStatus.UNPROCESSABLE_ENTITY),

  // OAuth 관련 (OAUTH_xxx)
  OAUTH_001("지원하지 않는 OAuth 제공자입니다", HttpStatus.BAD_REQUEST),
  OAUTH_002("소셜 로그인에 실패했습니다", HttpStatus.UNAUTHORIZED),
  OAUTH_003("사용자가 인증을 취소했습니다", HttpStatus.BAD_REQUEST),

  // 검색 관련 (SEARCH_xxx)
  SEARCH_001("검색어가 비어있습니다", HttpStatus.BAD_REQUEST),
  SEARCH_002("요청한 작업을 찾을 수 없습니다", HttpStatus.NOT_FOUND),
  SEARCH_003("분석 중 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR),
  SEARCH_004("화물번호 형식이 올바르지 않습니다", HttpStatus.BAD_REQUEST),
  SEARCH_005("화물 정보를 찾을 수 없습니다", HttpStatus.NOT_FOUND),
  SEARCH_006("검색어는 2자 이상이어야 합니다", HttpStatus.UNPROCESSABLE_ENTITY),
  SEARCH_007("분석할 수 없는 품목입니다", HttpStatus.UNPROCESSABLE_ENTITY),
  SEARCH_008("조회할 수 없는 주제입니다", HttpStatus.UNPROCESSABLE_ENTITY),

  // 북마크 관련 (BOOKMARK_xxx)
  BOOKMARK_001("북마크를 찾을 수 없습니다", HttpStatus.NOT_FOUND),
  BOOKMARK_002("이미 존재하는 북마크입니다", HttpStatus.CONFLICT),
  BOOKMARK_003("북마크 데이터가 올바르지 않습니다", HttpStatus.BAD_REQUEST),
  BOOKMARK_004("북마크할 수 없는 대상입니다", HttpStatus.UNPROCESSABLE_ENTITY),
  BOOKMARK_005("북마크 개수 한도를 초과했습니다", HttpStatus.TOO_MANY_REQUESTS),
  BOOKMARK_006("다른 곳에서 수정된 북마크입니다", HttpStatus.PRECONDITION_FAILED),

  // 피드 관련 (FEED_xxx)
  FEED_001("피드를 찾을 수 없습니다", HttpStatus.NOT_FOUND),

  // 외부 시스템 관련 (EXTERNAL_xxx)
  EXTERNAL_001("외부 시스템 연결에 실패했습니다", HttpStatus.BAD_GATEWAY),
  EXTERNAL_002("외부 시스템 응답 시간이 초과되었습니다", HttpStatus.GATEWAY_TIMEOUT),

  // Rate Limiting (RATE_LIMIT_xxx)
  RATE_LIMIT_001("로그인 시도 한도를 초과했습니다", HttpStatus.TOO_MANY_REQUESTS),
  RATE_LIMIT_002("검색 요청 한도를 초과했습니다", HttpStatus.TOO_MANY_REQUESTS),

  // 시스템 관련 (SYSTEM_xxx)
  SYSTEM_001("현재 서비스가 과부하 상태입니다", HttpStatus.SERVICE_UNAVAILABLE),

  // 공통 에러 (COMMON_xxx)
  COMMON_001("필수 입력 정보가 누락되었습니다", HttpStatus.BAD_REQUEST),
  COMMON_002("서버에서 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR),
  COMMON_003("요청 데이터 크기가 허용 한도를 초과했습니다", HttpStatus.PAYLOAD_TOO_LARGE),
  COMMON_004("API 호출 한도를 초과했습니다", HttpStatus.TOO_MANY_REQUESTS);

  private final String message;
  private final HttpStatus httpStatus;
}