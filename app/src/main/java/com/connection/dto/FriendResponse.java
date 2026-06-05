package com.connection.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FriendResponse {
    private Long userId;
    private String username;
    private String email;
    private String profileName;
    private String bio;
    private byte[] profilePictureBlob;
    private boolean isOnline;
    private String lastSeenTimestamp;
    private String status;
    private Long connectedSince;
}
