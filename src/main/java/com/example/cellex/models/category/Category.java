package com.example.cellex.models.category;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "categories")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    @JsonIgnore
    private UUID uuid;

    private String name;

    @Column(unique = true)
    private String slug;

    @Column(name = "parent_id")
    @JsonIgnore
    private UUID parentUuid;

    @Column(name = "image_url")
    private String imageUrl;

    private String description;

    @Builder.Default
    @Column(name = "is_active")
    private Boolean isActive = true;

    @Builder.Default
    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // --- Backward-compat String-based ID accessors ---

    @JsonProperty("id")
    public String getId() { return uuid != null ? uuid.toString() : null; }

    public void setId(String id) { this.uuid = id != null ? UUID.fromString(id) : null; }

    @JsonProperty("parentId")
    public String getParentId() { return parentUuid != null ? parentUuid.toString() : null; }

    public void setParentId(String parentId) {
        this.parentUuid = (parentId != null && !parentId.isEmpty()) ? UUID.fromString(parentId) : null;
    }
}