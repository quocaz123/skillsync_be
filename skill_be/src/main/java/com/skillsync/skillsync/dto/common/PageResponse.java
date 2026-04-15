package com.skillsync.skillsync.dto.common;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;

import java.util.Collections;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PageResponse<T> {
    int currentPage;
    int totalPages;
    int pageSize;
    long totalElements;

    @Builder.Default
    List<T> data = Collections.emptyList();

    public static <T> PageResponse<T> from(Page<T> page) {
        if (page == null) {
            return PageResponse.<T>builder()
                    .currentPage(0)
                    .totalPages(0)
                    .pageSize(0)
                    .totalElements(0)
                    .data(Collections.emptyList())
                    .build();
        }

        return PageResponse.<T>builder()
                .currentPage(page.getNumber())
                .totalPages(page.getTotalPages())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .data(page.getContent())
                .build();
    }
}
