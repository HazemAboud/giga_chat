package com.connection.repository;

import com.connection.model.Connection;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConnectionRepository extends JpaRepository<Connection, Long> {
    List<Connection> findByUserIdOrFriendId(Long userId, Long friendId);
    List<Connection> findByUserIdAndStatus(Long userId, String status);
    List<Connection> findByFriendIdAndStatus(Long friendId, String status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Connection> findByUserIdAndFriendId(Long userId, Long friendId);
}