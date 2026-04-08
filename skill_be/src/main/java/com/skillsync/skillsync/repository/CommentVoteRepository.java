package com.skillsync.skillsync.repository;

import com.skillsync.skillsync.entity.CommentVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CommentVoteRepository extends JpaRepository<CommentVote, UUID> {
    Optional<CommentVote> findByCommentIdAndUserId(UUID commentId, UUID userId);

    Long countByCommentId(UUID commentId);

    boolean existsByCommentIdAndUserId(UUID commentId, UUID userId);

    void deleteByCommentIdAndUserId(UUID commentId, UUID userId);
}