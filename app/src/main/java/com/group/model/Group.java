package com.group.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "groups")
public class Group {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_id")
    private Long groupId;

    @Column(unique = true, nullable = false)
    private String groupName;

    @Column(name = "description", length = 255)
    private String description;

    @Lob
    @Column(name = "group_picture_blob")
    private byte[] groupPictureBlob;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @Column(name = "member_limit", nullable = false)
    private int memberLimit;

    @Column(name = "member_count", nullable = false)
    private int memberCount;
    @Column(name = "member_limit_reached", nullable = false)
    private boolean memberLimitReached;

    @Column(name = "is_public", nullable = false)
    private boolean isPublic;
   
}
