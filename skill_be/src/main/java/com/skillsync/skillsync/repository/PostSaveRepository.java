package com.skillsync.skillsync.repository;

import com.skillsync.skillsync.entity.PostSave;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PostSaveRepository extends JpaRepository<PostSave, UUID> {
    Optional<PostSave> findByPostIdAndUserId(UUID postId, UUID userId);

    Page<PostSave> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    boolean existsByPostIdAndUserId(UUID postId, UUID userId);

    void deleteByPostIdAndUserId(UUID postId, UUID userId);

    Long countByPostId(UUID postId);

    interface PostSaveAgg {
        UUID getPostId();

        Long getSaveCount();
    }

    @Query("""
            select s.post.id as postId,
                   count(s.id) as saveCount
            from PostSave s
            where s.post.id in :postIds
            group by s.post.id
            """)
    List<PostSaveAgg> aggregateSavesByPostIds(@Param("postIds") Collection<UUID> postIds);

    @Query("""
            select s.post.id
            from PostSave s
            where s.user.id = :userId
              and s.post.id in :postIds
            """)
    List<UUID> findSavedPostIds(@Param("userId") UUID userId, @Param("postIds") Collection<UUID> postIds);
}
