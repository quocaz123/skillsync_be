package com.skillsync.skillsync.repository;

import com.skillsync.skillsync.entity.PostVote;
import com.skillsync.skillsync.enums.VoteType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PostVoteRepository extends JpaRepository<PostVote, UUID> {
    Optional<PostVote> findByPostIdAndUserId(UUID postId, UUID userId);

    Long countByPostIdAndVoteType(UUID postId, VoteType voteType);

    Integer countByPostId(UUID postId);

    boolean existsByPostIdAndUserId(UUID postId, UUID userId);

    void deleteByPostIdAndUserId(UUID postId, UUID userId);
}
