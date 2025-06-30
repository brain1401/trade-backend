package com.hscoderadar.domain.chat.entity;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ChatSessionId implements Serializable {
    private UUID sessionUuid;
    private LocalDateTime createdAt;
}