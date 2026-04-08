package com.skillsync.skillsync.repository;

import com.skillsync.skillsync.entity.ForumCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ForumCategoryRepository extends JpaRepository<ForumCategory, UUID> {
    Optional<ForumCategory> findByNameIgnoreCase(String name);

    boolean existsByName(String name);

    List<ForumCategory> findAllByOrderByDisplayOrderAsc();
}
