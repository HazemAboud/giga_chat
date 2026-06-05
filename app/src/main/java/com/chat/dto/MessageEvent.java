package com.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageEvent {
    private String type;
    private String messageId;
    private Long senderId;
    private Long receiverId;
    private Long userId;
    private String emoji;
}
