package com.example.cellex.dtos.request;

import lombok.Data;

// Data Transfer Object for creating a new user account.
@Data
public class CreateUserRequest {
    private String fullName;
    private String email;
    private String password;
    private String phoneNumber;
    private String addresses;
}