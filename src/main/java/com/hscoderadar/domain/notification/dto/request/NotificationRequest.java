package com.hscoderadar.domain.notification.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {
    private Long userId;
    private Long bookmarkId;
    private boolean smsEnabled; 
    private boolean emailEnabled;
    private String title;
    private String content;
}