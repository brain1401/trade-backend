package com.hscoderadar.domain.bookmark.dto.request;

import jakarta.validation.constraints.Size;

public record BookmarkUpdateRequest(
    @Size(min = 1, max = 200, message = "표시 이름은 1자 이상 200자 이하이어야 합니다.") String displayName,
    Boolean smsNotificationEnabled,
    Boolean emailNotificationEnabled) {
}