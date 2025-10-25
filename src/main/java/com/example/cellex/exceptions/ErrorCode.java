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
    EMAIL_SEND_FAILED(1013, "Failed to send email. Please try again later", HttpStatus.INTERNAL_SERVER_ERROR),

    // Domain Specific Errors
    PRODUCT_NOT_FOUND(2001, "Product not found", HttpStatus.NOT_FOUND),
    CATEGORY_NOT_FOUND(2002, "Category not found", HttpStatus.NOT_FOUND),
    CATEGORY_NOT_EXISTED(2003, "Category not existed", HttpStatus.NOT_FOUND),
    SHOP_NOT_FOUND(2004, "Shop not found", HttpStatus.NOT_FOUND),
    SHOP_ALREADY_EXISTS(2005, "Shop already exists for this vendor", HttpStatus.BAD_REQUEST),
    SHOP_NOT_VERIFIED(2006, "Shop is not verified", HttpStatus.BAD_REQUEST),
    SHOP_NOT_FOUND_OR_NOT_VERIFIED(2007, "Shop not found or not verified", HttpStatus.BAD_REQUEST),

    // Category Attribute Errors
    CATEGORY_ATTRIBUTE_NOT_FOUND(2008, "Category attribute not found", HttpStatus.NOT_FOUND),
    ATTRIBUTE_KEY_EXISTED(2009, "Attribute key already exists in this category", HttpStatus.BAD_REQUEST),
    SELECT_OPTIONS_REQUIRED(2010, "Select options are required for SELECT or MULTI_SELECT type", HttpStatus.BAD_REQUEST),
    REQUIRED_ATTRIBUTE_MISSING(2011, "Required attribute is missing", HttpStatus.BAD_REQUEST),
    INVALID_ATTRIBUTE_VALUE(2012, "Invalid attribute value", HttpStatus.BAD_REQUEST),

    // File Upload Errors
    INVALID_INPUT(3001, "Invalid input data", HttpStatus.BAD_REQUEST),
    FILE_UPLOAD_FAILED(3002, "File upload failed", HttpStatus.INTERNAL_SERVER_ERROR),
    FILE_DELETE_FAILED(3003, "File delete failed", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_FILE_FORMAT(3004, "Invalid file format", HttpStatus.BAD_REQUEST),
    FILE_SIZE_TOO_LARGE(3005, "File size too large", HttpStatus.BAD_REQUEST),
    FILE_NOT_FOUND(3006, "File not found", HttpStatus.NOT_FOUND),

    // Access Control Errors
    ACCESS_DENIED(4001, "Access denied", HttpStatus.FORBIDDEN),
    RESOURCE_NOT_OWNED(4002, "You don't own this resource", HttpStatus.FORBIDDEN),
    OPERATION_NOT_ALLOWED(4003, "Operation not allowed", HttpStatus.FORBIDDEN),

    // Validation Errors
    FIELD_REQUIRED(5001, "Required field is missing", HttpStatus.BAD_REQUEST),
    FIELD_TOO_LONG(5002, "Field value is too long", HttpStatus.BAD_REQUEST),
    FIELD_TOO_SHORT(5003, "Field value is too short", HttpStatus.BAD_REQUEST),
    INVALID_EMAIL_FORMAT(5004, "Invalid email format", HttpStatus.BAD_REQUEST),
    INVALID_PHONE_FORMAT(5005, "Invalid phone number format", HttpStatus.BAD_REQUEST),
    DUPLICATE_VALUE(5006, "Duplicate value not allowed", HttpStatus.BAD_REQUEST),

    // Account Ban Errors
    ACCOUNT_BANNED(6001, "Account is banned", HttpStatus.FORBIDDEN),
    ACCOUNT_ALREADY_BANNED(6002, "Account is already banned", HttpStatus.BAD_REQUEST),
    ACCOUNT_NOT_BANNED(6003, "Account is not banned", HttpStatus.BAD_REQUEST),
    CANNOT_BAN_ADMIN(6004, "Cannot ban admin account", HttpStatus.FORBIDDEN),
    CANNOT_BAN_SELF(6005, "Cannot ban your own account", HttpStatus.FORBIDDEN),
    
    // Customer Segment Errors
    SEGMENT_NOT_FOUND(7001, "Customer segment not found", HttpStatus.NOT_FOUND),
    SEGMENT_ALREADY_EXISTS(7002, "Customer segment already exists", HttpStatus.BAD_REQUEST),
    
    // Coupon Errors
    COUPON_NOT_FOUND(8001, "Coupon not found", HttpStatus.NOT_FOUND),
    COUPON_EXPIRED(8002, "Coupon has expired", HttpStatus.BAD_REQUEST),
    COUPON_ALREADY_USED(8003, "Coupon has already been used", HttpStatus.BAD_REQUEST),
    COUPON_NOT_APPLICABLE(8004, "Coupon is not applicable for this order", HttpStatus.BAD_REQUEST),
    
    // General Errors
    INVALID_REQUEST(9001, "Invalid request", HttpStatus.BAD_REQUEST),
    INTERNAL_SERVER_ERROR(9002, "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR),
    USER_NOT_EXISTED(9003, "User not existed", HttpStatus.NOT_FOUND);
    
    private final int code;
    private final String message;
    private final HttpStatus httpStatus;
}
