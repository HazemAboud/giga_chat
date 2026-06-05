package com.group.controller;

import java.io.IOException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.auth.model.User;
import com.group.model.Group;
import com.group.model.GroupMembers;
import com.group.service.GroupService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    @PostMapping("/create")
    public ResponseEntity<Group> createGroup(@AuthenticationPrincipal User user, @Valid @RequestBody Group group) {
        return ResponseEntity.ok(groupService.createGroup(group, user.getUserId()));
    }

    @PutMapping("/{groupId}")
    public ResponseEntity<Void> updateGroup(
            @AuthenticationPrincipal User user,
            @PathVariable Long groupId,
            @RequestParam(required = false) String groupName,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Integer memberLimit) {
        groupService.requireAdmin(groupId, user.getUserId());
        groupService.updateGroup(groupId, groupName, description, memberLimit);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{groupId}")
    public ResponseEntity<Void> deleteGroup(@AuthenticationPrincipal User user, @PathVariable Long groupId) {
        groupService.requireAdmin(groupId, user.getUserId());
        groupService.deleteGroup(groupId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public ResponseEntity<Page<Group>> searchGroups(
            @RequestParam String query,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(groupService.searchGroups(query, pageable));
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<Group> getGroupDetails(@PathVariable Long groupId) {
        return ResponseEntity.ok(groupService.getGroupById(groupId));
    }

    @GetMapping("/list")
    public ResponseEntity<Page<Group>> listGroups(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(groupService.getAllPublicGroups(pageable));
    }

    @GetMapping("/my-groups")
    public ResponseEntity<Page<Group>> listMyGroups(
            @AuthenticationPrincipal User user,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(groupService.getMyGroups(user.getUserId(), pageable));
    }

    @GetMapping("/{groupId}/members")
    public ResponseEntity<Page<GroupMembers>> listGroupMembers(
            @PathVariable Long groupId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(groupService.getMembers(groupId, pageable));
    }

    @GetMapping("/{groupId}/requests")
    public ResponseEntity<Page<GroupMembers>> listPendingRequests(
            @AuthenticationPrincipal User user,
            @PathVariable Long groupId,
            @PageableDefault(size = 20) Pageable pageable) {
        groupService.requireAdmin(groupId, user.getUserId());
        return ResponseEntity.ok(groupService.getPendingRequests(groupId, pageable));
    }

    @GetMapping("/{groupId}/picture")
    public ResponseEntity<byte[]> getGroupPicture(@PathVariable Long groupId) {
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(groupService.getGroupPicture(groupId));
    }

    @PostMapping("/{groupId}/picture")
    public ResponseEntity<Void> uploadGroupPicture(@AuthenticationPrincipal User user, @PathVariable Long groupId, @RequestParam("file") MultipartFile file) throws IOException {
        groupService.requireAdmin(groupId, user.getUserId());
        groupService.uploadGroupPicture(groupId, file);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{groupId}/request-join")
    public ResponseEntity<Void> requestJoinGroup(@AuthenticationPrincipal User user, @PathVariable Long groupId) {
        groupService.requestJoin(groupId, user.getUserId());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{groupId}/approve/{userId}")
    public ResponseEntity<Void> approveJoinRequest(@AuthenticationPrincipal User user, @PathVariable Long groupId, @PathVariable Long userId) {
        groupService.requireAdmin(groupId, user.getUserId());
        groupService.handleJoinRequest(groupId, userId, true);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{groupId}/reject/{userId}")
    public ResponseEntity<Void> rejectJoinRequest(@AuthenticationPrincipal User user, @PathVariable Long groupId, @PathVariable Long userId) {
        groupService.requireAdmin(groupId, user.getUserId());
        groupService.handleJoinRequest(groupId, userId, false);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{groupId}/leave")
    public ResponseEntity<Void> leaveGroup(@AuthenticationPrincipal User user, @PathVariable Long groupId) {
        groupService.leaveGroup(groupId, user.getUserId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{groupId}/add/{userId}")
    public ResponseEntity<Void> addMemberToGroup(@AuthenticationPrincipal User user, @PathVariable Long groupId, @PathVariable Long userId, @RequestParam String role) {
        groupService.requireAdmin(groupId, user.getUserId());
        groupService.addMember(groupId, userId, role);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{groupId}/role/{userId}")
    public ResponseEntity<Void> updateMemberRole(@AuthenticationPrincipal User user, @PathVariable Long groupId, @PathVariable Long userId, @RequestParam String role) {
        groupService.requireAdmin(groupId, user.getUserId());
        groupService.updateRole(groupId, userId, role);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{groupId}/remove/{userId}")
    public ResponseEntity<Void> removeMember(@AuthenticationPrincipal User user, @PathVariable Long groupId, @PathVariable Long userId) {
        groupService.requireAdmin(groupId, user.getUserId());
        groupService.removeMember(groupId, userId);
        return ResponseEntity.ok().build();
    }
}
