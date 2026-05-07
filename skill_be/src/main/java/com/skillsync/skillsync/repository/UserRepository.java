package com.skillsync.skillsync.repository;

import com.skillsync.skillsync.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    boolean existsByEmail(String email);
    Optional<User> findByEmail(String email);
    long countByRole(com.skillsync.skillsync.enums.Role role);
    long countByStatus(com.skillsync.skillsync.enums.UserStatus status);

    /** Tìm user theo tên hoặc email — dùng cho @mention dropdown */
    @Query("SELECT u FROM User u WHERE " +
           "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "ORDER BY u.fullName ASC")
    List<User> searchForMention(@Param("q") String q, Pageable pageable);

    @Query("SELECT u FROM User u WHERE " +
           "(:search IS NULL OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))")
    org.springframework.data.domain.Page<User> searchAllForAdmin(@Param("search") String search, Pageable pageable);
}
