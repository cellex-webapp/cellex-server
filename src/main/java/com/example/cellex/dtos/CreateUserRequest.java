package com.example.cellex.dtos;

import lombok.Data;
import java.util.List;

// Data Transfer Object for creating a new user account.
@Data
public class CreateUserRequest {
    private String fullName;
    private String email;
    private String password;
    private String phoneNumber;
    private String addresses;
}