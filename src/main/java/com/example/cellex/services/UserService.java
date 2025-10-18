package com.example.cellex.services;

import com.example.cellex.dtos.request.profile.CreateUserDataRequest;
import com.example.cellex.dtos.request.profile.UpdateUserDataRequest;
import com.example.cellex.dtos.request.profile.UpdateUserRequest;
import com.example.cellex.dtos.response.UserResponse;
import com.example.cellex.enums.Role;
import com.example.cellex.exceptions.AppException;
import com.example.cellex.exceptions.ErrorCode;
import com.example.cellex.models.User;
import com.example.cellex.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final S3Service s3Service;
    private final AddressService addressService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User createAccount(CreateUserDataRequest request, MultipartFile avatar) {
        log.info("Creating account for email: {} with role: {}", request.getEmail(), request.getRole());

        // Check if email already exists
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            log.warn("Account creation failed - email already exists: {}", request.getEmail());
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        // Build user with new address system
        User.UserBuilder userBuilder = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber())
                .role(request.getRole())
                .isActive(true);

        // Handle avatar upload if provided
        if (avatar != null && !avatar.isEmpty()) {
            try {
                String avatarUrl = s3Service.uploadFile(avatar, "avatars");
                userBuilder.avatarUrl(avatarUrl);
                log.debug("Avatar uploaded for new user: {}", avatarUrl);
            } catch (IOException e) {
                log.error("Failed to upload avatar for new user: {}", request.getEmail(), e);
                throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
            }
        }

        // Handle address if provided
        if (request.getProvinceCode() != null || request.getCommuneCode() != null ||
            request.getDetailAddress() != null) {

            User.Address address = buildAddress(request.getProvinceCode(),
                    request.getCommuneCode(), request.getDetailAddress());
            userBuilder.address(address);
            log.debug("Address set for new user: {}", address.getFullAddress());
        }

        User savedUser = userRepository.save(userBuilder.build());
        log.info("Account created successfully for email: {} with role: {}",
                savedUser.getEmail(), savedUser.getRole());

        return savedUser;
    }

    private User.Address buildAddress(String provinceCode, String communeCode, String detailAddress) {
        User.Address.AddressBuilder addressBuilder = User.Address.builder();

        // Set province info
        if (provinceCode != null && !provinceCode.trim().isEmpty()) {
            addressBuilder.provinceCode(provinceCode.trim());
            var province = addressService.getProvinceByCode(provinceCode.trim());
            if (province != null) {
                addressBuilder.provinceName(province.getName());
            } else {
                log.warn("Province not found with code: {}", provinceCode);
            }
        }

        // Set commune info
        if (communeCode != null && !communeCode.trim().isEmpty()) {
            addressBuilder.communeCode(communeCode.trim());
            var commune = addressService.getCommuneByCode(communeCode.trim());
            if (commune != null) {
                addressBuilder.communeName(commune.getName());
            } else {
                log.warn("Commune not found with code: {}", communeCode);
            }
        }

        // Set detail address
        if (detailAddress != null && !detailAddress.trim().isEmpty()) {
            addressBuilder.detailAddress(detailAddress.trim());
        }

        // Build the address to get the names
        User.Address tempAddress = addressBuilder.build();

        // Generate full address
        StringBuilder fullAddress = new StringBuilder();
        if (tempAddress.getDetailAddress() != null && !tempAddress.getDetailAddress().isEmpty()) {
            fullAddress.append(tempAddress.getDetailAddress()).append(", ");
        }
        if (tempAddress.getCommuneName() != null) {
            fullAddress.append(tempAddress.getCommuneName()).append(", ");
        }
        if (tempAddress.getProvinceName() != null) {
            fullAddress.append(tempAddress.getProvinceName());
        }

        addressBuilder.fullAddress(fullAddress.toString().replaceAll(", $", ""));
        return addressBuilder.build();
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
                .address(mapToAddressResponse(savedUser.getAddress()))
                .customerSegmentId(savedUser.getCustomerSegmentId())
                .isActive(savedUser.isEnabled())
                .createdAt(savedUser.getCreatedAt())
                .updatedAt(savedUser.getUpdatedAt())
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

    // Upload/Update user avatar
    public UserResponse uploadUserAvatar(String userId, MultipartFile avatar) throws IOException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (avatar == null || avatar.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_INPUT);
        }

        // Delete old avatar if exists
        if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
            s3Service.deleteFile(user.getAvatarUrl());
        }

        // Upload new avatar
        String avatarUrl = s3Service.uploadFile(avatar, "avatars");
        user.setAvatarUrl(avatarUrl);

        User savedUser = userRepository.save(user);
        return mapToUserResponse(savedUser);
    }

    // Update profile with JSON data only
    public UserResponse updateProfile(String userId, UpdateUserDataRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Update basic info
        if (request.getFullName() != null && !request.getFullName().trim().isEmpty()) {
            user.setFullName(request.getFullName().trim());
        }

        if (request.getPhoneNumber() != null && !request.getPhoneNumber().trim().isEmpty()) {
            user.setPhoneNumber(request.getPhoneNumber().trim());
        }

        // Handle address update
        if (request.getProvinceCode() != null || request.getCommuneCode() != null ||
                request.getDetailAddress() != null) {

            User.Address address = buildAddress(request.getProvinceCode(),
                    request.getCommuneCode(), request.getDetailAddress());
            user.setAddress(address);
        }

        User savedUser = userRepository.save(user);
        return mapToUserResponse(savedUser);
    }

    // Ban user account
    public UserResponse banUser(String userId, String banReason, String adminId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Kiểm tra không thể ban chính mình
        if (userId.equals(adminId)) {
            throw new AppException(ErrorCode.CANNOT_BAN_SELF);
        }

        // Kiểm tra không thể ban admin khác
        if (user.getRole() == Role.ADMIN) {
            throw new AppException(ErrorCode.CANNOT_BAN_ADMIN);
        }

        // Kiểm tra tài khoản đã bị ban chưa
        if (user.isBanned()) {
            throw new AppException(ErrorCode.ACCOUNT_ALREADY_BANNED);
        }

        // Ban tài khoản
        user.setBanned(true);
        user.setBanReason(banReason);
        user.setBannedAt(LocalDateTime.now());
        user.setBannedBy(adminId);

        User savedUser = userRepository.save(user);
        log.info("User {} has been banned by admin {}", userId, adminId);

        return mapToUserResponse(savedUser);
    }

    // Unban user account
    public UserResponse unbanUser(String userId, String adminId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Kiểm tra tài khoản có bị ban không
        if (!user.isBanned()) {
            throw new AppException(ErrorCode.ACCOUNT_NOT_BANNED);
        }

        // Mở ban tài khoản
        user.setBanned(false);
        user.setBanReason(null);
        user.setBannedAt(null);
        user.setBannedBy(null);

        User savedUser = userRepository.save(user);
        log.info("User {} has been unbanned by admin {}", userId, adminId);

        return mapToUserResponse(savedUser);
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .address(mapToAddressResponse(user.getAddress()))
                .customerSegmentId(user.getCustomerSegmentId())
                .isActive(user.isEnabled())
                .isBanned(user.isBanned())
                .banReason(user.getBanReason())
                .bannedAt(user.getBannedAt())
                .bannedBy(user.getBannedBy())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    private UserResponse.AddressResponse mapToAddressResponse(User.Address address) {
        if (address == null) {
            return null;
        }

        return UserResponse.AddressResponse.builder()
                .street(address.getDetailAddress())
                .ward(address.getCommuneName())
                .province(address.getProvinceName())
                .country("Việt Nam") // Mặc định là Vietnam
                .fullAddress(address.getFullAddress())
                .isDefault(true)
                .build();
    }

}
