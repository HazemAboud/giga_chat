package com.auth.config;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

@Configuration
public class FirebaseConfig {

    @Value("${firebase.credentials.path:service-account.json}")
    private String credentialsPath;

    @Value("${firebase.project.id}")
    private String projectId;

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        // Fail-fast validation to catch empty configuration properties early
        if (!StringUtils.hasText(projectId)) {
            throw new IllegalStateException("Firebase configuration error: 'firebase.project.id' is missing or empty in properties.");
        }

        if (FirebaseApp.getApps().isEmpty()) {
            try (InputStream serviceAccount = new ClassPathResource(credentialsPath).getInputStream()) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .setProjectId(projectId.trim())
                        .build();

                return FirebaseApp.initializeApp(options);
            }
        }
        return FirebaseApp.getInstance();
    }

    @Bean
    public Firestore firestore() {
        // Initializing via FirestoreOptions using the project ID guarantees 
        // it links correctly with your initialized Firebase configuration
        return FirestoreOptions.newBuilder()
                .setProjectId(projectId)
                .build()
                .getService();
    }
}