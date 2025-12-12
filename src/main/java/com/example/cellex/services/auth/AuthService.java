package com.example.cellex.services.auth;

import com.example.cellex.dtos.request.auth.ChangePasswordRequest;
import com.example.cellex.dtos.request.auth.LoginRequest;
import com.example.cellex.dtos.request.auth.RefreshTokenRequest;
import com.example.cellex.dtos.request.auth.SendOtpRequest;
import com.example.cellex.dtos.request.auth.VerifyOtpRequest;
import com.example.cellex.dtos.response.auth.AuthResponse;
import com.example.cellex.dtos.response.user.UserResponse;
import com.example.cellex.enums.Role;
import com.example.cellex.exceptions.AccountBannedException;
import com.example.cellex.exceptions.AppException;
import com.example.cellex.exceptions.ErrorCode;
import com.example.cellex.models.auth.Otp;
import com.example.cellex.models.user.User;
import com.example.cellex.repositories.auth.OtpRepository;
import com.example.cellex.repositories.user.UserRepository;
import com.example.cellex.services.email.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final OtpRepository otpRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public AuthResponse login(LoginRequest request) {
        // Validate email format and length
        if (request.getEmail() == null || request.getEmail().trim().isEmpty() ||
                request.getEmail().length() < 3 || !isValidEmail(request.getEmail())) {
            throw new AppException(ErrorCode.USERNAME_INVALID);
        }

        // Validate password
        if (request.getPassword() == null ) {
            throw new AppException(ErrorCode.PASSWORD_INVALID);
        }

        // Check if user exists
        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Check if user account is locked
        if (user.isBanned()) {
            // Tạo custom exception với thông tin về lý do khóa
            throw new AccountBannedException(user.getBanReason());
        }

        // Check if user is active
        if (!user.isEnabled()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
        } catch (BadCredentialsException e) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        var accessToken = jwtService.generateToken(user);
        var refreshToken = jwtService.generateRefreshToken(user);

        UserResponse userResponse = UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .isActive(user.isEnabled())
                .createdAt(user.getCreatedAt())
                .build();

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(userResponse)
                .build();
    }

    public AuthResponse refreshToken(RefreshTokenRequest request) {
        if (request.getRefreshToken() == null || request.getRefreshToken().trim().isEmpty()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        String userEmail;
        try {
            userEmail = jwtService.extractUsername(request.getRefreshToken());
        } catch (Exception e) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (!user.isEnabled()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        if (!jwtService.isTokenValid(request.getRefreshToken(), user)) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        var accessToken = jwtService.generateToken(user);

        UserResponse userResponse = UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .isActive(user.isEnabled())
                .createdAt(user.getCreatedAt())
                .build();

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(request.getRefreshToken())
                .user(userResponse)
                .build();
    }

    public void sendSignupCode(SendOtpRequest request) {
        // Validate email format and length
        if (request.getEmail() == null || request.getEmail().trim().isEmpty() ||
                request.getEmail().length() < 3 || !isValidEmail(request.getEmail())) {
            throw new AppException(ErrorCode.USERNAME_INVALID);
        }

        // Validate password
        if (request.getPassword() == null || request.getPassword().length() < 8) {
            throw new AppException(ErrorCode.PASSWORD_INVALID);
        }

        // Validate password confirmation
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new AppException(ErrorCode.PASSWORDS_DO_NOT_MATCH);
        }

        // Check if user already exists
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }

        String otpCode = String.format("%06d", new Random().nextInt(999999));

        Otp otp = Otp.builder()
                .email(request.getEmail())
                .code(otpCode)
                .isUsed(false)
                .createdAt(LocalDateTime.now())
                .expiredAt(LocalDateTime.now().plusMinutes(5))
                .fullName(request.getFullName())
                .hashedPassword(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber())
                .build();
        otpRepository.save(otp);

        emailService.sendOtpEmail(request.getEmail(), otpCode);
    }

    public UserResponse verifySignupCode(VerifyOtpRequest request) {
        // Validate email
        if (request.getEmail() == null || request.getEmail().trim().isEmpty() ||
                !isValidEmail(request.getEmail())) {
            throw new AppException(ErrorCode.USERNAME_INVALID);
        }

        // Check if user already exists
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }

        Otp otp = otpRepository.findByCodeAndEmail(request.getOtp(), request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_OTP));

        if (otp.isUsed()) {
            throw new AppException(ErrorCode.OTP_ALREADY_USED);
        }
        if (LocalDateTime.now().isAfter(otp.getExpiredAt())) {
            throw new AppException(ErrorCode.OTP_EXPIRED);
        }

        otp.setUsed(true);
        otpRepository.save(otp);

        User newUser = User.builder()
                .fullName(otp.getFullName())
                .email(otp.getEmail())
                .password(otp.getHashedPassword())
                .phoneNumber(otp.getPhoneNumber())
                .role(Role.USER)
                .isActive(true)
                .build();

        User savedUser = userRepository.save(newUser);

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

    public void changePassword(String email, ChangePasswordRequest request) {
        // Find user by email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Check if user account is locked
        if (user.isBanned()) {
            throw new AccountBannedException(user.getBanReason());
        }

        // Verify old password
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        // Validate new password
        if (request.getNewPassword() == null || request.getNewPassword().length() < 8) {
            throw new AppException(ErrorCode.PASSWORD_INVALID);
        }

        // Check if new password is same as old password
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new AppException(ErrorCode.PASSWORD_INVALID);
        }

        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    private boolean isValidEmail(String email) {
        return email.contains("@") && email.contains(".");
    }
}
