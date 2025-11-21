package com.example.cellex.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Configuration
@Slf4j
public class FirebaseConfig {

    @Value("${firebase.config.path:firebase-service-account.json}")
    private String firebaseConfigPath;

    @Bean
    public FirebaseApp initializeFirebase() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                // Get input stream resource
                InputStream serviceAccountStream = getServiceAccountStream();
                
                // First, extract project ID from JSON
                String projectId = extractProjectId(getServiceAccountStream());
                
                // Then create credentials
                GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccountStream);
                
                FirebaseOptions.Builder optionsBuilder = FirebaseOptions.builder()
                        .setCredentials(credentials);
                
                // Explicitly set project ID if found
                if (projectId != null && !projectId.isEmpty()) {
                    optionsBuilder.setProjectId(projectId);
                    log.info("📝 Setting Project ID: {}", projectId);
                }
                
                FirebaseOptions options = optionsBuilder.build();
                FirebaseApp app = FirebaseApp.initializeApp(options);
                
                // Log detailed info
                log.info("✅ Firebase initialized successfully");
                log.info("📱 Firebase App Name: {}", app.getName());
                log.info("🔑 Project ID: {}", app.getOptions().getProjectId());
                log.info("📧 Service Account: {}", app.getOptions().getServiceAccountId());
                
                return app;
            } else {
                log.info("Firebase already initialized");
                FirebaseApp app = FirebaseApp.getInstance();
                log.info("Existing Firebase Project ID: {}", app.getOptions().getProjectId());
                return app;
            }
        } catch (IOException e) {
            log.error("❌ Failed to initialize Firebase: {}", e.getMessage(), e);
            log.warn("⚠️ Push notifications will not work without Firebase configuration");
            return null;
        }
    }
    
    /**
     * Get service account input stream
     */
    private InputStream getServiceAccountStream() throws IOException {
        try {
            // Try to load from classpath (resources folder)
            InputStream stream = new ClassPathResource(firebaseConfigPath).getInputStream();
            log.info("Loading Firebase config from classpath: {}", firebaseConfigPath);
            return stream;
        } catch (Exception e) {
            // If not in classpath, try absolute path
            InputStream stream = new FileInputStream(firebaseConfigPath);
            log.info("Loading Firebase config from file system: {}", firebaseConfigPath);
            return stream;
        }
    }
    
    /**
     * Extract project_id from Firebase service account JSON
     */
    private String extractProjectId(InputStream serviceAccount) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(serviceAccount);
            JsonNode projectIdNode = root.get("project_id");
            
            if (projectIdNode != null) {
                String projectId = projectIdNode.asText();
                log.info("🔍 Extracted project_id from JSON: {}", projectId);
                return projectId;
            } else {
                log.warn("⚠️ project_id not found in service account JSON");
                return null;
            }
        } catch (Exception e) {
            log.error("❌ Failed to extract project_id: {}", e.getMessage());
            return null;
        }
    }
}
