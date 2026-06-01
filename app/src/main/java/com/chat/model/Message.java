package com.chat.model;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.annotation.DocumentId;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Message {
    public static final String COLLECTION_NAME = "messages";

    @DocumentId
    private String messageId;
    private Long senderId;
    private Long receiverId;
    private Timestamp timestamp;

}

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
class TextMessage extends Message {
    private String textContent;
}

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
class ImageMessage extends Message {
    private byte[] imageBlob;
}

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
class MixedMessage extends Message {
    private byte[] imageBlob;
    private String textContent;
}