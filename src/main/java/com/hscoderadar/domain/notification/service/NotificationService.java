package com.hscoderadar.domain.notification.service;

import com.hscoderadar.domain.notification.dto.NotificationSettingsDto;
import com.hscoderadar.domain.user.entity.User;
import com.hscoderadar.domain.user.entity.UserSettings;
import com.hscoderadar.domain.user.repository.UserSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

  private final UserSettingsRepository userSettingsRepository;

  @Transactional(readOnly = true)
  public NotificationSettingsDto.Response getNotificationSettings(User user) {
    UserSettings settings = userSettingsRepository.findByUser(user)
        .orElseThrow(() -> new IllegalArgumentException("사용자 설정을 찾을 수 없습니다."));
    return NotificationSettingsDto.Response.from(settings);
  }

  public NotificationSettingsDto.Response updateNotificationSettings(User user,
      NotificationSettingsDto.UpdateRequest request) {
    UserSettings settings = userSettingsRepository.findByUser(user)
        .orElseThrow(() -> new IllegalArgumentException("사용자 설정을 찾을 수 없습니다."));

    settings.updateNotificationSettings(
        request.smsNotificationEnabled(),
        request.emailNotificationEnabled(),
        request.notificationFrequency(),
        request.notificationTime());

    return NotificationSettingsDto.Response.from(settings);
  }
}