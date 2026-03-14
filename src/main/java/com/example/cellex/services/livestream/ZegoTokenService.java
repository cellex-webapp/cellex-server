package com.example.cellex.services.livestream;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class ZegoTokenService {

    @Value("${zego.app.id}")
    private long appId;

    @Value("${zego.server.secret}")
    private String serverSecret;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Sinh Token thực tế của ZegoCloud (Chuẩn Token04)
     * @param roomId ID phòng (mã phiên Live)
     * @param userId ID của người dùng (Host hoặc Viewer)
     * @param isHost Quyền hạn (true nếu là Host, false nếu là Viewer)
     */
    public String generateToken(String roomId, String userId, boolean isHost) {
        log.info("Generating Zego Token04 for Room: {}, User: {}, isHost: {}", roomId, userId, isHost);

        // Token có hiệu lực trong 24 giờ
        int effectiveTimeInSeconds = 3600 * 24; 

        try {
            // ==========================================================
            // 1. TẠO PAYLOAD PHÂN QUYỀN (PRIVILEGE) THEO CHUẨN ZEGO
            // ==========================================================
            Map<String, Integer> privilege = new HashMap<>();
            privilege.put("1", 1); // Quyền đăng nhập phòng (1 = Cho phép)
            privilege.put("2", isHost ? 1 : 0); // Quyền phát video (Host = 1, Viewer = 0)

            Map<String, Object> payloadData = new HashMap<>();
            payloadData.put("room_id", roomId);
            payloadData.put("privilege", privilege);
            payloadData.put("stream_id_list", null);

            String payload = objectMapper.writeValueAsString(payloadData);

            // ==========================================================
            // 2. TẠO NỘI DUNG TOKEN (JSON BODY)
            // ==========================================================
            long createTime = System.currentTimeMillis() / 1000;
            long expireTime = createTime + effectiveTimeInSeconds;
            long nonce = Math.abs(new SecureRandom().nextLong());

            Map<String, Object> tokenInfo = new HashMap<>();
            tokenInfo.put("app_id", appId);
            tokenInfo.put("user_id", userId);
            tokenInfo.put("nonce", nonce);
            tokenInfo.put("ctime", createTime);
            tokenInfo.put("expire", expireTime);
            tokenInfo.put("payload", payload);

            String jsonBody = objectMapper.writeValueAsString(tokenInfo);

            // ==========================================================
            // 3. MÃ HÓA AES-256-CBC
            // ==========================================================
            byte[] iv = new byte[16];
            new SecureRandom().nextBytes(iv); // Khởi tạo vector ngẫu nhiên (IV)

            if (serverSecret.length() != 32) {
                log.warn("CẢNH BÁO: Zego Server Secret thường có đúng 32 ký tự!");
            }

            SecretKeySpec keySpec = new SecretKeySpec(serverSecret.getBytes(StandardCharsets.UTF_8), "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);

            byte[] encryptedBody = cipher.doFinal(jsonBody.getBytes(StandardCharsets.UTF_8));

            // ==========================================================
            // 4. ĐÓNG GÓI THÀNH BYTE BUFFER (ZEGO TOKEN04 FORMAT)
            // ==========================================================
            ByteBuffer buffer = ByteBuffer.allocate(28 + encryptedBody.length);
            buffer.order(ByteOrder.BIG_ENDIAN);
            buffer.put("04".getBytes(StandardCharsets.UTF_8)); // Token Version = "04"
            buffer.putShort((short) 16); // Độ dài IV
            buffer.put(iv); // Dữ liệu IV
            buffer.putShort((short) encryptedBody.length); // Độ dài phần mã hóa
            buffer.put(encryptedBody); // Dữ liệu mã hóa

            // ==========================================================
            // 5. TRẢ VỀ CHUỖI BASE64
            // ==========================================================
            return Base64.getEncoder().encodeToString(buffer.array());

        } catch (Exception e) {
            log.error("Failed to generate Zego Token", e);
            throw new RuntimeException("Lỗi hệ thống khi sinh Token Livestream: " + e.getMessage());
        }
    }
}