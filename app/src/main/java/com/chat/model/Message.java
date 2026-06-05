package com.chat.model;

import java.util.Map;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.ServerTimestamp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message {
    public static final String COLLECTION_NAME = "messages";

    @DocumentId
    private String messageId;
    private Long senderId;
    private Long receiverId;

    @ServerTimestamp
    private Timestamp timestamp;

    private String textContent;
    private byte[] imageBlob;
    private Map<String, String> reactions;

}