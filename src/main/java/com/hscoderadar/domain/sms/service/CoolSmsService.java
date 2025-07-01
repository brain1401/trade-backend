package com.hscoderadar.domain.sms.service;

import com.hscoderadar.common.exception.SmsException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.nurigo.sdk.NurigoApp;
import net.nurigo.sdk.message.model.Message;
import net.nurigo.sdk.message.request.SingleMessageSendingRequest;
import net.nurigo.sdk.message.response.SingleMessageSentResponse;
import net.nurigo.sdk.message.service.DefaultMessageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class CoolSmsService implements SmsService {

    @Value("${app.sms.api-key}")
    private String apiKey;

    @Value("${app.sms.api-secret}")
    private String apiSecret;

    @Value("${app.sms.sender-number}")
    private String senderNumber;

    private DefaultMessageService messageService;
    private final StringRedisTemplate redisTemplate;
    private static final String VERIFICATION_CODE_PREFIX = "sms:verification:";
    private static final long VERIFICATION_CODE_EXPIRATION_MINUTES = 5;

    @PostConstruct
    protected void init() {
        this.messageService = NurigoApp.INSTANCE.initialize(apiKey, apiSecret, "https://api.coolsms.co.kr");
    }

    @Override
    public String sendVerificationCode(String to) {
        String verificationCode = generateVerificationCode();
        Message message = new Message();
        message.setFrom(senderNumber);
        message.setTo(to);
        message.setText("[HsCodeRadar] 인증번호는 [" + verificationCode + "] 입니다.");

        try {
            SingleMessageSentResponse response = this.messageService.sendOne(new SingleMessageSendingRequest(message));
            log.info("SMS 발송 결과: {}", response);

            if (response.getStatusCode().equals("2000")) {
                // Redis에 인증번호 저장 (5분 유효)
                redisTemplate.opsForValue().set(
                        VERIFICATION_CODE_PREFIX + to,
                        verificationCode,
                        VERIFICATION_CODE_EXPIRATION_MINUTES,
                        TimeUnit.MINUTES
                );
                return verificationCode;
            } else {
                throw SmsException.sendFailed();
            }
        } catch (Exception e) {
            log.error("SMS 발송 실패: to={}, error={}", to, e.getMessage());
            throw new SmsException(SmsException.sendFailed().getErrorCode(), e);
        }
    }

    @Override
    public boolean verifyCode(String phoneNumber, String code) {
        String storedCode = redisTemplate.opsForValue().get(VERIFICATION_CODE_PREFIX + phoneNumber);
        if (storedCode != null && storedCode.equals(code)) {
            // 인증 성공 시 Redis에서 코드 삭제
            redisTemplate.delete(VERIFICATION_CODE_PREFIX + phoneNumber);
            return true;
        }
        return false;
    }

    @Override
    public void sendMessage(String to, String content) {
        Message message = new Message();
        message.setFrom(senderNumber);
        message.setTo(to);
        message.setText(content);

        try {
            SingleMessageSentResponse response = this.messageService.sendOne(new SingleMessageSendingRequest(message));
            log.info("SMS 발송 결과: {}", response);

            if (!"2000".equals(response.getStatusCode())) {
                throw SmsException.sendFailed();
            }
        } catch (Exception e) {
            log.error("SMS 발송 실패: to={}, error={}", to, e.getMessage());
            throw new SmsException(SmsException.sendFailed().getErrorCode(), e);
        }
    }

    private String generateVerificationCode() {
        SecureRandom random = new SecureRandom();
        int num = random.nextInt(900000) + 100000; // 100000 ~ 999999
        return String.valueOf(num);
    }
}