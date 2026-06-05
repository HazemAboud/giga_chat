package com.chat.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.chat.dto.MessageEvent;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WebSocketNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public void notifyNewMessage(String messageId, Long senderId, Long receiverId) {
        MessageEvent event = MessageEvent.builder()
                .type("NEW_MESSAGE")
                .messageId(messageId)
                .senderId(senderId)
                .receiverId(receiverId)
                .build();
        messagingTemplate.convertAndSendToUser(
                receiverId.toString(), "/queue/messages", event);
        messagingTemplate.convertAndSendToUser(
                senderId.toString(), "/queue/messages", event);
    }

    public void notifyReactionAdded(String messageId, Long userId, String emoji, Long receiverId) {
        MessageEvent event = MessageEvent.builder()
                .type("REACTION_ADDED")
                .messageId(messageId)
                .userId(userId)
                .emoji(emoji)
                .build();
        messagingTemplate.convertAndSendToUser(
                receiverId.toString(), "/queue/messages", event);
    }

    public void notifyReactionRemoved(String messageId, Long userId, Long receiverId) {
        MessageEvent event = MessageEvent.builder()
                .type("REACTION_REMOVED")
                .messageId(messageId)
                .userId(userId)
                .build();
        messagingTemplate.convertAndSendToUser(
                receiverId.toString(), "/queue/messages", event);
    }
}
