package com.skillsync.skillsync.dto.response.forum;

import com.skillsync.skillsync.enums.VoteType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VoteResponse {
    UUID id;
    UUID postId;
    UUID userId;
    VoteType voteType;
    LocalDateTime createdAt;
}
