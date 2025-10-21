package com.example.cellex.models;

import com.example.cellex.enums.ShopStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Document(collection = "shops")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Shop {

    @Id
    private String id;

    @Field("vendor_id")
    private String vendorId;

    @Field("shop_name")
    private String shopName;

    @Field("description")
    private String description;

    @Field("logo_url")
    private String logoUrl;

    @Field("address")
    private String address;

    @Field("phone_number")
    private String phoneNumber;

    @Field("email")
    private String email;

    @Field("status")
    @Builder.Default
    private ShopStatus status = ShopStatus.PENDING;

    @Field("rating")
    @Builder.Default
    private Double rating = 0.0;

    @Field("rejection_reason")
    private String rejectionReason;

    @CreatedDate
    @Field("created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Field("updated_at")
    private LocalDateTime updatedAt;
}
