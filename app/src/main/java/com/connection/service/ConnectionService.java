package com.connection.service;

import com.auth.model.User;
import com.auth.repository.UserRepository;
import com.connection.dto.ConnectionStatusResponse;
import com.connection.dto.FriendRequest;
import com.connection.dto.FriendResponse;
import com.connection.model.Connection;
import com.connection.repository.ConnectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;


import java.time.Instant;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
public class ConnectionService {

    private final ConnectionRepository connectionRepository;
    private final UserRepository userRepository;

    public Page<FriendResponse> getFriends(Long userId, Pageable pageable) {
        return connectionRepository.findAcceptedConnections(userId, pageable)
                .map(c -> {
                    Long otherUserId = c.getUserId().equals(userId) ? c.getFriendId() : c.getUserId();
                    return buildFriendResponse(c, otherUserId, "ACCEPTED");
                });
    }

    public Page<FriendResponse> getSentFriendRequests(Long userId, Pageable pageable) {
        return connectionRepository.findByUserIdAndStatus(userId, "PENDING", pageable)
                .map(c -> buildFriendResponse(c, c.getFriendId(), "PENDING"));
    }

    public Page<FriendResponse> getReceivedFriendRequests(Long userId, Pageable pageable) {
        return connectionRepository.findByFriendIdAndStatus(userId, "PENDING", pageable)
                .map(c -> buildFriendResponse(c, c.getUserId(), "PENDING"));
    }

    private FriendResponse buildFriendResponse(Connection c, Long otherUserId, String status) {
        User other = userRepository.findById(otherUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return FriendResponse.builder()
                .userId(other.getUserId())
                .username(other.getUsername())
                .email(other.getEmail())
                .profileName(other.getProfileName())
                .bio(other.getBio())
                .isOnline(other.isOnline())
                .lastSeenTimestamp(other.getLastSeenTimestamp() != null ? other.getLastSeenTimestamp().toString() : null)
                .status(status)
                .connectedSince(c.getCreatedAt() != null ? c.getCreatedAt().toEpochMilli() : null)
                .build();
    }

    @Transactional
    public Connection sendRequest(FriendRequest request) {
        return sendConnectionRequest(request.getUserId(), request.getFriendId());
    }

    @Transactional
    public Connection sendConnectionRequest(Long userId, Long friendId) {
        if (userId.equals(friendId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot send a friend request to yourself.");
        }

        // Check if any connection exists in either direction to prevent duplicates/cross-requests
        Optional<Connection> existingOpt = connectionRepository.findByUserIdAndFriendId(userId, friendId)
                .or(() -> connectionRepository.findByUserIdAndFriendId(friendId, userId));

        if (existingOpt.isPresent()) {
            Connection existing = existingOpt.get();
            String status = existing.getStatus();

            if ("PENDING".equals(status)) {
                if (existing.getUserId().equals(userId)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You have already sent a friend request to this user.");
                } else {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This user has already sent you a friend request. Please accept it instead.");
                }
            } else if ("ACCEPTED".equals(status)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You are already friends with this user.");
            } else if ("BLOCKED".equals(status)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This connection is blocked.");
            } else if ("REJECTED".equals(status)) {
                // Re-initiate connection request: set current user as sender
                existing.setUserId(userId);
                existing.setFriendId(friendId);
                existing.setStatus("PENDING");
                existing.setUpdatedAt(Instant.now());
                return connectionRepository.save(existing);
            }
        }

        Connection connection = Connection.builder()
                .userId(userId)
                .friendId(friendId)
                .status("PENDING")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        return connectionRepository.save(connection);
    }

    @Transactional
    public Connection acceptRequest(FriendRequest request) {
        Optional<Connection> connectionOptional = connectionRepository.findByUserIdAndFriendId(request.getFriendId(), request.getUserId());

        if (connectionOptional.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Connection request not found.");
        }

        Connection connection = connectionOptional.get();
        if (!"PENDING".equals(connection.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot accept a connection that is not in PENDING status.");
        }

        connection.setStatus("ACCEPTED");
        connection.setUpdatedAt(Instant.now());
        return connectionRepository.save(connection);
    }

    @Transactional
    public Connection rejectRequest(FriendRequest request) {
        Optional<Connection> connectionOptional = connectionRepository.findByUserIdAndFriendId(request.getFriendId(), request.getUserId());

        if (connectionOptional.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Connection request not found.");
        }

        Connection connection = connectionOptional.get();
        if (!"PENDING".equals(connection.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot reject a connection that is not in PENDING status.");
        }

        connection.setStatus("REJECTED");
        connection.setUpdatedAt(Instant.now());
        return connectionRepository.save(connection);
    }

    @Transactional
    public Connection blockUser(FriendRequest request) {
        Long blockerId = request.getUserId();
        Long blockedId = request.getFriendId();

        if (blockerId.equals(blockedId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot block yourself.");
        }

        // 1. Check existing relationships in both directions
        Optional<Connection> existingForward = connectionRepository.findByUserIdAndFriendId(blockerId, blockedId);
        Optional<Connection> existingReverse = connectionRepository.findByUserIdAndFriendId(blockedId, blockerId);

        if (existingForward.isPresent() && "BLOCKED".equals(existingForward.get().getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This user is already blocked.");
        }

        // 2. Delete any non-blocked relationship in either direction
        existingForward.ifPresent(conn -> {
            if (!"BLOCKED".equals(conn.getStatus())) {
                connectionRepository.delete(conn);
            }
        });

        existingReverse.ifPresent(conn -> {
            if (!"BLOCKED".equals(conn.getStatus())) {
                connectionRepository.delete(conn);
            }
        });

        connectionRepository.flush();

        // 3. Create the new directed block relation from blocker to blocked
        Connection newBlock = Connection.builder()
                .userId(blockerId)
                .friendId(blockedId)
                .status("BLOCKED")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        return connectionRepository.save(newBlock);
    }

    @Transactional
    public void unblockUser(FriendRequest request) {
        Optional<Connection> connectionOptional = connectionRepository.findByUserIdAndFriendId(request.getUserId(), request.getFriendId());

        if (connectionOptional.isEmpty() || !"BLOCKED".equals(connectionOptional.get().getStatus())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Blocked connection not found.");
        }

        connectionRepository.delete(connectionOptional.get());
    }

    @Transactional
    public void unfriend(Long userId, Long friendId) {
        Optional<Connection> existing = connectionRepository.findByUserIdAndFriendId(userId, friendId)
                .or(() -> connectionRepository.findByUserIdAndFriendId(friendId, userId));

        Connection conn = existing.filter(c -> "ACCEPTED".equals(c.getStatus()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Friendship not found."));

        connectionRepository.delete(conn);
    }

    public Page<FriendResponse> getBlockedUsers(Long userId, Pageable pageable) {
        return connectionRepository.findByUserIdAndStatus(userId, "BLOCKED", pageable)
                .map(c -> buildFriendResponse(c, c.getFriendId(), "BLOCKED"));
    }

    public ConnectionStatusResponse getConnectionStatus(Long userId, Long otherUserId) {
        Optional<Connection> forward = connectionRepository.findConnection(userId, otherUserId);
        if (forward.isPresent()) {
            String status = forward.get().getStatus();
            if ("ACCEPTED".equals(status)) return new ConnectionStatusResponse("FRIENDS");
            if ("PENDING".equals(status)) return new ConnectionStatusResponse("REQUEST_SENT");
            if ("BLOCKED".equals(status)) return new ConnectionStatusResponse("BLOCKED_BY_ME");
        }

        Optional<Connection> reverse = connectionRepository.findConnection(otherUserId, userId);
        if (reverse.isPresent()) {
            String status = reverse.get().getStatus();
            if ("PENDING".equals(status)) return new ConnectionStatusResponse("REQUEST_RECEIVED");
            if ("BLOCKED".equals(status)) return new ConnectionStatusResponse("BLOCKED_BY_THEM");
        }

        return new ConnectionStatusResponse("NONE");
    }

}