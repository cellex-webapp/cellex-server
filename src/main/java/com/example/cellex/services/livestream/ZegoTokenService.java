package com.example.cellex.services.livestream;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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

    public String generateToken(String roomId, String userId, boolean isHost) {
        try {
            int effectiveTimeInSeconds = 3600; // 1 tiếng

            // 1. Chỉ cần tạo Payload quyền hạn theo chuẩn Zego
            Map<String, Object> payloadData = new HashMap<>();
            payloadData.put("room_id", roomId);
            Map<String, Integer> privilege = new HashMap<>();
            privilege.put("1", 1); // Quyền vào phòng
            privilege.put("2", isHost ? 1 : 0); // Quyền phát sóng (Mic/Camera)
            payloadData.put("privilege", privilege);
            payloadData.put("stream_id_list", null);
            String payloadString = objectMapper.writeValueAsString(payloadData);

            // 2. Giao toàn bộ việc mã hóa phức tạp cho file chính chủ của Zego
            TokenServerAssistant.TokenInfo tokenInfo = TokenServerAssistant.generateToken04(
                    appId, 
                    userId, 
                    serverSecret, 
                    effectiveTimeInSeconds, 
                    payloadString
            );

            // 3. Kiểm tra xem có lỗi sinh token không
            if (tokenInfo.error.code != TokenServerAssistant.ErrorCode.SUCCESS) {
                log.error("Zego Error: {}", tokenInfo.error.message);
                throw new RuntimeException("Lỗi từ thư viện Zego: " + tokenInfo.error.message);
            }

            // Trả về chuỗi token hoàn hảo (đã có sẵn prefix 04)
            return tokenInfo.data;

        } catch (Exception e) {
            log.error("Lỗi hệ thống khi sinh Token ZegoCloud", e);
            throw new RuntimeException("Lỗi hệ thống khi sinh Token Livestream");
        }
    }
}