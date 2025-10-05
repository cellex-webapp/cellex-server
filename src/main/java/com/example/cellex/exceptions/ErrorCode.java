package com.example.cellex.exceptions;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    // General Errors
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY(1001, "Invalid message key", HttpStatus.BAD_REQUEST),

    // Authentication & Authorization Errors
    USER_EXISTED(1002, "User existed", HttpStatus.BAD_REQUEST),
    USERNAME_INVALID(1003, "Username must be at least 3 characters", HttpStatus.BAD_REQUEST),
    PASSWORD_INVALID(1004, "Password must be at least 8 characters", HttpStatus.BAD_REQUEST),
    USER_NOT_FOUND(1005, "Email or password not found", HttpStatus.NOT_FOUND),
    UNAUTHENTICATED(1006, "Unauthenticated", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1007, "You do not have permission", HttpStatus.FORBIDDEN),

    // Sign Up Errors
    PASSWORDS_DO_NOT_MATCH(1008, "Passwords do not match", HttpStatus.BAD_REQUEST),
    EMAIL_ALREADY_EXISTS(1009, "This email is already registered", HttpStatus.BAD_REQUEST),
    INVALID_OTP(1010, "Invalid OTP code", HttpStatus.BAD_REQUEST),
    OTP_EXPIRED(1011, "OTP has expired", HttpStatus.BAD_REQUEST),
    OTP_ALREADY_USED(1012, "OTP has already been used", HttpStatus.BAD_REQUEST),

    // Domain Specific Errors
    PRODUCT_NOT_FOUND(2001, "Product not found", HttpStatus.NOT_FOUND),
    CATEGORY_NOT_FOUND(2002, "Category not found", HttpStatus.NOT_FOUND),
    SHOP_NOT_FOUND(2003, "Shop not found", HttpStatus.NOT_FOUND);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;
}