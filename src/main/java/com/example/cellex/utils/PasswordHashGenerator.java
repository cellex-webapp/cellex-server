package com.example.cellex.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Utility để generate BCrypt password hash cho seed data
 * Chạy main method để tạo hash cho password "admin123"
 */
public class PasswordHashGenerator {
    
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String password = "admin123";
        
        System.out.println("========================================");
        System.out.println("🔐 PASSWORD HASH GENERATOR");
        System.out.println("========================================");
        System.out.println();
        System.out.println("Password: " + password);
        System.out.println();
        
        // Generate 3 hashes để verify chúng khác nhau nhưng đều valid
        System.out.println("Generated BCrypt Hashes:");
        System.out.println("------------------------");
        for (int i = 1; i <= 3; i++) {
            String hash = encoder.encode(password);
            boolean isValid = encoder.matches(password, hash);
            System.out.println(i + ". " + hash);
            System.out.println("   Valid: " + isValid);
            System.out.println();
        }
        
        // Test với hash hiện tại trong seed data
        String currentHash = "$2a$10$xNH0Y2yPSJOBPXzF4Jls/OPqQvEt8FsHZ1R3LxGYx4.xkY5FQ0kv2";
        System.out.println("========================================");
        System.out.println("Testing current hash from seed data:");
        System.out.println(currentHash);
        System.out.println("Valid: " + encoder.matches(password, currentHash));
        System.out.println("========================================");
    }
}
