package com.hscoderadar.domain.notification.controller;

import com.hscoderadar.common.response.ApiResponseMessage;
import com.hscoderadar.config.oauth.PrincipalDetails;
import com.hscoderadar.domain.notification.dto.NotificationSettingsDto;
import com.hscoderadar.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/settings")
    @ApiResponseMessage("알림 설정이 조회되었습니다.")
    public NotificationSettingsDto.Response getNotificationSettings(
            @AuthenticationPrincipal PrincipalDetails principalDetails) {
        return notificationService.getNotificationSettings(principalDetails.getUser());
    }

    @PutMapping("/settings")
    @ApiResponseMessage("알림 설정이 변경되었습니다.")
    public NotificationSettingsDto.Response updateNotificationSettings(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @RequestBody NotificationSettingsDto.UpdateRequest request) {
        return notificationService.updateNotificationSettings(principalDetails.getUser(), request);
    }
}