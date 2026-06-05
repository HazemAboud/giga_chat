package com.group.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.group.model.Group;

@Repository
public interface GroupRepo extends JpaRepository<Group, Long> {
    Optional<Group> findByGroupName(String groupName);
    List<Group> findByIsPublicTrue();
    Page<Group> findByIsPublicTrue(Pageable pageable);

    @Query("SELECT g FROM Group g WHERE g.isPublic = true AND (LOWER(g.groupName) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(g.description) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<Group> searchPublicGroups(@Param("query") String query);

    @Query("SELECT g FROM Group g WHERE g.isPublic = true AND (LOWER(g.groupName) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(g.description) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Group> searchPublicGroups(@Param("query") String query, Pageable pageable);
}
