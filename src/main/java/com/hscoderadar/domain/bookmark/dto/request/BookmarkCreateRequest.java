package com.hscoderadar.domain.bookmark.dto.request;

import com.hscoderadar.domain.bookmark.entity.BookType;
import com.hscoderadar.domain.bookmark.entity.Bookmark;
import com.hscoderadar.domain.user.entity.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BookmarkCreateRequest(
    @NotNull(message = "북마크 타입은 필수입니다.") String type,
    @NotBlank(message = "대상 값은 비워둘 수 없습니다.") @Size(max = 50, message = "대상 값은 50자를 초과할 수 없습니다.") String targetValue,
    @NotBlank(message = "표시 이름은 비워둘 수 없습니다.") @Size(max = 200, message = "표시 이름은 200자를 초과할 수 없습니다.") String displayName,
    boolean sseGenerated,
    Object sseEventData,
    boolean smsNotificationEnabled,
    boolean emailNotificationEnabled) {
  public Bookmark toEntity(User user) {
    return Bookmark.builder()
        .user(user)
        .type(type)
        .targetValue(targetValue)
        .displayName(displayName)
        .sseGenerated(sseGenerated)
        .smsNotificationEnabled(smsNotificationEnabled)
        .emailNotificationEnabled(emailNotificationEnabled)
        .build();
  }
}