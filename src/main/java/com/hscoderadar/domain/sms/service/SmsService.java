package com.hscoderadar.domain.sms.service;

import com.hscoderadar.domain.sms.dto.SmsVerificationRequest;
import com.hscoderadar.domain.sms.dto.SmsVerificationResponse;
import com.hscoderadar.domain.sms.dto.SmsVerifyRequest;
import com.hscoderadar.domain.sms.dto.SmsLogResponse;
import com.hscoderadar.domain.users.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * v4.0 SMS 알림 시스템 서비스 인터페이스
 * 
 * 휴대폰 인증부터 SMS 알림 발송까지 완전한 SMS 생태계 제공
 * - 6자리 인증코드 기반 휴대폰 번호 검증
 * - 실시간 SMS 알림 (북마크 변동사항)
 * - 세밀한 알림 설정 (북마크별, 타입별)
 * - 발송 이력 및 비용 추적
 */
public interface SmsService {

  /**
   * 휴대폰 인증 코드 발송
   * 
   * @param user    인증된 사용자
   * @param request 휴대폰 번호 정보
   * @return 인증 세션 정보
   */
  SmsVerificationResponse sendVerificationCode(User user, SmsVerificationRequest request);

  /**
   * 휴대폰 인증 코드 확인
   * 
   * @param user    인증된 사용자
   * @param request 인증 정보
   * @return 인증 결과
   */
  Object verifyCode(User user, SmsVerifyRequest request);

  /**
   * 휴대폰 번호 등록
   * 
   * @param user    인증된 사용자
   * @param request 인증 세션 정보
   * @return 등록 결과
   */
  Object registerPhoneNumber(User user, Object request);

  /**
   * SMS 알림 설정 조회
   * 
   * @param user 인증된 사용자
   * @return SMS 알림 설정
   */
  Object getSmsSettings(User user);

  /**
   * SMS 알림 설정 수정
   * 
   * @param user    인증된 사용자
   * @param request 수정할 설정 정보
   * @return 수정 결과
   */
  Object updateSmsSettings(User user, Object request);

  /**
   * SMS 발송 이력 조회
   * 
   * @param user     인증된 사용자
   * @param pageable 페이징 정보
   * @param type     메시지 타입 필터
   * @param status   발송 상태 필터
   * @return SMS 발송 이력
   */
  Page<SmsLogResponse> getSmsLogs(User user, Pageable pageable, String type, String status);
}