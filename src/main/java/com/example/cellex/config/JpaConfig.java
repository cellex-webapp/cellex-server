package com.example.cellex.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * JPA Configuration for PostgreSQL (Supabase).
 * Scans User and Auth repository packages (migrated from MongoDB)
 * and the new 'jpa' package for additional JPA repositories.
 */
@Configuration
@EnableJpaAuditing
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackages = {
                "com.example.cellex.repositories.jpa",
                "com.example.cellex.repositories.user",
                "com.example.cellex.repositories.auth",
                "com.example.cellex.repositories.shop"
        }
)
public class JpaConfig {
}
