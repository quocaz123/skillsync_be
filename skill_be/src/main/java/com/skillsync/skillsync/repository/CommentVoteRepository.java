package com.skillsync.skillsync.repository;

import com.skillsync.skillsync.entity.CommentVote;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CommentVoteRepository extends JpaRepository<CommentVote, UUID> {
    Optional<CommentVote> findByCommentIdAndUserId(UUID commentId, UUID userId);

    Long countByCommentId(UUID commentId);

    boolean existsByCommentIdAndUserId(UUID commentId, UUID userId);

    void deleteByCommentIdAndUserId(UUID commentId, UUID userId);

    interface CommentLikeAgg {
        UUID getCommentId();

        Long getLikeCount();
    }

    @Query("""
            select v.comment.id as commentId,
                   count(v.id) as likeCount
            from CommentVote v
            where v.comment.id in :commentIds
            group by v.comment.id
            """)
    List<CommentLikeAgg> aggregateLikesByCommentIds(@Param("commentIds") Collection<UUID> commentIds);

    @Query("""
            select v.comment.id
            from CommentVote v
            where v.user.id = :userId
              and v.comment.id in :commentIds
            """)
    List<UUID> findLikedCommentIds(@Param("userId") UUID userId, @Param("commentIds") Collection<UUID> commentIds);
}