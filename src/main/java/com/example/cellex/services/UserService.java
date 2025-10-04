package com.example.cellex.services;

import com.example.cellex.dtos.request.profile.CreateUserRequest;
import com.example.cellex.dtos.request.profile.UpdateUserRequest;
import com.example.cellex.dtos.response.UserResponse;
import com.example.cellex.enums.Role;
import com.example.cellex.exceptions.AppException;
import com.example.cellex.exceptions.ErrorCode;
import com.example.cellex.models.User;
import com.example.cellex.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final S3Service s3Service;
    private final AddressService addressService;
    private final PasswordEncoder passwordEncoder;

    // Add the missing createAccount method
    public User createAccount(CreateUserRequest request) {
        // Check if email already exists
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        User newUser = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber())
                .role(Role.ADMIN)
                .isActive(true)
                .build();

        return userRepository.save(newUser);
    }

    public UserResponse updateProfile(String userId, UpdateUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Update basic info (removed email)
        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }

        // Handle avatar upload
        if (request.getAvatar() != null && !request.getAvatar().isEmpty()) {
            try {
                String avatarUrl = s3Service.uploadFile(request.getAvatar(), "avatars");
                user.setAvatarUrl(avatarUrl);
            } catch (IOException e) {
                throw new RuntimeException("Failed to upload avatar", e);
            }
        }

        // Handle address update
        if (request.getProvinceCode() != null || request.getCommuneCode() != null ||
                request.getDetailAddress() != null) {

            User.Address currentAddress = user.getAddress();
            if (currentAddress == null) {
                currentAddress = User.Address.builder().build();
            }

            if (request.getProvinceCode() != null) {
                currentAddress.setProvinceCode(request.getProvinceCode());
                var province = addressService.getProvinceByCode(request.getProvinceCode());
                if (province != null) {
                    currentAddress.setProvinceName(province.getName());
                }
            }

            if (request.getCommuneCode() != null) {
                currentAddress.setCommuneCode(request.getCommuneCode());
                var commune = addressService.getCommuneByCode(request.getCommuneCode());
                if (commune != null) {
                    currentAddress.setCommuneName(commune.getName());
                }
            }

            if (request.getDetailAddress() != null) {
                currentAddress.setDetailAddress(request.getDetailAddress());
            }

            // Generate full address
            StringBuilder fullAddress = new StringBuilder();
            if (request.getDetailAddress() != null && !request.getDetailAddress().trim().isEmpty()) {
                fullAddress.append(request.getDetailAddress().trim()).append(", ");
            }
            if (currentAddress.getCommuneName() != null) {
                fullAddress.append(currentAddress.getCommuneName()).append(", ");
            }
            if (currentAddress.getProvinceName() != null) {
                fullAddress.append(currentAddress.getProvinceName());
            }
            currentAddress.setFullAddress(fullAddress.toString());

            user.setAddress(currentAddress);
        }

        User savedUser = userRepository.save(user);

        return UserResponse.builder()
                .id(savedUser.getId())
                .fullName(savedUser.getFullName())
                .email(savedUser.getEmail())
                .phoneNumber(savedUser.getPhoneNumber())
                .avatarUrl(savedUser.getAvatarUrl())
                .role(savedUser.getRole())
                .isActive(savedUser.isEnabled())
                .createdAt(savedUser.getCreatedAt())
                .build();
    }
    public List<UserResponse> getAllUsers() {
        List<User> users = userRepository.findAll();
        return users.stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }

    public UserResponse getUserById(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        return mapToUserResponse(user);
    }

    public UserResponse getCurrentUser(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        return mapToUserResponse(user);
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .isActive(user.isEnabled())
                .createdAt(user.getCreatedAt())
                .build();
    }

}
