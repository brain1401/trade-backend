package com.hscoderadar.domain.sms.service;

public interface SmsService {
    /**
     * 지정된 번호로 인증 코드를 발송
     * @param to 수신자 휴대폰 번호
     * @return 생성된 인증 코드
     */
    String sendVerificationCode(String to);

    /**
     * 사용자가 입력한 코드와 발송된 코드가 일치하는지 확인
     * @param phoneNumber 휴대폰 번호
     * @param code 사용자가 입력한 인증 코드
     * @return 인증 성공 여부
     */
    boolean verifyCode(String phoneNumber, String code);

    /**
     * 일반 메시지를 발송
     * @param to 수신자 휴대폰 번호
     * @param content 발송할 메시지 내용
     */
    void sendMessage(String to, String content);
}