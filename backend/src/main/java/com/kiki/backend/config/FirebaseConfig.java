package com.kiki.backend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    @Value("${firebase.credentials-path:firebase-service-account.json}")
    private String credentialsPath;

    @Value("${firebase.enabled:true}")
    private boolean firebaseEnabled;

    private boolean initialized = false;

    @PostConstruct
    public void initFirebase() {
        if (!firebaseEnabled) {
            log.info("Firebase is explicitly disabled via configuration.");
            return;
        }

        if (!FirebaseApp.getApps().isEmpty()) {
            initialized = true;
            return;
        }

        try {
            InputStream serviceAccount = null;
            File file = new File(credentialsPath);
            if (file.exists()) {
                serviceAccount = new FileInputStream(file);
                log.info("Loading Firebase credentials from filesystem: {}", file.getAbsolutePath());
            } else {
                serviceAccount = getClass().getClassLoader().getResourceAsStream(credentialsPath);
                if (serviceAccount != null) {
                    log.info("Loading Firebase credentials from classpath: {}", credentialsPath);
                }
            }

            if (serviceAccount == null) {
                log.warn("⚠️ Firebase service account file NOT found at: '{}'. Server will run in LOCAL_DEV_MODE (mock JWT verification enabled).", credentialsPath);
                return;
            }

            FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build();

            FirebaseApp.initializeApp(options);
            initialized = true;
            log.info("✅ Firebase Admin SDK initialized successfully.");
        } catch (Exception e) {
            log.error("Failed to initialize Firebase Admin SDK: {}. Running in DEV fallback mode.", e.getMessage());
        }
    }

    public boolean isInitialized() {
        return initialized;
    }
}
