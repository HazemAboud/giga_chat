package com.chat.service;

import java.util.concurrent.ExecutionException;

import org.springframework.stereotype.Service;

import com.chat.model.Message;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteResult;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final Firestore firestore;

    public String saveMessage(Message msg) throws ExecutionException, InterruptedException {
        // This is where you set the collection name
        DocumentReference docRef = firestore.collection(Message.COLLECTION_NAME).document();
        
        // If the messageId is already set, Firestore will use it;
        // otherwise, it will generate a new one.
        ApiFuture<WriteResult> result = docRef.set(msg);
        return result.get().getUpdateTime().toString();
    }
}