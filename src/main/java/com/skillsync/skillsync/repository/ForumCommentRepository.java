package com.skillsync.skillsync.repository;

import com.skillsync.skillsync.entity.ForumComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ForumCommentRepository extends JpaRepository<ForumComment, UUID> {
    List<ForumComment> findByPostIdAndParentCommentIsNullOrderByCreatedAtAsc(UUID postId);

    List<ForumComment> findByParentCommentIdOrderByCreatedAtAsc(UUID parentCommentId);

    List<ForumComment> findByPostIdOrderByCreatedAtAsc(UUID postId);

    List<ForumComment> findByAuthorIdOrderByCreatedAtDesc(UUID authorId);

    Integer countByPostId(UUID postId);
}
