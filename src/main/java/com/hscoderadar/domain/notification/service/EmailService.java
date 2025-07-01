package com.hscoderadar.domain.notification.service;

public interface EmailService {
    void sendEmail(String to, String subject, String content);
}