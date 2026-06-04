package com.connection.service;

import com.connection.dto.FriendRequest;
import com.connection.model.Connection;
import com.connection.repository.ConnectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.Optional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConnectionService {

    private final ConnectionRepository connectionRepository;

    public List<Connection> getConnections(Long userId) {
        return connectionRepository.findByUserIdOrFriendId(userId, userId);
    }

    public List<Connection> getSentFriendRequests(Long userId) {
        return connectionRepository.findByUserIdAndStatus(userId, "PENDING");
    }

    public List<Connection> getReceivedFriendRequests(Long userId) {
        return connectionRepository.findByFriendIdAndStatus(userId, "PENDING");
    }

    @Transactional
    public Connection sendRequest(FriendRequest request) {
        return sendConnectionRequest(request.getUserId(), request.getFriendId());
    }

    @Transactional
    public Connection sendConnectionRequest(Long userId, Long friendId) {
        if (userId.equals(friendId)) {
            throw new IllegalArgumentException("You cannot send a friend request to yourself.");
        }

        // Check if any connection exists in either direction to prevent duplicates/cross-requests
        Optional<Connection> existingOpt = connectionRepository.findByUserIdAndFriendId(userId, friendId)
                .or(() -> connectionRepository.findByUserIdAndFriendId(friendId, userId));

        if (existingOpt.isPresent()) {
            Connection existing = existingOpt.get();
            String status = existing.getStatus();

            if ("PENDING".equals(status)) {
                if (existing.getUserId().equals(userId)) {
                    throw new IllegalStateException("You have already sent a friend request to this user.");
                } else {
                    throw new IllegalStateException("This user has already sent you a friend request. Please accept it instead.");
                }
            } else if ("ACCEPTED".equals(status)) {
                throw new IllegalStateException("You are already friends with this user.");
            } else if ("BLOCKED".equals(status)) {
                throw new IllegalStateException("This connection is blocked.");
            } else if ("REJECTED".equals(status)) {
                // Re-initiate connection request: set current user as sender
                existing.setUserId(userId);
                existing.setFriendId(friendId);
                existing.setStatus("PENDING");
                existing.setUpdatedAt(System.currentTimeMillis());
                return connectionRepository.save(existing);
            }
        }

        Connection connection = Connection.builder()
                .userId(userId)
                .friendId(friendId)
                .status("PENDING")
                .createdAt(System.currentTimeMillis())
                .updatedAt(System.currentTimeMillis())
                .build();

        return connectionRepository.save(connection);
    }

    @Transactional
    public Connection acceptRequest(FriendRequest request) {
        // request.getFriendId() is the sender of the request, and request.getUserId() is the acceptor.
        Optional<Connection> connectionOptional = connectionRepository.findByUserIdAndFriendId(request.getFriendId(), request.getUserId());

        if (connectionOptional.isEmpty()) {
            throw new RuntimeException("Connection request not found.");
        }

        Connection connection = connectionOptional.get();
        if (!"PENDING".equals(connection.getStatus())) {
            throw new IllegalStateException("Cannot accept a connection that is not in PENDING status.");
        }

        connection.setStatus("ACCEPTED");
        connection.setUpdatedAt(System.currentTimeMillis());
        return connectionRepository.save(connection);
    }

    @Transactional
    public Connection rejectRequest(FriendRequest request) {
        // request.getFriendId() is the sender of the request, and request.getUserId() is the rejector.
        Optional<Connection> connectionOptional = connectionRepository.findByUserIdAndFriendId(request.getFriendId(), request.getUserId());

        if (connectionOptional.isEmpty()) {
            throw new RuntimeException("Connection request not found.");
        }

        Connection connection = connectionOptional.get();
        if (!"PENDING".equals(connection.getStatus())) {
            throw new IllegalStateException("Cannot reject a connection that is not in PENDING status.");
        }

        connection.setStatus("REJECTED");
        connection.setUpdatedAt(System.currentTimeMillis());
        return connectionRepository.save(connection);
    }

    @Transactional
    public Connection blockUser(FriendRequest request) {
        Long blockerId = request.getUserId();
        Long blockedId = request.getFriendId();

        if (blockerId.equals(blockedId)) {
            throw new IllegalArgumentException("You cannot block yourself.");
        }

        // 1. Check existing relationships in both directions
        Optional<Connection> existingForward = connectionRepository.findByUserIdAndFriendId(blockerId, blockedId);
        Optional<Connection> existingReverse = connectionRepository.findByUserIdAndFriendId(blockedId, blockerId);

        // If already blocked by this user, return as-is
        if (existingForward.isPresent() && "BLOCKED".equals(existingForward.get().getStatus())) {
            return existingForward.get();
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
                .createdAt(System.currentTimeMillis())
                .updatedAt(System.currentTimeMillis())
                .build();

        return connectionRepository.save(newBlock);
    }

    @Transactional
    public void unblockUser(FriendRequest request) {
        // Check for a blocked connection where the requesting user is the blocker (userId)
        Optional<Connection> connectionOptional = connectionRepository.findByUserIdAndFriendId(request.getUserId(), request.getFriendId());

        if (connectionOptional.isEmpty() || !"BLOCKED".equals(connectionOptional.get().getStatus())) {
            throw new IllegalStateException("Blocked connection not found, or you are not authorized to unblock this user.");
        }

        connectionRepository.delete(connectionOptional.get());
    }

    @Transactional
    public Connection updateConnectionStatus(Long id, String status) {
        Connection connection = connectionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Connection not found with id: " + id));

        connection.setStatus(status);
        connection.setUpdatedAt(System.currentTimeMillis());

        return connectionRepository.save(connection);
    }

    @Transactional
    public void removeConnection(Long id) {
        if (!connectionRepository.existsById(id)) {
            throw new RuntimeException("Connection not found with id: " + id);
        }
        connectionRepository.deleteById(id);
    }

    public Connection getConnection(Long id) {
        return connectionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Connection not found with id: " + id));
    }
}