package com.example.cellex;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

@SpringBootApplication
@EnableMongoAuditing
public class CellexApplication {

	public static void main(String[] args) {
		// Load .env file if exists (for local development)
		// In production (Docker/Render), environment variables are injected directly
		try {
			Dotenv dotenv = Dotenv.configure()
					.ignoreIfMissing() // Don't fail if .env doesn't exist
					.load();
			dotenv.entries().forEach(entry -> {
				// Only set if not already set by system
				if (System.getenv(entry.getKey()) == null) {
					System.setProperty(entry.getKey(), entry.getValue());
				}
			});
			System.out.println("✅ Loaded environment from .env file");
		} catch (Exception e) {
			System.out.println("ℹ️  No .env file found, using system environment variables");
		}

		SpringApplication.run(CellexApplication.class, args);
	}
}
