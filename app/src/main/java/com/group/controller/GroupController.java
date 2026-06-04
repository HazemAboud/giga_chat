package com.group.controller;

import java.io.IOException;

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
import com.group.service.GroupService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    @PostMapping("/create")
    public ResponseEntity<Group> createGroup(@AuthenticationPrincipal User user, @RequestBody Group group) {
        return ResponseEntity.ok(groupService.createGroup(group, user.getUserId()));
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchGroups(@RequestParam String query) {
        return ResponseEntity.ok(groupService.searchGroups(query));
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<?> getGroupDetails(@PathVariable Long groupId) {
        return ResponseEntity.ok(groupService.getGroupById(groupId));
    }

    @GetMapping("/list")
    public ResponseEntity<?> listGroups() {
        return ResponseEntity.ok(groupService.getAllPublicGroups());
    }

    @GetMapping("/my-groups")
    public ResponseEntity<?> listMyGroups(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(groupService.getMyGroups(user.getUserId()));
    }

    @GetMapping("/{groupId}/members")
    public ResponseEntity<?> listGroupMembers(@PathVariable Long groupId) {
        return ResponseEntity.ok(groupService.getMembers(groupId));
    }

    @GetMapping("/{groupId}/picture")
    public ResponseEntity<byte[]> getGroupPicture(@PathVariable Long groupId) {
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(groupService.getGroupPicture(groupId));
    }

    @PostMapping("/{groupId}/picture")
    public ResponseEntity<?> uploadGroupPicture(@AuthenticationPrincipal User user, @PathVariable Long groupId, @RequestParam("file") MultipartFile file) throws IOException {
        groupService.requireAdmin(groupId, user.getUserId());
        groupService.uploadGroupPicture(groupId, file);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{groupId}/request-join")
    public ResponseEntity<?> requestJoinGroup(@AuthenticationPrincipal User user, @PathVariable Long groupId) {
        groupService.requestJoin(groupId, user.getUserId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{groupId}/requests")
    public ResponseEntity<?> listPendingJoinRequests(@AuthenticationPrincipal User user, @PathVariable Long groupId) {
        groupService.requireAdmin(groupId, user.getUserId());
        return ResponseEntity.ok(groupService.getPendingRequests(groupId));
    }

    @PutMapping("/{groupId}/approve/{userId}")
    public ResponseEntity<?> approveJoinRequest(@AuthenticationPrincipal User user, @PathVariable Long groupId, @PathVariable Long userId) {
        groupService.requireAdmin(groupId, user.getUserId());
        groupService.handleJoinRequest(groupId, userId, true);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{groupId}/reject/{userId}")
    public ResponseEntity<?> rejectJoinRequest(@AuthenticationPrincipal User user, @PathVariable Long groupId, @PathVariable Long userId) {
        groupService.requireAdmin(groupId, user.getUserId());
        groupService.handleJoinRequest(groupId, userId, false);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{groupId}/leave")
    public ResponseEntity<?> leaveGroup(@AuthenticationPrincipal User user, @PathVariable Long groupId) {
        groupService.leaveGroup(groupId, user.getUserId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{groupId}/add/{userId}")
    public ResponseEntity<?> addMemberToGroup(@AuthenticationPrincipal User user, @PathVariable Long groupId, @PathVariable Long userId, @RequestParam String role) {
        groupService.requireAdmin(groupId, user.getUserId());
        groupService.addMember(groupId, userId, role);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{groupId}/role/{userId}")
    public ResponseEntity<?> updateMemberRole(@AuthenticationPrincipal User user, @PathVariable Long groupId, @PathVariable Long userId, @RequestParam String role) {
        groupService.requireAdmin(groupId, user.getUserId());
        groupService.updateRole(groupId, userId, role);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{groupId}/remove/{userId}")
    public ResponseEntity<?> removeMember(@AuthenticationPrincipal User user, @PathVariable Long groupId, @PathVariable Long userId) {
        groupService.requireAdmin(groupId, user.getUserId());
        groupService.removeMember(groupId, userId);
        return ResponseEntity.ok().build();
    }
}
