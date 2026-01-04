package com.example.cellex.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.cellex.repositories.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.concurrent.Executor;

import java.util.Map;

@Configuration
@RequiredArgsConstructor
@EnableAsync
public class ApplicationConfig {

    private final UserRepository userRepository;

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> (org.springframework.security.core.userdetails.UserDetails) userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + username));
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService()); // Cung cấp UserDetailsService
        authProvider.setPasswordEncoder(passwordEncoder()); // Cung cấp cơ chế mã hóa password
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Value("${CLOUDINARY_URL:}")
    private String cloudinaryUrl;

    @Value("${cloudinary.cloud_name:}")
    private String cloudName;

    @Value("${cloudinary.api_key:}")
    private String apiKey;

    @Value("${cloudinary.api_secret:}")
    private String apiSecret;

    @Bean
    public Cloudinary cloudinary() {
        System.out.println("\n========================================");
        System.out.println("🔧 CLOUDINARY CONFIGURATION TEST");
        System.out.println("========================================");

        // Log thông tin config (ẩn secret)
        System.out.println("📋 Configuration values:");
        System.out.println("   CLOUDINARY_URL: " + (cloudinaryUrl != null && !cloudinaryUrl.trim().isEmpty()
            ? maskSecret(cloudinaryUrl) : "❌ NOT SET"));
        System.out.println("   cloudinary.cloud_name: " + (cloudName != null && !cloudName.trim().isEmpty()
            ? cloudName : "❌ NOT SET"));
        System.out.println("   cloudinary.api_key: " + (apiKey != null && !apiKey.trim().isEmpty()
            ? apiKey : "❌ NOT SET"));
        System.out.println("   cloudinary.api_secret: " + (apiSecret != null && !apiSecret.trim().isEmpty()
            ? "***hidden*** (length: " + apiSecret.length() + ")" : "❌ NOT SET"));

        Cloudinary cloudinary = null;
        boolean isConfigValid = false;

        // Ưu tiên sử dụng CLOUDINARY_URL nếu có
        if (cloudinaryUrl != null && !cloudinaryUrl.trim().isEmpty()) {
            System.out.println("\n✅ Using CLOUDINARY_URL configuration");
            cloudinary = new Cloudinary(cloudinaryUrl);
            isConfigValid = true;
        }
        // Fallback về cấu hình riêng lẻ
        else if (cloudName != null && !cloudName.trim().isEmpty()
            && apiKey != null && !apiKey.trim().isEmpty()
            && apiSecret != null && !apiSecret.trim().isEmpty()) {
            System.out.println("\n✅ Using individual configuration (cloud_name, api_key, api_secret)");
            cloudinary = new Cloudinary(ObjectUtils.asMap(
                    "cloud_name", cloudName,
                    "api_key", apiKey,
                    "api_secret", apiSecret
            ));
            isConfigValid = true;
        }
        // Nếu không có cấu hình nào
        else {
            System.err.println("\n❌ Cloudinary: No valid configuration found!");
            System.err.println("   Please set either:");
            System.err.println("   - CLOUDINARY_URL=cloudinary://api_key:api_secret@cloud_name");
            System.err.println("   OR all of:");
            System.err.println("   - CLOUDINARY_CLOUD_NAME=your-cloud-name");
            System.err.println("   - CLOUDINARY_API_KEY=your-api-key");
            System.err.println("   - CLOUDINARY_API_SECRET=your-api-secret");
            cloudinary = new Cloudinary(ObjectUtils.asMap(
                    "cloud_name", "disabled",
                    "api_key", "disabled",
                    "api_secret", "disabled"
            ));
            isConfigValid = false;
        }

        // Test kết nối chỉ khi có config hợp lệ
        if (cloudinary != null && isConfigValid) {
            testCloudinaryConnection(cloudinary);
        }

        System.out.println("========================================\n");

        return cloudinary;
    }

    /**
     * Test kết nối đến Cloudinary API
     */
    private void testCloudinaryConnection(Cloudinary cloudinary) {
        try {
            System.out.println("\n🔌 Testing Cloudinary connection...");

            // Lấy thông tin config từ Cloudinary instance
            Map<String, Object> config = cloudinary.config.asMap();
            String testCloudName = (String) config.get("cloud_name");

            System.out.println("   Cloud Name: " + testCloudName);
            System.out.println("   API Endpoint: https://api.cloudinary.com/v1_1/" + testCloudName);

            // Thử ping API bằng cách gọi method đơn giản (cần truyền empty map)
            Map pingResult = cloudinary.api().ping(ObjectUtils.emptyMap());

            if (pingResult != null && "ok".equals(pingResult.get("status"))) {
                System.out.println("✅ SUCCESS: Connected to Cloudinary API!");
                System.out.println("   Status: " + pingResult.get("status"));
            } else {
                System.out.println("⚠️  WARNING: Received unexpected response from Cloudinary");
            }

        } catch (Exception e) {
            System.err.println("❌ FAILED: Cannot connect to Cloudinary API");
            System.err.println("   Error: " + e.getMessage());
            System.err.println("   Error Type: " + e.getClass().getSimpleName());
            System.err.println("\n   Possible causes:");
            System.err.println("   1. Invalid API credentials");
            System.err.println("   2. Network/Firewall blocking connection");
            System.err.println("   3. IP address not whitelisted on Cloudinary Dashboard");
            System.err.println("   4. Cloudinary API is down (rare)");
            System.err.println("\n   💡 Troubleshooting:");
            System.err.println("   - Check Cloudinary Dashboard → Settings → Security");
            System.err.println("   - Verify 'Allowed Admin API IP addresses' is empty or contains your IP");

            // Lấy cloud name từ config để hiển thị command test
            try {
                Map<String, Object> config = cloudinary.config.asMap();
                String testCloudName = (String) config.get("cloud_name");
                System.err.println("   - Try: curl https://api.cloudinary.com/v1_1/" + testCloudName);
            } catch (Exception ignored) {
                System.err.println("   - Try: curl https://api.cloudinary.com/v1_1/YOUR_CLOUD_NAME");
            }
        }
    }

    /**
     * Mask secret trong URL để log an toàn
     */
    private String maskSecret(String url) {
        if (url == null || url.isEmpty()) {
            return url;
        }

        // cloudinary://api_key:api_secret@cloud_name
        if (url.contains("cloudinary://")) {
            try {
                String[] parts = url.split("@");
                if (parts.length == 2) {
                    String[] credentials = parts[0].split(":");
                    if (credentials.length >= 3) {
                        String apiKey = credentials[1].substring(2); // Remove "//"
                        return "cloudinary://" + apiKey + ":***hidden***@" + parts[1];
                    }
                }
            } catch (Exception e) {
                return "cloudinary://***masked***";
            }
        }

        return url;
    }
    
    @Bean("taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);     // Số luồng cơ bản luôn chạy
        executor.setMaxPoolSize(10);     // Số luồng tối đa khi tải cao
        executor.setQueueCapacity(100);  // Hàng đợi chờ xử lý
        executor.setThreadNamePrefix("CellexAsync-");
        executor.initialize();
        return executor;
    }
}
