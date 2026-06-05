package com.chat.controller;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.auth.model.User;
import com.chat.dto.ReactionRequest;
import com.chat.model.Message;
import com.chat.service.MessageService;
import com.chat.service.WebSocketNotificationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;
    private final WebSocketNotificationService notificationService;

    @PostMapping
    public ResponseEntity<String> sendMessage(
            @AuthenticationPrincipal User loggedInUser,
            @RequestParam("receiverId") Long receiverId,
            @RequestParam(value = "textContent", required = false) String textContent,
            @RequestParam(value = "file", required = false) MultipartFile file
    ) throws ExecutionException, InterruptedException, IOException {
        if ((textContent == null || textContent.isBlank()) && (file == null || file.isEmpty())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message must have text content or a file");
        }
        messageService.checkReceiverExists(receiverId);
        messageService.checkFriendship(loggedInUser.getUserId(), receiverId);

        Message msg = Message.builder()
                .senderId(loggedInUser.getUserId())
                .receiverId(receiverId)
                .textContent(textContent)
                .imageBlob(file != null ? file.getBytes() : null)
                .build();
        String messageId = messageService.saveMessage(msg);
        notificationService.notifyNewMessage(messageId, loggedInUser.getUserId(), receiverId);
        return ResponseEntity.status(HttpStatus.CREATED).body(messageId);
    }

    @GetMapping
    public ResponseEntity<List<Message>> getConversation(
            @AuthenticationPrincipal User loggedInUser,
            @RequestParam Long userId,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(required = false) String lastMessageId
    ) throws ExecutionException, InterruptedException {
        return ResponseEntity.ok(messageService.getConversation(loggedInUser.getUserId(), userId, limit, lastMessageId));
    }

    @PostMapping("/{messageId}/reactions")
    public ResponseEntity<Void> addReaction(
            @AuthenticationPrincipal User loggedInUser,
            @PathVariable String messageId,
            @Valid @RequestBody ReactionRequest request
    ) throws ExecutionException, InterruptedException {
        Message msg = messageService.getMessage(messageId);
        messageService.addReaction(messageId, loggedInUser.getUserId(), request.getEmoji());
        Long otherParty = msg.getSenderId().equals(loggedInUser.getUserId()) ? msg.getReceiverId() : msg.getSenderId();
        notificationService.notifyReactionAdded(messageId, loggedInUser.getUserId(), request.getEmoji(), otherParty);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{messageId}/reactions")
    public ResponseEntity<Void> removeReaction(
            @AuthenticationPrincipal User loggedInUser,
            @PathVariable String messageId
    ) throws ExecutionException, InterruptedException {
        Message msg = messageService.getMessage(messageId);
        messageService.removeReaction(messageId, loggedInUser.getUserId());
        Long otherParty = msg.getSenderId().equals(loggedInUser.getUserId()) ? msg.getReceiverId() : msg.getSenderId();
        notificationService.notifyReactionRemoved(messageId, loggedInUser.getUserId(), otherParty);
        return ResponseEntity.ok().build();
    }
}
