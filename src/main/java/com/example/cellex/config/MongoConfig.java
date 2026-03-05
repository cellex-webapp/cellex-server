package com.example.cellex.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * MongoDB Configuration.
 * Scans ONLY MongoDB repository packages. User and Auth repos have been
 * migrated to PostgreSQL and are now scanned by JpaConfig.
 */
@Configuration
@EnableMongoAuditing
@EnableMongoRepositories(
        basePackages = {
                "com.example.cellex.repositories.cart",
                "com.example.cellex.repositories.chat",
                "com.example.cellex.repositories.common",
                "com.example.cellex.repositories.product",
                "com.example.cellex.repositories.recommendation"
        }
)
public class MongoConfig {
}
