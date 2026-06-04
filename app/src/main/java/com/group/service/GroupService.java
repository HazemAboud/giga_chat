package com.group.service;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.auth.repository.UserRepository;
import com.group.model.Group;
import com.group.model.GroupMembers;
import com.group.repository.GroupMemberRepo;
import com.group.repository.GroupRepo;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepo groupRepo;
    private final GroupMemberRepo memberRepo;
    private final UserRepository userRepository;

    @Transactional
    public Group createGroup(Group group, Long creatorId) {
        group.setCreatedAt(Instant.now());
        group.setMemberCount(1);
        group.setMemberLimitReached(false);
        Group savedGroup = groupRepo.save(group);

        GroupMembers admin = GroupMembers.builder()
                .group(savedGroup)
                .user(userRepository.getReferenceById(creatorId))
                .role("ADMIN")
                .joinedAt(Instant.now())
                .build();
        memberRepo.save(admin);
        
        return savedGroup;
    }

    public List<Group> searchGroups(String query) {
        return groupRepo.searchPublicGroups(query);
    }

    public Group getGroupById(Long groupId) {
        return groupRepo.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));
    }

    public List<Group> getAllPublicGroups() {
        return groupRepo.findByIsPublicTrue();
    }

    public List<Group> getMyGroups(Long userId) {
        List<Long> groupIds = memberRepo.findByUser_UserId(userId).stream()
                .map(m -> m.getGroup().getGroupId())
                .collect(Collectors.toList());
        return groupRepo.findAllById(groupIds);
    }

    public byte[] getGroupPicture(Long groupId) {
        Group group = getGroupById(groupId);
        if (group.getGroupPictureBlob() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No picture set for this group");
        }
        return group.getGroupPictureBlob();
    }

    @Transactional
    public void uploadGroupPicture(Long groupId, MultipartFile file) throws IOException {
        Group group = getGroupById(groupId);
        group.setGroupPictureBlob(file.getBytes());
        groupRepo.save(group);
    }

    @Transactional
    public void requestJoin(Long groupId, Long userId) {
        Group group = getGroupById(groupId);
        if (group.isMemberLimitReached()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Group is full");
        }
        if (memberRepo.existsByGroup_GroupIdAndUser_UserId(groupId, userId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Already a member or request pending");
        }

        GroupMembers request = GroupMembers.builder()
                .group(group)
                .user(userRepository.getReferenceById(userId))
                .role("PENDING")
                .joinedAt(Instant.now())
                .build();
        memberRepo.save(request);
    }

    @Transactional
    public void handleJoinRequest(Long groupId, Long userId, boolean approve) {
        GroupMembers member = memberRepo.findByGroup_GroupIdAndUser_UserId(groupId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));

        if (!"PENDING".equals(member.getRole())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not a pending request");
        }

        if (approve) {
            Group group = getGroupById(groupId);
            member.setRole("MEMBER");
            memberRepo.save(member);
            
            group.setMemberCount(group.getMemberCount() + 1);
            if (group.getMemberCount() >= group.getMemberLimit()) {
                group.setMemberLimitReached(true);
            }
            groupRepo.save(group);
        } else {
            memberRepo.delete(member);
        }
    }

    @Transactional
    public void leaveGroup(Long groupId, Long userId) {
        memberRepo.findByGroup_GroupIdAndUser_UserId(groupId, userId).ifPresent(member -> {
            memberRepo.delete(member);
            Group group = getGroupById(groupId);
            group.setMemberCount(Math.max(0, group.getMemberCount() - 1));
            group.setMemberLimitReached(false);
            groupRepo.save(group);
        });
    }

    @Transactional
    public void addMember(Long groupId, Long userId, String role) {
        if (memberRepo.existsByGroup_GroupIdAndUser_UserId(groupId, userId)) return;
        
        Group group = getGroupById(groupId);
        GroupMembers member = GroupMembers.builder()
                .group(group)
                .user(userRepository.getReferenceById(userId))
                .role(role)
                .joinedAt(Instant.now())
                .build();
        memberRepo.save(member);
        group.setMemberCount(group.getMemberCount() + 1);
        groupRepo.save(group);
    }

    @Transactional
    public void updateRole(Long groupId, Long userId, String newRole) {
        GroupMembers member = memberRepo.findByGroup_GroupIdAndUser_UserId(groupId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));
        member.setRole(newRole);
        memberRepo.save(member);
    }

    @Transactional
    public void removeMember(Long groupId, Long userId) {
        GroupMembers member = memberRepo.findByGroup_GroupIdAndUser_UserId(groupId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));
        memberRepo.delete(member);
        Group group = getGroupById(groupId);
        group.setMemberCount(Math.max(0, group.getMemberCount() - 1));
        group.setMemberLimitReached(false);
        groupRepo.save(group);
    }

    public void requireAdmin(Long groupId, Long userId) {
        memberRepo.findByGroup_GroupIdAndUser_UserId(groupId, userId)
                .filter(m -> "ADMIN".equals(m.getRole()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Only group admins can perform this action"));
    }

    public List<GroupMembers> getMembers(Long groupId) { return memberRepo.findByGroup_GroupId(groupId); }
    public List<GroupMembers> getPendingRequests(Long groupId) { return memberRepo.findByGroup_GroupIdAndRole(groupId, "PENDING"); }
}