package com.example.cellex.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "otps")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Otp {
    @Id
    private String id;
    private String code;
    private String email;
    private boolean isUsed;
    private LocalDateTime createdAt;
    private LocalDateTime expiredAt;

    // Lưu tạm thông tin người dùng để tạo tài khoản sau khi xác thực
    private String fullName;
    private String hashedPassword;
    private String phoneNumber;
}