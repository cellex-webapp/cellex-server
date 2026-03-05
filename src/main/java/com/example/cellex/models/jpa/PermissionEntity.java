package com.example.cellex.models.jpa;

import jakarta.persistence.*;
import lombok.*;

/**
 * JPA Entity for the 'permissions' table in PostgreSQL (Supabase).
 * Stores fine-grained permissions per module.
 */
@Entity
@Table(name = "permissions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class PermissionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "permission_key", unique = true, nullable = false, length = 100)
    private String permissionKey;

    @Column(name = "module", nullable = false, length = 50)
    private String module;
}
