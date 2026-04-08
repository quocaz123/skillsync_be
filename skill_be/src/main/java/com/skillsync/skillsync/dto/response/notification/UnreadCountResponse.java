package com.skillsync.skillsync.dto.response.notification;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UnreadCountResponse {
    long unreadCount;
}
