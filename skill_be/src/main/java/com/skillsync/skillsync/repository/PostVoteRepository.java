package com.skillsync.skillsync.repository;

import com.skillsync.skillsync.entity.PostVote;
import com.skillsync.skillsync.enums.VoteType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PostVoteRepository extends JpaRepository<PostVote, UUID> {
    Optional<PostVote> findByPostIdAndUserId(UUID postId, UUID userId);

    Long countByPostIdAndVoteType(UUID postId, VoteType voteType);

    Integer countByPostId(UUID postId);

    boolean existsByPostIdAndUserId(UUID postId, UUID userId);

    void deleteByPostIdAndUserId(UUID postId, UUID userId);

    interface PostVoteAgg {
        UUID getPostId();

        Long getUpvotes();

        Long getDownvotes();
    }

    @Query("""
            select v.post.id as postId,
                   sum(case when v.voteType = com.skillsync.skillsync.enums.VoteType.UPVOTE then 1 else 0 end) as upvotes,
                   sum(case when v.voteType = com.skillsync.skillsync.enums.VoteType.DOWNVOTE then 1 else 0 end) as downvotes
            from PostVote v
            where v.post.id in :postIds
            group by v.post.id
            """)
    List<PostVoteAgg> aggregateVotesByPostIds(@Param("postIds") Collection<UUID> postIds);

    @Query("""
            select v.post.id
            from PostVote v
            where v.user.id = :userId
              and v.post.id in :postIds
              and v.voteType = com.skillsync.skillsync.enums.VoteType.UPVOTE
            """)
    List<UUID> findUpvotedPostIds(@Param("userId") UUID userId, @Param("postIds") Collection<UUID> postIds);
}
