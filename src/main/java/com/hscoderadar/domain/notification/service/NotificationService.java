package com.hscoderadar.domain.notification.service;

import com.hscoderadar.domain.notification.dto.request.NotificationSettingsUpdateRequest;
import com.hscoderadar.domain.notification.dto.response.NotificationSettingsResponse;
import com.hscoderadar.domain.user.entity.User;
import com.hscoderadar.domain.user.entity.UserSettings;
import com.hscoderadar.domain.user.repository.UserSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

  private final UserSettingsRepository userSettingsRepository;

  @Transactional(readOnly = true)
  public NotificationSettingsResponse getNotificationSettings(User user) {
    UserSettings settings = user.getUserSettings();
    return NotificationSettingsResponse.from(settings);
  }

  @Transactional
  public NotificationSettingsResponse updateNotificationSettings(User user, NotificationSettingsUpdateRequest request) {
    UserSettings settings = user.getUserSettings();
    settings.updateNotificationSettings(
        request.smsNotificationEnabled(),
        request.emailNotificationEnabled(),
        request.notificationFrequency(),
        request.notificationTime());
    userSettingsRepository.save(settings);
    return NotificationSettingsResponse.from(settings);
  }
}