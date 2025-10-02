package com.example.cellex.services;

import com.example.cellex.dtos.request.auth.LoginRequest;
import com.example.cellex.dtos.request.auth.RefreshTokenRequest;
import com.example.cellex.dtos.request.auth.SendOtpRequest;
import com.example.cellex.dtos.request.auth.VerifyOtpRequest;
import com.example.cellex.dtos.response.AuthResponse;
import com.example.cellex.dtos.response.UserResponse;
import com.example.cellex.enums.Role;
import com.example.cellex.exceptions.AppException;
import com.example.cellex.exceptions.ErrorCode;
import com.example.cellex.models.Otp;
import com.example.cellex.models.User;
import com.example.cellex.repositories.OtpRepository;
import com.example.cellex.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
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

    // ... login, refreshToken, and sendSignupCode methods remain the same ...
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );
        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

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
        String userEmail = jwtService.extractUsername(request.getRefreshToken());
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (jwtService.isTokenValid(request.getRefreshToken(), user)) {
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
        throw new AppException(ErrorCode.UNAUTHENTICATED);
    }

    public void sendSignupCode(SendOtpRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new AppException(ErrorCode.PASSWORDS_DO_NOT_MATCH);
        }
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
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
}