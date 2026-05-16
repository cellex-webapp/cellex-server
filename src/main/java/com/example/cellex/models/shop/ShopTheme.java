package com.example.cellex.models.shop;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * JPA Entity for Server-Driven UI (SDUI) - ShopTheme configuration.
 * Represents the UI configuration for a Shop (1-to-1 relationship).
 * Stores color scheme, typography, and layout configuration as JSONB in PostgreSQL.
 */
@Entity
@Table(
    name = "shop_themes",
    indexes = @Index(name = "idx_shop_themes_shop_id", columnList = "shop_id", unique = true)
)
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ShopTheme {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "shop_id", nullable = false, unique = true)
    private UUID shopId;

    @Column(name = "primary_color", nullable = false, length = 7)
    @Builder.Default
    private String primaryColor = "#1677FF";

    @Column(name = "secondary_color", nullable = false, length = 7)
    @Builder.Default
    private String secondaryColor = "#FFFFFF";

    @Column(name = "font_family", nullable = false)
    @Builder.Default
    private String fontFamily = "Inter";

    /**
     * Layout configuration stored as JSONB in PostgreSQL.
     * Hibernate 6 natively maps Map<String, Object> ↔ JSONB type automatically.
     * 
     * Example structure:
     * {
     *   "header": { "backgroundColor": "#F5F5F5", "height": 80 },
     *   "footer": { "show": true, "backgroundColor": "#333333" },
     *   "sections": [
     *     { "id": "hero", "type": "banner", "height": 400 },
     *     { "id": "products", "type": "grid", "columns": 4 }
     *   ]
     * }
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "layout_config", columnDefinition = "jsonb")
    private Map<String, Object> layoutConfig;

    @Column(name = "is_published", nullable = false)
    @Builder.Default
    private Boolean isPublished = true;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
