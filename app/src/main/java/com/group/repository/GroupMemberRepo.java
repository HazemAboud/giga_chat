package com.group.repository;

import com.group.model.GroupMembers;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupMemberRepo extends JpaRepository<GroupMembers, Long> {
    List<GroupMembers> findByGroup_GroupId(Long groupId);
    List<GroupMembers> findByUser_UserId(Long userId);
    Optional<GroupMembers> findByGroup_GroupIdAndUser_UserId(Long groupId, Long userId);
    List<GroupMembers> findByGroup_GroupIdAndRole(Long groupId, String role);
    void deleteByGroup_GroupIdAndUser_UserId(Long groupId, Long userId);
    boolean existsByGroup_GroupIdAndUser_UserId(Long groupId, Long userId);
}