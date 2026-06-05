package com.chat.service;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.chat.model.Message;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final Firestore firestore;

    public String saveMessage(Message msg) throws ExecutionException, InterruptedException {
        DocumentReference docRef = firestore.collection(Message.COLLECTION_NAME).document();
        msg.setMessageId(docRef.getId());
        docRef.set(msg).get();
        return docRef.getId();
    }

    public List<Message> getConversation(Long user1, Long user2) throws ExecutionException, InterruptedException {
        Query query = firestore.collection(Message.COLLECTION_NAME)
                .whereIn("senderId", List.of(user1, user2))
                .whereIn("receiverId", List.of(user1, user2))
                .orderBy("timestamp", Query.Direction.ASCENDING);

        return query.get().get().getDocuments().stream()
                .map(doc -> doc.toObject(Message.class))
                .filter(msg -> msg.getSenderId() != null && msg.getReceiverId() != null)
                .filter(msg -> {
                    boolean fromAToB = msg.getSenderId().equals(user1) && msg.getReceiverId().equals(user2);
                    boolean fromBToA = msg.getSenderId().equals(user2) && msg.getReceiverId().equals(user1);
                    return fromAToB || fromBToA;
                })
                .collect(Collectors.toList());
    }

    public Message getMessage(String messageId) throws ExecutionException, InterruptedException {
        DocumentSnapshot snapshot = firestore.collection(Message.COLLECTION_NAME).document(messageId).get().get();
        if (!snapshot.exists()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found: " + messageId);
        }
        return snapshot.toObject(Message.class);
    }

    public void addReaction(String messageId, Long userId, String emoji)
            throws ExecutionException, InterruptedException {
        DocumentReference docRef = firestore.collection(Message.COLLECTION_NAME).document(messageId);
        DocumentSnapshot snapshot = docRef.get().get();
        if (!snapshot.exists()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found: " + messageId);
        }
        docRef.update("reactions." + userId, emoji).get();
    }

    public void removeReaction(String messageId, Long userId)
            throws ExecutionException, InterruptedException {
        DocumentReference docRef = firestore.collection(Message.COLLECTION_NAME).document(messageId);
        DocumentSnapshot snapshot = docRef.get().get();
        if (!snapshot.exists()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found: " + messageId);
        }
        docRef.update("reactions." + userId, FieldValue.delete()).get();
    }
}