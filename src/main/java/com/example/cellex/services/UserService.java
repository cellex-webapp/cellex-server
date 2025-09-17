package com.example.cellex.services;

import com.example.cellex.dtos.CreateUserRequest;
import com.example.cellex.enums.Role;
import com.example.cellex.models.User;
import com.example.cellex.repositories.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
// This class contains the business logic for user-related operations.
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User createAccount(CreateUserRequest request) {
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            throw new IllegalStateException("Email already in use.");
        });

        User newUser = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber())
                .addresses(request.getAddresses()) // This line now works with a String
                .role(Role.ADMIN)
                .isActive(true)
                .build();

        return userRepository.save(newUser);
    }
}