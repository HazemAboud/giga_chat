package com.connection.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.auth.model.User;
import com.connection.dto.ConnectionStatusResponse;
import com.connection.dto.FriendRequest;
import com.connection.dto.FriendResponse;
import com.connection.service.ConnectionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/connections")
public class ConnController {

    private final ConnectionService connService;

    public ConnController(ConnectionService connService) {
        this.connService = connService;
    }

    @PostMapping("/request")
    public ResponseEntity<Void> connect(@AuthenticationPrincipal User loggedInUser, @Valid @RequestBody FriendRequest request) {
        // Enforce that the initiator is always the currently authenticated user
        request.setUserId(loggedInUser.getUserId());
        connService.sendRequest(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/accept")
    public ResponseEntity<Void> accept(@AuthenticationPrincipal User loggedInUser, @Valid @RequestBody FriendRequest request) {
        // Enforce that the acceptor is always the currently authenticated user
        request.setUserId(loggedInUser.getUserId());
        connService.acceptRequest(request);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/reject")
    public ResponseEntity<Void> reject(@AuthenticationPrincipal User loggedInUser, @Valid @RequestBody FriendRequest request) {
        // Enforce that the rejector is always the currently authenticated user
        request.setUserId(loggedInUser.getUserId());
        connService.rejectRequest(request);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/block")
    public ResponseEntity<Void> block(@AuthenticationPrincipal User loggedInUser, @Valid @RequestBody FriendRequest request) {
        // Enforce that the blocker is always the currently authenticated user
        request.setUserId(loggedInUser.getUserId());
        connService.blockUser(request);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/unblock")
    public ResponseEntity<Void> unblock(@AuthenticationPrincipal User loggedInUser, @Valid @RequestBody FriendRequest request) {
        // Enforce that the unblocker is always the currently authenticated user
        request.setUserId(loggedInUser.getUserId());
        connService.unblockUser(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<List<FriendResponse>> getFriends(@AuthenticationPrincipal User loggedInUser) {
        return ResponseEntity.ok(connService.getFriends(loggedInUser.getUserId()));
    }

    @GetMapping("/requests/sent")
    public ResponseEntity<List<FriendResponse>> getSentRequests(@AuthenticationPrincipal User loggedInUser) {
        return ResponseEntity.ok(connService.getSentFriendRequests(loggedInUser.getUserId()));
    }

    @GetMapping("/requests/received")
    public ResponseEntity<List<FriendResponse>> getReceivedRequests(@AuthenticationPrincipal User loggedInUser) {
        return ResponseEntity.ok(connService.getReceivedFriendRequests(loggedInUser.getUserId()));
    }

    @DeleteMapping("/friend/{friendId}")
    public ResponseEntity<Void> unfriend(@AuthenticationPrincipal User loggedInUser, @PathVariable Long friendId) {
        connService.unfriend(loggedInUser.getUserId(), friendId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/blocked")
    public ResponseEntity<List<FriendResponse>> getBlocked(@AuthenticationPrincipal User loggedInUser) {
        return ResponseEntity.ok(connService.getBlockedUsers(loggedInUser.getUserId()));
    }

    @GetMapping("/status/{userId}")
    public ResponseEntity<ConnectionStatusResponse> getConnectionStatus(
            @AuthenticationPrincipal User loggedInUser,
            @PathVariable Long userId
    ) {
        return ResponseEntity.ok(connService.getConnectionStatus(loggedInUser.getUserId(), userId));
    }

}
