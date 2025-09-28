package com.example.cellex.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class EnvLoader {

    private final ConfigurableEnvironment environment;

    public EnvLoader(ConfigurableEnvironment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void loadEnvFile() {
        Map<String, Object> envProperties = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(".env"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                String[] parts = line.split("=", 2);
                if (parts.length == 2) {
                    String key = parts[0].trim();
                    String value = parts[1].trim();

                    // Convert environment variable format to Spring property format
                    String springKey = convertEnvKeyToSpringKey(key);
                    envProperties.put(springKey, value);

                    System.out.println("✅ Loaded: " + springKey + " = " +
                            (key.contains("SECRET") ? "***hidden***" : value));
                }
            }

            // Add properties to Spring environment
            environment.getPropertySources().addFirst(new MapPropertySource("envFile", envProperties));
            System.out.println("🎉 Successfully loaded " + envProperties.size() + " properties from .env");

        } catch (IOException e) {
            System.err.println("❌ Could not load .env file: " + e.getMessage());
            System.err.println("📁 Make sure .env file exists in project root directory");
        }
    }

    private String convertEnvKeyToSpringKey(String envKey) {
        switch (envKey) {
            case "APPLICATION_SECURITY_JWT_SECRET_KEY":
                return "application.security.jwt.secret-key";
            case "APPLICATION_SECURITY_JWT_ACCESS_TOKEN_EXPIRATION":
                return "application.security.jwt.access-token-expiration";
            case "APPLICATION_SECURITY_JWT_REFRESH_TOKEN_EXPIRATION":
                return "application.security.jwt.refresh-token-expiration";
            case "SERVER_PORT":
                return "server.port";
            case "MONGO_URI":
                return "spring.data.mongodb.uri";
            case "AWS_ACCESS_KEY_ID":
                return "aws.access-key-id";
            case "AWS_SECRET_ACCESS_KEY":
                return "aws.secret-access-key";
            case "S3_BUCKET_NAME":
                return "aws.s3.bucketName";
            case "S3_REGION":
                return "aws.region";
            default:
                return envKey.toLowerCase().replace("_", ".");
        }
    }
}