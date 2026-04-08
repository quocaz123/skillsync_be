package com.skillsync.skillsync.dto.request.forum;

import com.skillsync.skillsync.enums.PostType;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateForumPostRequest {
    @NotNull(message = "Category ID cannot be null")
    UUID categoryId;

    @NotBlank(message = "Title cannot be blank")
    @Size(min = 3, max = 200, message = "Title must be between 3 and 200 characters")
    String title;

    @NotBlank(message = "Content cannot be blank")
    @Size(min = 1, max = 5000, message = "Content must be between 1 and 5000 characters")
    String content;

    PostType postType;
    List<String> tags;
}
