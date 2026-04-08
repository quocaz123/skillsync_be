package com.skillsync.skillsync.repository;

import com.skillsync.skillsync.entity.PostSave;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PostSaveRepository extends JpaRepository<PostSave, UUID> {
    Optional<PostSave> findByPostIdAndUserId(UUID postId, UUID userId);

    Page<PostSave> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    boolean existsByPostIdAndUserId(UUID postId, UUID userId);

    void deleteByPostIdAndUserId(UUID postId, UUID userId);

    Long countByPostId(UUID postId);
}
