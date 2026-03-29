package com.skillsync.skillsync.service;

import com.skillsync.skillsync.dto.request.forum.CreateCategoryRequest;
import com.skillsync.skillsync.dto.response.forum.ForumCategoryResponse;
import com.skillsync.skillsync.entity.ForumCategory;
import com.skillsync.skillsync.repository.ForumCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ForumCategoryService {
    private final ForumCategoryRepository categoryRepository;

    /**
     * Get all categories sorted by display order
     */
    public List<ForumCategoryResponse> getAllCategories() {
        try {
            List<ForumCategory> categories = categoryRepository.findAllByOrderByDisplayOrderAsc();
            return categories.stream()
                    .map(this::toResponse)
                    .toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * Get category by ID
     */
    public ForumCategoryResponse getCategoryById(UUID id) {
        ForumCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + id));
        return toResponse(category);
    }

    /**
     * Create new category (admin only)
     */
    @Transactional
    public ForumCategoryResponse createCategory(CreateCategoryRequest request) {
        if (categoryRepository.existsByName(request.getName())) {
            throw new RuntimeException("Category with name '" + request.getName() + "' already exists");
        }

        ForumCategory category = ForumCategory.builder()
                .name(request.getName())
                .description(request.getDescription())
                .icon(request.getIcon())
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .build();

        ForumCategory saved = categoryRepository.save(category);
        return toResponse(saved);
    }

    /**
     * Update category (admin only)
     */
    @Transactional
    public ForumCategoryResponse updateCategory(UUID id, CreateCategoryRequest request) {
        ForumCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + id));

        if (request.getName() != null && !request.getName().equals(category.getName())) {
            if (categoryRepository.existsByName(request.getName())) {
                throw new RuntimeException("Category with name '" + request.getName() + "' already exists");
            }
            category.setName(request.getName());
        }

        if (request.getDescription() != null) {
            category.setDescription(request.getDescription());
        }
        if (request.getIcon() != null) {
            category.setIcon(request.getIcon());
        }
        if (request.getDisplayOrder() != null) {
            category.setDisplayOrder(request.getDisplayOrder());
        }

        ForumCategory updated = categoryRepository.save(category);
        return toResponse(updated);
    }

    /**
     * Delete category (admin only)
     */
    @Transactional
    public void deleteCategory(UUID id) {
        ForumCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + id));

        categoryRepository.delete(category);
    }

    /**
     * Convert entity to response
     */
    private ForumCategoryResponse toResponse(ForumCategory category) {
        return ForumCategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .icon(category.getIcon())
                .displayOrder(category.getDisplayOrder())
                .createdAt(category.getCreatedAt())
                .build();
    }
}
