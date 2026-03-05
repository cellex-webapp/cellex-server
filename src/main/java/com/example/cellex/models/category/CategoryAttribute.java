package com.example.cellex.models.category;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "category_attributes")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CategoryAttribute {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    @JsonIgnore
    private UUID uuid;

    @Column(name = "category_id", nullable = false)
    @JsonIgnore
    private UUID categoryUuid;

    @Column(name = "attribute_name")
    private String attributeName;

    @Column(name = "attribute_key")
    private String attributeKey;

    @Column(name = "data_type")
    private String dataType;

    private String unit;

    @Column(name = "is_required")
    private Boolean isRequired;

    @Column(name = "is_highlight")
    private Boolean isHighlight;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "select_options", columnDefinition = "jsonb")
    private List<String> selectOptions;

    @Column(name = "validation_pattern")
    private String validationPattern;

    @Column(name = "sort_order")
    private Integer sortOrder;

    private String description;

    @Builder.Default
    @Column(name = "is_active")
    private Boolean isActive = true;

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

    @JsonProperty("categoryId")
    public String getCategoryId() { return categoryUuid != null ? categoryUuid.toString() : null; }

    public void setCategoryId(String categoryId) {
        this.categoryUuid = categoryId != null ? UUID.fromString(categoryId) : null;
    }
}
