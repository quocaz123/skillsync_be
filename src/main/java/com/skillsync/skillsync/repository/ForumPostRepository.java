package com.skillsync.skillsync.repository;

import com.skillsync.skillsync.entity.ForumPost;
import com.skillsync.skillsync.enums.PostType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ForumPostRepository extends JpaRepository<ForumPost, UUID> {
    Page<ForumPost> findByAuthorIdOrderByCreatedAtDesc(UUID authorId, Pageable pageable);

    Page<ForumPost> findByCategoryIdOrderByCreatedAtDesc(UUID categoryId, Pageable pageable);

    Page<ForumPost> findByPostTypeOrderByCreatedAtDesc(PostType postType, Pageable pageable);

    Page<ForumPost> findByTitleContainingIgnoreCaseOrContentContainingIgnoreCaseOrderByCreatedAtDesc(
            String title, String content, Pageable pageable);

    List<ForumPost> findTop10ByOrderByCreatedAtDesc();

    Page<ForumPost> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
