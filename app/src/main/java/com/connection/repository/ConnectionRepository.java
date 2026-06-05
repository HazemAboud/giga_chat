package com.connection.repository;

import com.connection.model.Connection;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConnectionRepository extends JpaRepository<Connection, Long> {
    List<Connection> findByUserIdOrFriendId(Long userId, Long friendId);
    List<Connection> findByUserIdAndStatus(Long userId, String status);
    List<Connection> findByFriendIdAndStatus(Long friendId, String status);

    Page<Connection> findByUserIdAndStatus(Long userId, String status, Pageable pageable);
    Page<Connection> findByFriendIdAndStatus(Long friendId, String status, Pageable pageable);

    @Query("SELECT c FROM Connection c WHERE (c.userId = :userId OR c.friendId = :userId) AND c.status = 'ACCEPTED'")
    Page<Connection> findAcceptedConnections(@Param("userId") Long userId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Connection> findByUserIdAndFriendId(Long userId, Long friendId);

    @Query("SELECT c FROM Connection c WHERE c.userId = :userId AND c.friendId = :friendId")
    Optional<Connection> findConnection(@Param("userId") Long userId, @Param("friendId") Long friendId);
}