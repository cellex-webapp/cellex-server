package com.example.cellex.models.jpa;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "livestream_products")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LivestreamProductEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private LivestreamSessionEntity session;

    @Column(name = "product_id", nullable = false)
    private String productId; // ID chiếu sang MongoDB Collection "products"

    @Column(name = "flash_sale_price")
    private Double flashSalePrice;

    @Column(name = "is_pinned", nullable = false)
    @Builder.Default
    private Boolean isPinned = false;
}