package com.example.cellex;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import io.github.cdimascio.dotenv.Dotenv;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class CellexApplication {

	private static final List<String> CRITICAL_ENV_KEYS = List.of(
			"MONGO_URI",
			"SUPABASE_DB_URL",
			"SUPABASE_DB_USERNAME",
			"SUPABASE_DB_PASSWORD"
	);

	public static void main(String[] args) {
		// Load .env file if exists (for local development)
		// In production (Docker/Render), environment variables are injected directly
		try {
			Dotenv dotenv = Dotenv.configure()
					.ignoreIfMissing() // Don't fail if .env doesn't exist
					.load();

			// Prefer .env values for local runs when process env does not provide them.
			dotenv.entries().forEach(entry -> {
				String key = entry.getKey();
				if (System.getenv(key) == null) {
					System.setProperty(key, entry.getValue());
				}
			});

			ensureCriticalEnvLoadedFromFile();
			System.out.println("✅ Loaded environment from .env file");
		} catch (Exception e) {
			System.out.println("ℹ️  No .env file found, using system environment variables");
		}

		SpringApplication.run(CellexApplication.class, args);
	}

	private static void ensureCriticalEnvLoadedFromFile() {
		boolean hasMissingCriticalKey = CRITICAL_ENV_KEYS.stream()
				.anyMatch(key -> System.getProperty(key) == null && System.getenv(key) == null);

		if (!hasMissingCriticalKey) {
			return;
		}

		Path envPath = Path.of(".env");
		if (!Files.exists(envPath)) {
			return;
		}

		try {
			for (String rawLine : Files.readAllLines(envPath)) {
				String line = rawLine.trim();
				if (line.isEmpty() || line.startsWith("#")) {
					continue;
				}

				int separator = line.indexOf('=');
				if (separator <= 0) {
					continue;
				}

				String key = line.substring(0, separator).trim();
				String value = line.substring(separator + 1).trim();
				if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
					value = value.substring(1, value.length() - 1);
				}

				if (System.getenv(key) == null) {
					System.setProperty(key, value);
				}
			}
		} catch (IOException ignored) {
			// Keep startup resilient; app will continue with existing env sources.
		}
	}
}
