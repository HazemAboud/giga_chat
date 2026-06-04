package com.chat.service;

import java.util.concurrent.ExecutionException;

import org.springframework.stereotype.Service;

import com.chat.model.Message;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;

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
}