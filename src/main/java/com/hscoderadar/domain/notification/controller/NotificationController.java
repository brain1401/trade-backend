package com.hscoderadar.domain.notification.controller;

import com.hscoderadar.config.oauth.PrincipalDetails;
import com.hscoderadar.domain.notification.dto.request.NotificationSettingsUpdateRequest;
import com.hscoderadar.domain.notification.dto.response.NotificationSettingsResponse;
import com.hscoderadar.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications/settings")
@RequiredArgsConstructor
public class NotificationController {

  private final NotificationService notificationService;

  @GetMapping
  public NotificationSettingsResponse getNotificationSettings(
      @AuthenticationPrincipal PrincipalDetails principalDetails) {
    return notificationService.getNotificationSettings(principalDetails.getUser());
  }

  @PutMapping
  public NotificationSettingsResponse updateNotificationSettings(
      @AuthenticationPrincipal PrincipalDetails principalDetails,
      @RequestBody NotificationSettingsUpdateRequest request) {
    return notificationService.updateNotificationSettings(principalDetails.getUser(), request);
  }
}