package com.example.cellex.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor // Lombok constructor for final fields
public class SecurityConfig {

    // Inject các component đã tạo ở các bước trước
    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // Vô hiệu hóa CSRF vì chúng ta dùng token
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/v1/auth/**", // Cho phép tất cả các endpoint trong AuthController
                                "/v3/api-docs/**",   // Swagger JSON endpoint
                                "/swagger-ui/**"     // Swagger UI
                        ).permitAll() // Cho phép truy cập công khai
                        .anyRequest().authenticated() // Tất cả các request khác phải được xác thực
                )
                // Cấu hình quản lý session
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // Luôn là STATELESS với JWT
                // Chỉ định AuthenticationProvider
                .authenticationProvider(authenticationProvider)
                // Thêm JWT filter vào trước filter UsernamePasswordAuthenticationFilter
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}