package com.group.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.group.model.Group;
import com.group.model.GroupMembers;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupMemberRepo extends JpaRepository<GroupMembers, Long> {
    List<GroupMembers> findByGroup_GroupId(Long groupId);
    Page<GroupMembers> findByGroup_GroupId(Long groupId, Pageable pageable);
    List<GroupMembers> findByUser_UserId(Long userId);
    Optional<GroupMembers> findByGroup_GroupIdAndUser_UserId(Long groupId, Long userId);
    List<GroupMembers> findByGroup_GroupIdAndRole(Long groupId, String role);
    Page<GroupMembers> findByGroup_GroupIdAndRole(Long groupId, String role, Pageable pageable);
    boolean existsByGroup_GroupIdAndUser_UserId(Long groupId, Long userId);

    @Query("SELECT gm.group FROM GroupMembers gm WHERE gm.user.userId = :userId")
    List<Group> findGroupsByUserId(@Param("userId") Long userId);

    @Query("SELECT gm.group FROM GroupMembers gm WHERE gm.user.userId = :userId")
    Page<Group> findGroupsByUserId(@Param("userId") Long userId, Pageable pageable);
}