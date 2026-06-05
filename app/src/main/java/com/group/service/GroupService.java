package com.group.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.List;

import javax.imageio.ImageIO;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
        if (groupRepo.findByGroupName(group.getGroupName()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Group name already exists");
        }
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

    @Transactional
    public Group updateGroup(Long groupId, String groupName, String description, Integer memberLimit) {
        Group group = getGroupById(groupId);
        if (groupName != null && !groupName.equals(group.getGroupName())) {
            if (groupRepo.findByGroupName(groupName).isPresent()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Group name already exists");
            }
            group.setGroupName(groupName);
        }
        if (description != null) {
            group.setDescription(description);
        }
        if (memberLimit != null) {
            group.setMemberLimit(memberLimit);
            group.setMemberLimitReached(memberLimit > 0 && group.getMemberCount() >= memberLimit);
        }
        return groupRepo.save(group);
    }

    @Transactional
    public void deleteGroup(Long groupId) {
        Group group = getGroupById(groupId);
        memberRepo.deleteAll(memberRepo.findByGroup_GroupId(groupId));
        groupRepo.delete(group);
    }

    public List<Group> searchGroups(String query) {
        return groupRepo.searchPublicGroups(query);
    }

    public Page<Group> searchGroups(String query, Pageable pageable) {
        return groupRepo.searchPublicGroups(query, pageable);
    }

    public Group getGroupById(Long groupId) {
        return groupRepo.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));
    }

    public List<Group> getAllPublicGroups() {
        return groupRepo.findByIsPublicTrue();
    }

    public Page<Group> getAllPublicGroups(Pageable pageable) {
        return groupRepo.findByIsPublicTrue(pageable);
    }

    public List<Group> getMyGroups(Long userId) {
        return memberRepo.findGroupsByUserId(userId);
    }

    public Page<Group> getMyGroups(Long userId, Pageable pageable) {
        return memberRepo.findGroupsByUserId(userId, pageable);
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
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only image files are allowed");
        }
        Group group = getGroupById(groupId);
        group.setGroupPictureBlob(convertToPng(file));
        groupRepo.save(group);
    }

    private byte[] convertToPng(MultipartFile file) throws IOException {
        BufferedImage image = ImageIO.read(file.getInputStream());
        if (image == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid image file");
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return baos.toByteArray();
    }

    @Transactional
    public void requestJoin(Long groupId, Long userId) {
        Group group = getGroupById(groupId);
        if (!group.isPublic()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Group is not public");
        }
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
            if (group.getMemberLimit() > 0 && group.getMemberCount() >= group.getMemberLimit()) {
                group.setMemberLimitReached(true);
            }
            groupRepo.save(group);
        } else {
            memberRepo.delete(member);
        }
    }

    @Transactional
    public void leaveGroup(Long groupId, Long userId) {
        GroupMembers member = memberRepo.findByGroup_GroupIdAndUser_UserId(groupId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not a member of this group"));

        if ("ADMIN".equals(member.getRole())) {
            long adminCount = memberRepo.findByGroup_GroupIdAndRole(groupId, "ADMIN").size();
            if (adminCount <= 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot leave group as the only admin");
            }
        }

        memberRepo.delete(member);
        Group group = getGroupById(groupId);
        group.setMemberCount(Math.max(0, group.getMemberCount() - 1));
        group.setMemberLimitReached(false);
        groupRepo.save(group);
    }

    @Transactional
    public void addMember(Long groupId, Long userId, String role) {
        if (!Group.VALID_ROLES.contains(role)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role: " + role);
        }
        if (memberRepo.existsByGroup_GroupIdAndUser_UserId(groupId, userId)) return;
        
        Group group = getGroupById(groupId);
        if (group.getMemberLimit() > 0 && group.getMemberCount() >= group.getMemberLimit()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Group is full");
        }

        GroupMembers member = GroupMembers.builder()
                .group(group)
                .user(userRepository.getReferenceById(userId))
                .role(role)
                .joinedAt(Instant.now())
                .build();
        memberRepo.save(member);
        group.setMemberCount(group.getMemberCount() + 1);
        if (group.getMemberLimit() > 0 && group.getMemberCount() >= group.getMemberLimit()) {
            group.setMemberLimitReached(true);
        }
        groupRepo.save(group);
    }

    @Transactional
    public void updateRole(Long groupId, Long userId, String newRole) {
        if (!Group.VALID_ROLES.contains(newRole)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role: " + newRole);
        }
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
    public Page<GroupMembers> getMembers(Long groupId, Pageable pageable) { return memberRepo.findByGroup_GroupId(groupId, pageable); }
    public List<GroupMembers> getPendingRequests(Long groupId) { return memberRepo.findByGroup_GroupIdAndRole(groupId, "PENDING"); }
    public Page<GroupMembers> getPendingRequests(Long groupId, Pageable pageable) { return memberRepo.findByGroup_GroupIdAndRole(groupId, "PENDING", pageable); }
}