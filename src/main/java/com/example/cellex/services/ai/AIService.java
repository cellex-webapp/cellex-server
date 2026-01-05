package com.example.cellex.services.ai;

import com.example.cellex.dtos.request.ai.AIChatRequest;
import com.example.cellex.dtos.response.ai.AIChatResponse;
import com.example.cellex.dtos.response.ai.AIChatResponse.*;
import com.example.cellex.enums.Role;
import com.example.cellex.models.chat.AIConversation;
import com.example.cellex.models.chat.AIMessage;
import com.example.cellex.models.user.User;
import com.example.cellex.repositories.chat.AIConversationRepository;
import com.example.cellex.repositories.chat.AIMessageRepository;
import com.example.cellex.repositories.shop.ShopRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service xử lý AI Chat với Gemini API
 * Hỗ trợ Function Calling và Role-based AI
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AIService {

    private final AIToolsService aiToolsService;
    private final AIConversationRepository conversationRepository;
    private final AIMessageRepository messageRepository;
    private final ShopRepository shopRepository;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";
    private static final int MAX_CONTEXT_MESSAGES = 10;

    /**
     * Xử lý tin nhắn chat với AI
     */
    public AIChatResponse chat(AIChatRequest request, User currentUser) {
        log.info("Processing AI chat for user: {}, role: {}", currentUser.getId(), currentUser.getRole());

        // 1. Lấy hoặc tạo conversation
        AIConversation conversation = getOrCreateConversation(request.getConversationId(), currentUser, request.getShopId());

        // 2. Lưu tin nhắn user
        AIMessage userMessage = saveUserMessage(conversation, currentUser, request.getMessage());

        // 3. Lấy context từ history (exclude tin nhắn vừa lưu)
        List<AIMessage> contextMessages = getContextMessages(conversation.getId(), userMessage.getId());

        // 4. Tạo system prompt theo role
        String systemPrompt = buildSystemPrompt(currentUser.getRole(), currentUser.getId(), request.getShopId());

        // 5. Gọi Gemini API với Function Calling
        String aiResponse;
        AIMetadata metadata = null;
        String functionCalled = null;

        try {
            Map<String, Object> geminiResponse = callGeminiAPI(systemPrompt, contextMessages, request.getMessage(), currentUser.getRole(), request.getShopId());
            aiResponse = (String) geminiResponse.get("text");
            metadata = (AIMetadata) geminiResponse.get("metadata");
            functionCalled = (String) geminiResponse.get("functionCalled");
        } catch (Exception e) {
            log.error("Error calling Gemini API", e);
            aiResponse = "Xin lỗi, tôi đang gặp sự cố kỹ thuật. Vui lòng thử lại sau.";
        }

        // 6. Lưu tin nhắn AI
        AIMessage aiMessage = saveAIMessage(conversation, aiResponse, functionCalled, metadata);

        // 7. Cập nhật conversation
        updateConversation(conversation, aiResponse);

        // 8. Build response
        return buildResponse(conversation, aiMessage, aiResponse, metadata);
    }

    /**
     * Lấy lịch sử conversation
     */
    public Page<AIConversation> getConversations(String userId, int page, int size) {
        return conversationRepository.findByUserIdAndIsActiveTrueOrderByLastMessageAtDesc(
            userId, PageRequest.of(page, size));
    }

    /**
     * Lấy tin nhắn của một conversation
     */
    public Page<AIMessage> getMessages(String conversationId, String userId, int page, int size) {
        // Verify ownership
        conversationRepository.findByIdAndUserId(conversationId, userId)
            .orElseThrow(() -> new RuntimeException("Conversation not found"));

        return messageRepository.findByConversationIdOrderByCreatedAtAsc(
            conversationId, PageRequest.of(page, size));
    }

    /**
     * Xóa conversation
     */
    public void deleteConversation(String conversationId, String userId) {
        AIConversation conversation = conversationRepository.findByIdAndUserId(conversationId, userId)
            .orElseThrow(() -> new RuntimeException("Conversation not found"));

        conversation.setIsActive(false);
        conversationRepository.save(conversation);
    }

    // ==================== PRIVATE METHODS ====================

    private AIConversation getOrCreateConversation(String conversationId, User user, String shopId) {
        if (conversationId != null && !conversationId.isEmpty()) {
            return conversationRepository.findByIdAndUserId(conversationId, user.getId())
                .orElseGet(() -> createNewConversation(user, shopId));
        }
        return createNewConversation(user, shopId);
    }

    private AIConversation createNewConversation(User user, String shopId) {
        AIConversation conversation = AIConversation.builder()
            .userId(user.getId())
            .userRole(user.getRole().name())
            .shopId(shopId)
            .title("Cuộc hội thoại mới")
            .messageCount(0)
            .isActive(true)
            .createdAt(LocalDateTime.now())
            .build();

        return conversationRepository.save(conversation);
    }

    private AIMessage saveUserMessage(AIConversation conversation, User user, String content) {
        AIMessage message = AIMessage.builder()
            .userId(user.getId())
            .conversationId(conversation.getId())
            .messageType(AIMessage.AIMessageType.USER)
            .content(content)
            .userRole(user.getRole().name())
            .shopId(conversation.getShopId())
            .createdAt(LocalDateTime.now())
            .build();

        return messageRepository.save(message);
    }

    private AIMessage saveAIMessage(AIConversation conversation, String content, String functionCalled, AIMetadata metadata) {
        Map<String, Object> metadataMap = null;
        if (metadata != null) {
            try {
                String json = objectMapper.writeValueAsString(metadata);
                metadataMap = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
            } catch (JsonProcessingException e) {
                log.error("Error converting metadata", e);
            }
        }

        AIMessage message = AIMessage.builder()
            .userId(conversation.getUserId())
            .conversationId(conversation.getId())
            .messageType(AIMessage.AIMessageType.AI)
            .content(content)
            .userRole(conversation.getUserRole())
            .shopId(conversation.getShopId())
            .functionCalled(functionCalled)
            .metadata(metadataMap)
            .createdAt(LocalDateTime.now())
            .build();

        return messageRepository.save(message);
    }

    private void updateConversation(AIConversation conversation, String lastMessage) {
        conversation.setMessageCount(conversation.getMessageCount() + 2);
        conversation.setLastMessage(lastMessage.length() > 100 ? lastMessage.substring(0, 100) + "..." : lastMessage);
        conversation.setLastMessageAt(LocalDateTime.now());

        // Tự động đặt title từ tin nhắn đầu tiên
        if (conversation.getMessageCount() == 2 && "Cuộc hội thoại mới".equals(conversation.getTitle())) {
            conversation.setTitle(lastMessage.length() > 50 ? lastMessage.substring(0, 50) + "..." : lastMessage);
        }

        conversationRepository.save(conversation);
    }

    private List<AIMessage> getContextMessages(String conversationId, String excludeMessageId) {
        List<AIMessage> allMessages = messageRepository.findTopByConversationIdOrderByCreatedAtDesc(
            conversationId, PageRequest.of(0, MAX_CONTEXT_MESSAGES + 1)); // +1 to account for excluded message
        
        // Filter out the current user message to avoid duplication
        return allMessages.stream()
            .filter(msg -> !msg.getId().equals(excludeMessageId))
            .limit(MAX_CONTEXT_MESSAGES)
            .collect(Collectors.toList());
    }

    private String buildSystemPrompt(Role role, String userId, String shopId) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("Bạn là Cellex AI Assistant - trợ lý AI thông minh của sàn thương mại điện tử Cellex. ");
        prompt.append("Bạn luôn trả lời bằng tiếng Việt, thân thiện và chuyên nghiệp. ");
        prompt.append("Ngày hôm nay là: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append(". ");

        switch (role) {
            case USER:
                prompt.append("\n\n=== VAI TRÒ: TƯ VẤN KHÁCH HÀNG ===\n");
                prompt.append("Bạn đang hỗ trợ một KHÁCH HÀNG (userId: ").append(userId).append(").\n");
                prompt.append("Nhiệm vụ chính:\n");
                prompt.append("1. Tư vấn chọn sản phẩm công nghệ phù hợp nhu cầu và ngân sách\n");
                prompt.append("2. So sánh cấu hình, thông số kỹ thuật giữa các sản phẩm\n");
                prompt.append("3. Gợi ý sản phẩm dựa trên sở thích và lịch sử mua hàng\n");
                prompt.append("4. Giải đáp thắc mắc về sản phẩm, chính sách đổi trả, bảo hành\n\n");
                prompt.append("Khi gợi ý sản phẩm, HÃY SỬ DỤNG các function tools để tìm kiếm và lấy thông tin sản phẩm thực từ database.\n");
                prompt.append("VÍ DỤ: User hỏi 'các sản phẩm bán chạy', BẮT BUỘC gọi getHotProducts() để lấy dữ liệu thực.\n");
                prompt.append("VÍ DỤ: User hỏi về spec cụ thể như 'điện thoại pin trâu nhất', 'laptop RAM lớn nhất', 'màn hình to nhất', BẮT BUỘC gọi searchByAttribute() với attributeKey tương ứng (battery, ram, screen).\n");
                prompt.append("QUAN TRỌNG: Các attribute key phổ biến: battery (pin), ram (RAM), screen (màn hình), storage (bộ nhớ), cpu (CPU), vga (card đồ họa).\n");
                prompt.append("Khi trả về danh sách sản phẩm, luôn bao gồm productIds trong metadata để frontend render Product Cards.\n");
                break;

            case VENDOR:
                prompt.append("\n\n=== VAI TRÒ: HỖ TRỢ NGƯỜI BÁN ===\n");
                prompt.append("Bạn đang hỗ trợ một VENDOR (userId: ").append(userId);
                if (shopId != null) {
                    prompt.append(", shopId: ").append(shopId);
                }
                prompt.append(").\n");
                prompt.append("Nhiệm vụ chính:\n");
                prompt.append("1. Phân tích doanh thu, báo cáo kinh doanh của shop\n");
                prompt.append("2. Liệt kê sản phẩm bán chạy, cảnh báo hàng tồn kho thấp\n");
                prompt.append("3. Phân tích đơn hàng theo trạng thái\n");
                prompt.append("4. Gợi ý chiến lược coupon/khuyến mãi để tăng doanh số\n");
                prompt.append("5. Tư vấn cải thiện hiệu quả kinh doanh\n\n");
                prompt.append("HÃY SỬ DỤNG các function tools để truy vấn dữ liệu thực từ database.\n");
                prompt.append("VÍ DỤ: Khi user hỏi 'phân tích kinh doanh quý 1/2026', BẮT BUỘC gọi getRevenueStats với startDate='2026-01-01', endDate='2026-03-31'\n");
                prompt.append("VÍ DỤ: Khi user hỏi 'sản phẩm tồn kho thấp' hoặc 'sản phẩm sắp hết hàng', BẮT BUỘC gọi getLowStockProducts() NGAY LẬP TỨC\n");
                prompt.append("VÍ DỤ: Khi user hỏi 'sản phẩm bán chạy', BẮT BUỘC gọi getTopSellingProducts() NGAY LẬP TỨC\n");
                prompt.append("VÍ DỤ: Khi user hỏi 'doanh thu' hoặc 'báo cáo', BẮT BUỘC gọi getRevenueStats() NGAY LẬP TỨC\n");
                prompt.append("VÍ DỤ: Khi user hỏi 'chiến lược kinh doanh', 'đề xuất để tăng doanh số', 'làm sao cải thiện', BẮT BUỘC gọi suggestVendorBusinessStrategy() để phân tích toàn diện\n");
                prompt.append("KHÔNG BAO GIỜ chỉ nói 'tôi sẽ kiểm tra' hay 'hãy để tôi xem' - PHẢI GỌI FUNCTION NGAY\n");
                prompt.append("Khi báo cáo số liệu, luôn format tiền tệ VNĐ và cung cấp chartData trong metadata nếu có thể.\n");
                break;

            case ADMIN:
                prompt.append("\n\n=== VAI TRÒ: HỖ TRỢ QUẢN TRỊ ===\n");
                prompt.append("Bạn đang hỗ trợ một ADMIN (userId: ").append(userId).append(").\n");
                prompt.append("Nhiệm vụ chính:\n");
                prompt.append("1. Tổng hợp doanh thu, báo cáo toàn hệ thống\n");
                prompt.append("2. Phân tích hiệu quả các phân khúc khách hàng (CustomerSegment)\n");
                prompt.append("3. Gợi ý chiến dịch marketing tổng thể\n");
                prompt.append("4. Đề xuất điều chỉnh phân khúc khách hàng\n");
                prompt.append("5. Cung cấp insights về hoạt động của sàn\n\n");
                prompt.append("HÃY SỬ DỤNG các function tools để truy vấn dữ liệu thực từ database.\n");
                prompt.append("VÍ DỤ: User hỏi 'chiến lược kinh doanh quý tiếp theo', BẮT BUỘC gọi suggestAdminBusinessStrategy() để có phân tích toàn diện.\n");
                prompt.append("VÍ DỤ: User hỏi 'tổng quan hệ thống', BẮT BUỘC gọi getSystemOverview() ngay lập tức.\n");
                prompt.append("VÍ DỤ: User hỏi 'đề xuất phát triển platform', 'làm sao tăng GMV', BẮT BUỘC gọi suggestAdminBusinessStrategy().\n");
                prompt.append("KHÔNG được chỉ nói 'cần phân tích' mà phải GỌI FUNCTION để lấy dữ liệu thực.\n");
                prompt.append("Cung cấp chartData và tableData trong metadata để hiển thị trực quan.\n");
                break;
        }

        prompt.append("\n=== QUY TẮC QUAN TRỌNG ===\n");
        prompt.append("1. BẮT BUỘC phải sử dụng function tools để lấy dữ liệu thực. KHÔNG BAO GIỜ trả lời chung chung hoặc bịa số liệu\n");
        prompt.append("2. Nếu user hỏi về sản phẩm/doanh thu/đơn hàng/phân khúc, PHẢI gọi function tương ứng TRƯỚC KHI trả lời\n");
        prompt.append("3. KHÔNG được trả lời kiểu 'hãy xem xét', 'cần phân tích', 'tôi sẽ kiểm tra', 'để tôi xem' mà KHÔNG gọi function\n");
        prompt.append("4. KHÔNG BAO GIỜ nói 'chờ tôi kiểm tra' hay 'tôi sẽ giúp bạn kiểm tra' - PHẢI GỌI FUNCTION NGAY LẬP TỨC\n");
        prompt.append("5. Khi không tìm thấy dữ liệu từ function, hãy thông báo rõ ràng 'Không tìm thấy dữ liệu'\n");
        prompt.append("6. Format tiền tệ: VNĐ với dấu phân cách hàng nghìn (ví dụ: 1.500.000đ)\n");
        prompt.append("7. Khi gợi ý sản phẩm, ĐẢM BẢO trả về productIds trong response\n");
        prompt.append("8. KHÔNG BAO GIỜ hiển thị ID sản phẩm, ID danh mục hay bất kỳ ID nào trong phản hồi văn bản\n");
        prompt.append("9. Chỉ đề cập đến tên sản phẩm, tên danh mục, và thông tin có ý nghĩa\n");
        prompt.append("10. Trả lời CHI TIẾT với dữ liệu cụ thể, giải thích cặn kẽ, phân tích xu hướng và đưa ra đề xuất\n");
        prompt.append("\n⚠️ CẢNH BÁO: Nếu bạn trả lời bằng văn bản mà KHÔNG gọi function khi cần thiết, câu trả lời của bạn sẽ BỊ TỪ CHỐI!\n");
        prompt.append("✅ ĐÚNG: User hỏi 'tồn kho thấp' → Gọi getLowStockProducts() ngay → Trả về dữ liệu\n");
        prompt.append("❌ SAI: User hỏi 'tồn kho thấp' → Trả lời 'Tôi sẽ kiểm tra...' → KHÔNG được phép!\n");

        return prompt.toString();
    }

    private Map<String, Object> callGeminiAPI(String systemPrompt, List<AIMessage> contextMessages,
                                               String userMessage, Role role, String shopId) {
        String apiUrl = GEMINI_API_URL + "?key=" + geminiApiKey;

        // Build request body
        Map<String, Object> requestBody = buildGeminiRequest(systemPrompt, contextMessages, userMessage, role, shopId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return processGeminiResponse(response.getBody(), role, shopId, userMessage);
            }
        } catch (Exception e) {
            log.error("Error calling Gemini API: {}", e.getMessage());
            throw new RuntimeException("Failed to call Gemini API", e);
        }

        Map<String, Object> fallbackResult = new HashMap<>();
        fallbackResult.put("text", "Không thể xử lý yêu cầu. Vui lòng thử lại.");
        fallbackResult.put("metadata", null);
        return fallbackResult;
    }

    private Map<String, Object> buildGeminiRequest(String systemPrompt, List<AIMessage> contextMessages,
                                                    String userMessage, Role role, String shopId) {
        Map<String, Object> request = new HashMap<>();

        // System instruction
        Map<String, Object> systemInstruction = new HashMap<>();
        systemInstruction.put("parts", List.of(Map.of("text", systemPrompt)));
        request.put("system_instruction", systemInstruction);

        // Contents (history + new message)
        List<Map<String, Object>> contents = new ArrayList<>();

        // Add context messages (reversed to chronological order)
        List<AIMessage> reversedMessages = new ArrayList<>(contextMessages);
        Collections.reverse(reversedMessages);
        for (AIMessage msg : reversedMessages) {
            Map<String, Object> content = new HashMap<>();
            content.put("role", msg.getMessageType() == AIMessage.AIMessageType.USER ? "user" : "model");
            content.put("parts", List.of(Map.of("text", msg.getContent())));
            contents.add(content);
        }

        // Add new user message
        Map<String, Object> newMessage = new HashMap<>();
        newMessage.put("role", "user");
        newMessage.put("parts", List.of(Map.of("text", userMessage)));
        contents.add(newMessage);

        request.put("contents", contents);

        // Tools (Function declarations)
        request.put("tools", List.of(Map.of("function_declarations", buildFunctionDeclarations(role))));

        // Tool config - ưu tiên gọi function khi có thể
        Map<String, Object> toolConfig = new HashMap<>();
        Map<String, Object> functionCallingConfig = new HashMap<>();
        functionCallingConfig.put("mode", "AUTO"); // AUTO mode để ưu tiên function calling
        toolConfig.put("function_calling_config", functionCallingConfig);
        request.put("tool_config", toolConfig);

        // Generation config
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 0.8);
        generationConfig.put("topK", 40);
        generationConfig.put("topP", 0.95);
        generationConfig.put("maxOutputTokens", 16384);
        request.put("generation_config", generationConfig);

        return request;
    }

    private List<Map<String, Object>> buildFunctionDeclarations(Role role) {
        List<Map<String, Object>> functions = new ArrayList<>();

        // Common functions for all roles
        functions.add(buildFunction("searchProducts", "Tìm kiếm sản phẩm theo keyword, category, giá. Keyword có thể là tên sản phẩm hoặc tên danh mục (ví dụ: 'laptop', 'điện thoại', 'tai nghe')",
            Map.of(
                "keyword", Map.of("type", "string", "description", "Từ khóa tìm kiếm (tên sản phẩm hoặc tên danh mục như 'laptop', 'điện thoại')"),
                "categoryId", Map.of("type", "string", "description", "ID danh mục"),
                "minPrice", Map.of("type", "number", "description", "Giá tối thiểu"),
                "maxPrice", Map.of("type", "number", "description", "Giá tối đa"),
                "limit", Map.of("type", "integer", "description", "Số lượng kết quả (mặc định 10)")
            )));

        functions.add(buildFunction("getProductDetails", "Lấy thông tin chi tiết sản phẩm",
            Map.of("productId", Map.of("type", "string", "description", "ID sản phẩm"))));

        functions.add(buildFunction("compareProducts", "So sánh nhiều sản phẩm",
            Map.of("productIds", Map.of("type", "array", "items", Map.of("type", "string"),
                "description", "Danh sách ID sản phẩm cần so sánh"))));

        functions.add(buildFunction("getSimilarProducts", "Gợi ý sản phẩm tương tự",
            Map.of(
                "productId", Map.of("type", "string", "description", "ID sản phẩm gốc"),
                "limit", Map.of("type", "integer", "description", "Số lượng gợi ý")
            )));

        functions.add(buildFunction("getHotProducts", "Lấy sản phẩm hot/bán chạy",
            Map.of(
                "categoryId", Map.of("type", "string", "description", "ID danh mục (tùy chọn)"),
                "limit", Map.of("type", "integer", "description", "Số lượng kết quả")
            )));

        functions.add(buildFunction("searchByAttribute", "Tìm và sắp xếp sản phẩm theo thuộc tính kỹ thuật (pin, RAM, màn hình, storage, CPU). Dùng khi user hỏi về spec cụ thể như 'pin trâu nhất', 'RAM lớn nhất', 'màn hình to nhất' HOẶC 'RAM 12GB', 'pin 5000mAh'",
            Map.of(
                "keyword", Map.of("type", "string", "description", "Từ khóa loại sản phẩm (laptop, điện thoại...)"),
                "attributeKey", Map.of("type", "string", "description", "Tên attribute (battery, ram, screen, storage, cpu, vga). VÍ DỤ: battery cho pin, ram cho RAM, screen cho màn hình"),
                "attributeValue", Map.of("type", "string", "description", "Giá trị cụ thể cần tìm (VD: '12' cho RAM 12GB, '5000' cho pin 5000mAh). Để trống nếu muốn lấy các sản phẩm lớn nhất/nhỏ nhất"),
                "sortOrder", Map.of("type", "string", "description", "Thứ tự sắp xếp: 'desc' (lớn nhất) hoặc 'asc' (nhỏ nhất). Chỉ dùng khi không có attributeValue"),
                "limit", Map.of("type", "integer", "description", "Số lượng kết quả")
            )));

        functions.add(buildFunction("getCategories", "Lấy danh sách categories", Map.of()));

        // USER-specific functions
        if (role == Role.USER) {
            functions.add(buildFunction("getPersonalizedRecommendations", "Gợi ý sản phẩm cá nhân hóa cho user",
                Map.of(
                    "userId", Map.of("type", "string", "description", "ID của user"),
                    "limit", Map.of("type", "integer", "description", "Số lượng gợi ý")
                )));
        }

        // VENDOR-specific functions
        if (role == Role.VENDOR) {
            functions.add(buildFunction("getRevenueStats", "Thống kê doanh thu shop",
                Map.of(
                    "shopId", Map.of("type", "string", "description", "ID shop"),
                    "startDate", Map.of("type", "string", "description", "Ngày bắt đầu (YYYY-MM-DD)"),
                    "endDate", Map.of("type", "string", "description", "Ngày kết thúc (YYYY-MM-DD)")
                )));

            functions.add(buildFunction("getTopSellingProducts", "Lấy sản phẩm bán chạy nhất của shop",
                Map.of(
                    "shopId", Map.of("type", "string", "description", "ID shop"),
                    "limit", Map.of("type", "integer", "description", "Số lượng")
                )));

            functions.add(buildFunction("getLowStockProducts", "Cảnh báo hàng tồn kho thấp",
                Map.of(
                    "shopId", Map.of("type", "string", "description", "ID shop"),
                    "threshold", Map.of("type", "integer", "description", "Ngưỡng cảnh báo (mặc định 10)")
                )));

            functions.add(buildFunction("suggestCoupons", "Gợi ý tạo coupon cho sản phẩm tiềm năng",
                Map.of("shopId", Map.of("type", "string", "description", "ID shop"))));

            functions.add(buildFunction("getOrderAnalytics", "Phân tích đơn hàng của shop",
                Map.of(
                    "shopId", Map.of("type", "string", "description", "ID shop"),
                    "startDate", Map.of("type", "string", "description", "Ngày bắt đầu"),
                    "endDate", Map.of("type", "string", "description", "Ngày kết thúc")
                )));

            functions.add(buildFunction("suggestVendorBusinessStrategy", "Đề xuất chiến lược kinh doanh toàn diện cho shop dựa trên phân tích sâu doanh thu, sản phẩm, tồn kho, xu hướng. Dùng khi vendor hỏi về 'chiến lược', 'đề xuất kinh doanh', 'làm sao để tăng doanh thu', 'cải thiện hiệu quả'",
                Map.of(
                    "shopId", Map.of("type", "string", "description", "ID shop"),
                    "daysToAnalyze", Map.of("type", "integer", "description", "Số ngày phân tích (mặc định 30)")
                )));
        }

        // ADMIN-specific functions
        if (role == Role.ADMIN) {
            functions.add(buildFunction("getSystemRevenue", "Tổng hợp doanh thu toàn hệ thống",
                Map.of(
                    "startDate", Map.of("type", "string", "description", "Ngày bắt đầu (YYYY-MM-DD)"),
                    "endDate", Map.of("type", "string", "description", "Ngày kết thúc (YYYY-MM-DD)")
                )));

            functions.add(buildFunction("getSegmentAnalytics", "Phân tích hiệu quả phân khúc khách hàng",
                Map.of()));

            functions.add(buildFunction("suggestSegmentAdjustments", "Gợi ý điều chỉnh phân khúc khách hàng",
                Map.of()));

            functions.add(buildFunction("getSystemOverview", "Tổng quan hệ thống",
                Map.of()));

            functions.add(buildFunction("suggestAdminBusinessStrategy", "Đề xuất chiến lược kinh doanh toàn hệ thống dựa trên phân tích sâu GMV, vendors, segments, growth opportunities. Dùng khi admin hỏi về 'chiến lược tổng thể', 'làm sao phát triển hệ thống', 'tăng trưởng platform', 'đề xuất mở rộng'",
                Map.of(
                    "daysToAnalyze", Map.of("type", "integer", "description", "Số ngày phân tích (mặc định 30)")
                )));
        }

        return functions;
    }

    private Map<String, Object> buildFunction(String name, String description, Map<String, Object> properties) {
        Map<String, Object> function = new HashMap<>();
        function.put("name", name);
        function.put("description", description);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("type", "object");
        parameters.put("properties", properties);
        function.put("parameters", parameters);

        return function;
    }

    private Map<String, Object> processGeminiResponse(String responseBody, Role role, String shopId, String userMessage) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode candidates = root.path("candidates");

            if (candidates.isArray() && candidates.size() > 0) {
                JsonNode content = candidates.get(0).path("content");
                JsonNode parts = content.path("parts");

                if (parts.isArray() && parts.size() > 0) {
                    JsonNode firstPart = parts.get(0);

                    // Check for function call
                    if (firstPart.has("functionCall")) {
                        JsonNode functionCall = firstPart.path("functionCall");
                        String functionName = functionCall.path("name").asText();
                        JsonNode args = functionCall.path("args");

                        log.info("Function call detected: {} with args: {}", functionName, args);

                        // Execute function and get result
                        Map<String, Object> functionResult = executeFunctionCall(functionName, args, shopId);

                        // Call Gemini again with function result to get natural language response
                        return callGeminiWithFunctionResult(functionName, functionResult, role, shopId);
                    }

                    // Regular text response (không có function call)
                    String text = firstPart.path("text").asText("");
                    log.warn("AI returned text response without function call. This may lack specific data. Text: {}", text.substring(0, Math.min(100, text.length())));
                    
                    // Kiểm tra xem có phải là câu trả lời cần gọi function không
                    String lowerText = text.toLowerCase();
                    String warningMessage = null;
                    String suggestedFunction = detectMissingFunctionCall(userMessage != null ? userMessage : "", role.toString());
                    
                    if (lowerText.contains("tôi sẽ") || lowerText.contains("để tôi") || 
                        lowerText.contains("chờ tôi") || lowerText.contains("hãy để")) {
                        if (suggestedFunction != null) {
                            warningMessage = String.format("\n\n⚠️ *Hệ thống phát hiện AI chưa gọi hàm %s. Vui lòng hỏi lại rõ hơn hoặc sử dụng từ khóa cụ thể.*", suggestedFunction);
                        } else {
                            warningMessage = "\n\n⚠️ *Lưu ý: AI đang cố trả lời nhưng chưa lấy được dữ liệu thực tế. Vui lòng hỏi lại cụ thể hơn hoặc thử lại.*";
                        }
                        log.error("AI violated function calling rule - returned promise without calling function. Suggested: {}", suggestedFunction);
                    } else if (suggestedFunction != null && text.length() < 200) {
                        // Nếu detect được function nên gọi nhưng AI không gọi và trả lời ngắn
                        warningMessage = String.format("\n\n💡 *Gợi ý: Thử hỏi 'Cho tôi xem dữ liệu chi tiết về %s' để nhận thông tin đầy đủ.*", 
                            suggestedFunction.replace("get", "").replace("suggest", ""));
                        log.error("AI may have missed function call {}. User message was unclear.", suggestedFunction);
                    }
                    
                    if (warningMessage != null) {
                        text = text + warningMessage;
                    }
                    
                    Map<String, Object> result = new HashMap<>();
                    result.put("text", text);
                    result.put("metadata", null);
                    result.put("functionCalled", "");
                    return result;
                }
            }
        } catch (Exception e) {
            log.error("Error processing Gemini response", e);
        }

        Map<String, Object> errorResult = new HashMap<>();
        errorResult.put("text", "Không thể xử lý phản hồi từ AI.");
        errorResult.put("metadata", null);
        errorResult.put("functionCalled", "");
        return errorResult;
    }

    private Map<String, Object> executeFunctionCall(String functionName, JsonNode args, String shopId) {
        log.info("Executing function: {}", functionName);

        try {
            switch (functionName) {
                case "searchProducts":
                    return aiToolsService.searchProducts(
                        args.path("keyword").asText(null),
                        args.path("categoryId").asText(null),
                        args.has("minPrice") ? args.path("minPrice").asDouble() : null,
                        args.has("maxPrice") ? args.path("maxPrice").asDouble() : null,
                        args.has("limit") ? args.path("limit").asInt() : null
                    );

                case "getProductDetails":
                    return aiToolsService.getProductDetails(args.path("productId").asText());

                case "compareProducts":
                    List<String> productIds = new ArrayList<>();
                    args.path("productIds").forEach(node -> productIds.add(node.asText()));
                    return aiToolsService.compareProducts(productIds);

                case "getSimilarProducts":
                    return aiToolsService.getSimilarProducts(
                        args.path("productId").asText(),
                        args.has("limit") ? args.path("limit").asInt() : null
                    );

                case "getHotProducts":
                    return aiToolsService.getHotProducts(
                        args.path("categoryId").asText(null),
                        args.has("limit") ? args.path("limit").asInt() : null
                    );

                case "searchByAttribute":
                    return aiToolsService.searchByAttribute(
                        args.path("keyword").asText(null),
                        args.path("attributeKey").asText(null),
                        args.path("attributeValue").asText(null),
                        args.path("sortOrder").asText("desc"),
                        args.has("limit") ? args.path("limit").asInt() : null
                    );

                case "getCategories":
                    return aiToolsService.getCategories();

                case "getPersonalizedRecommendations":
                    return aiToolsService.getPersonalizedRecommendations(
                        args.path("userId").asText(),
                        args.has("limit") ? args.path("limit").asInt() : null
                    );

                case "getRevenueStats":
                    return aiToolsService.getRevenueStats(
                        args.has("shopId") ? args.path("shopId").asText() : shopId,
                        args.path("startDate").asText(null),
                        args.path("endDate").asText(null)
                    );

                case "getTopSellingProducts":
                    return aiToolsService.getTopSellingProducts(
                        args.has("shopId") ? args.path("shopId").asText() : shopId,
                        args.has("limit") ? args.path("limit").asInt() : null
                    );

                case "getLowStockProducts":
                    return aiToolsService.getLowStockProducts(
                        args.has("shopId") ? args.path("shopId").asText() : shopId,
                        args.has("threshold") ? args.path("threshold").asInt() : null
                    );

                case "suggestCoupons":
                    return aiToolsService.suggestCoupons(
                        args.has("shopId") ? args.path("shopId").asText() : shopId
                    );

                case "getOrderAnalytics":
                    return aiToolsService.getOrderAnalytics(
                        args.has("shopId") ? args.path("shopId").asText() : shopId,
                        args.path("startDate").asText(null),
                        args.path("endDate").asText(null)
                    );

                case "getSystemRevenue":
                    return aiToolsService.getSystemRevenue(
                        args.path("startDate").asText(null),
                        args.path("endDate").asText(null)
                    );

                case "getSegmentAnalytics":
                    return aiToolsService.getSegmentAnalytics();

                case "suggestSegmentAdjustments":
                    return aiToolsService.suggestSegmentAdjustments();

                case "getSystemOverview":
                    return aiToolsService.getSystemOverview();

                case "suggestVendorBusinessStrategy":
                    return aiToolsService.suggestVendorBusinessStrategy(
                        shopId,
                        args.has("daysToAnalyze") ? args.path("daysToAnalyze").asInt() : null
                    );

                case "suggestAdminBusinessStrategy":
                    return aiToolsService.suggestAdminBusinessStrategy(
                        args.has("daysToAnalyze") ? args.path("daysToAnalyze").asInt() : null
                    );

                default:
                    return Map.of("error", "Unknown function: " + functionName);
            }
        } catch (Exception e) {
            log.error("Error executing function {}: {}", functionName, e.getMessage());
            return Map.of("error", e.getMessage());
        }
    }

    private Map<String, Object> callGeminiWithFunctionResult(String functionName, Map<String, Object> functionResult,
                                                              Role role, String shopId) {
        // Instead of calling Gemini again, directly format the result
        // This avoids the "function call turn" error from Gemini API
        log.info("Formatting function result for: {} with data: {}", functionName, functionResult);
        
        // Kiểm tra xem có dữ liệu không
        if (functionResult == null || functionResult.isEmpty() || functionResult.containsKey("error")) {
            log.warn("Function {} returned no data or error: {}", functionName, functionResult);
        }
        
        String formattedText = formatFunctionResultAsText(functionName, functionResult);
        AIMetadata metadata = buildMetadataFromFunctionResult(functionName, functionResult);
        
        log.info("Formatted text length: {}, metadata: {}", formattedText.length(), metadata != null ? "present" : "null");
        
        Map<String, Object> result = new HashMap<>();
        result.put("text", formattedText);
        result.put("metadata", metadata);
        result.put("functionCalled", functionName);
        return result;
    }

    @SuppressWarnings("unchecked")
    private AIMetadata buildMetadataFromFunctionResult(String functionName, Map<String, Object> result) {
        AIMetadata.AIMetadataBuilder builder = AIMetadata.builder();
        builder.functionCalled(functionName);

        // Extract productIds if present (không hiển thị ID trong text, chỉ dùng để render card)
        if (result.containsKey("productIds")) {
            builder.productIds((List<String>) result.get("productIds"));
        }

        // Extract chartData if present
        if (result.containsKey("chartData")) {
            Object chartDataObj = result.get("chartData");
            if (chartDataObj instanceof ChartData) {
                builder.chartData((ChartData) chartDataObj);
            }
        }

        // Extract coupon suggestions if present
        if (result.containsKey("suggestions") && "suggestCoupons".equals(functionName)) {
            Object suggestions = result.get("suggestions");
            if (suggestions instanceof List) {
                builder.couponSuggestions((List<CouponSuggestion>) suggestions);
            }
        }

        // Extract table data for analytics and comparisons
        if (result.containsKey("segments") || result.containsKey("products") || 
            result.containsKey("lowStockProducts") || result.containsKey("comparison")) {
            
            List<Map<String, Object>> tableData = new ArrayList<>();
            
            if (result.containsKey("comparison")) {
                // Bảng so sánh sản phẩm - thêm STT và sắp xếp cột
                List<Map<String, Object>> comparison = (List<Map<String, Object>>) result.get("comparison");
                tableData = formatComparisonTable(comparison);
            } else if ("compareProducts".equals(functionName) && result.containsKey("products")) {
                // compareProducts cũng trả về products nhưng cần format như comparison
                List<Map<String, Object>> comparison = (List<Map<String, Object>>) result.get("products");
                tableData = formatComparisonTable(comparison);
            } else if (result.containsKey("segments")) {
                tableData = addIndexToTable((List<Map<String, Object>>) result.get("segments"));
            } else if (result.containsKey("products")) {
                tableData = addIndexToTable((List<Map<String, Object>>) result.get("products"));
            } else if (result.containsKey("lowStockProducts")) {
                tableData = addIndexToTable((List<Map<String, Object>>) result.get("lowStockProducts"));
            }
            
            if (!tableData.isEmpty()) {
                builder.tableData(tableData);
            }
        }

        return builder.build();
    }

    // Thêm STT vào bảng và ưu tiên cột tên quan trọng ở vị trí thứ 2
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> addIndexToTable(List<Map<String, Object>> data) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < data.size(); i++) {
            Map<String, Object> original = data.get(i);
            Map<String, Object> row = new LinkedHashMap<>();
            
            // 1. STT
            row.put("stt", i + 1);
            
            // 2. Tên (ưu tiên vị trí thứ 2) - tìm theo thứ tự ưu tiên
            String nameKey = findNameKey(original);
            if (nameKey != null) {
                // Đổi tên key thành dạng chuẩn tương ứng
                String displayKey = getDisplayNameKey(nameKey);
                row.put(displayKey, original.get(nameKey));
            }
            
            // 3. Các cột còn lại (loại bỏ các trường ID không cần thiết)
            for (Map.Entry<String, Object> entry : original.entrySet()) {
                String key = entry.getKey();
                // Bỏ qua các trường đã thêm, *Id (trừ categoryId nếu cần), và các tên đã được chuẩn hóa
                if (!row.containsKey(key) && 
                    !row.containsKey(getDisplayNameKey(key)) &&
                    (!key.endsWith("Id") || key.equals("categoryId")) && 
                    !isNameField(key)) {
                    row.put(key, entry.getValue());
                }
            }
            
            result.add(row);
        }
        return result;
    }
    
    /**
     * Tìm key chứa tên trong map theo thứ tự ưu tiên
     */
    private String findNameKey(Map<String, Object> map) {
        // Thứ tự ưu tiên: segmentName > productName > categoryName > name
        String[] nameKeys = {"segmentName", "productName", "categoryName", "shopName", "userName", "name"};
        for (String key : nameKeys) {
            if (map.containsKey(key)) {
                return key;
            }
        }
        return null;
    }
    
    /**
     * Kiểm tra xem key có phải là trường tên không
     */
    private boolean isNameField(String key) {
        return key.equals("name") || key.equals("productName") || key.equals("segmentName") || 
               key.equals("categoryName") || key.equals("shopName") || key.equals("userName");
    }
    
    /**
     * Lấy tên hiển thị chuẩn cho key
     */
    private String getDisplayNameKey(String key) {
        switch (key) {
            case "productName":
                return "name";
            case "segmentName":
                return "name";
            case "categoryName":
                return "name";
            case "shopName":
                return "name";
            case "userName":
                return "name";
            default:
                return key;
        }
    }

    // Format bảng so sánh - đưa cột tên lên đầu, thêm STT
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> formatComparisonTable(List<Map<String, Object>> comparison) {
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (int i = 0; i < comparison.size(); i++) {
            Map<String, Object> original = comparison.get(i);
            Map<String, Object> row = new LinkedHashMap<>();
            
            // 1. STT
            row.put("STT", i + 1);
            
            // 2. Tên sản phẩm (ưu tiên đầu tiên)
            if (original.containsKey("name")) {
                row.put("Tên sản phẩm", original.get("name"));
            } else if (original.containsKey("productName")) {
                row.put("Tên sản phẩm", original.get("productName"));
            }
            
            // 3. Review Count
            if (original.containsKey("reviewCount")) {
                row.put("Review Count", original.get("reviewCount"));
            }
            
            // 4. Giá
            if (original.containsKey("price")) {
                row.put("Giá", original.get("price"));
            }
            
            // 5. Average Rating
            if (original.containsKey("averageRating")) {
                row.put("Average Rating", original.get("averageRating"));
            }
            
            // 6. Key Attributes (đã được format thành string)
            if (original.containsKey("keyAttributes")) {
                row.put("Key Attributes", original.get("keyAttributes"));
            }
            
            // 7. Final Price
            if (original.containsKey("finalPrice")) {
                row.put("Final Price", original.get("finalPrice"));
            }
            
            // 8. Tồn kho
            if (original.containsKey("stockQuantity")) {
                row.put("Tồn kho", original.get("stockQuantity"));
            }
            
            // 9. Sale Off
            if (original.containsKey("saleOff")) {
                row.put("Sale Off", original.get("saleOff"));
            }
            
            // 10. Các cột còn lại (trừ ID và các trường đã xử lý)
            for (Map.Entry<String, Object> entry : original.entrySet()) {
                String key = entry.getKey();
                if (!key.endsWith("Id") && 
                    !key.equals("name") && 
                    !key.equals("productName") &&
                    !key.equals("reviewCount") &&
                    !key.equals("price") &&
                    !key.equals("averageRating") &&
                    !key.equals("keyAttributes") &&
                    !key.equals("finalPrice") &&
                    !key.equals("stockQuantity") &&
                    !key.equals("saleOff") &&
                    !key.equals("description") &&
                    !key.equals("attributes") &&
                    !key.equals("purchaseCount")) {
                    row.put(key, entry.getValue());
                }
            }
            
            result.add(row);
        }
        
        return result;
    }

    @SuppressWarnings("unchecked")
    private String formatFunctionResultAsText(String functionName, Map<String, Object> result) {
        StringBuilder sb = new StringBuilder();

        switch (functionName) {
            case "searchProducts":
                int totalFound = getIntValue(result.get("totalFound"));
                sb.append("🔍 Tôi đã tìm thấy **").append(totalFound).append(" sản phẩm** phù hợp với yêu cầu của bạn.");
                if (totalFound > 0) {
                    sb.append("\n\n");
                    Object productsObj = result.get("products");
                    if (productsObj instanceof List) {
                        appendProductList(sb, (List<Map<String, Object>>) productsObj);
                    }
                }
                break;

            case "getHotProducts":
                sb.append("🔥 **Top sản phẩm đang bán chạy:**\n\n");
                sb.append("Đây là các sản phẩm bán chạy và được quan tâm nhiều nhất hiện nay:\n\n");
                Object hotProductsObj = result.get("products");
                if (hotProductsObj instanceof List) {
                    appendProductList(sb, (List<Map<String, Object>>) hotProductsObj);
                }
                break;

            case "searchByAttribute":
                String sortedBy = result.get("sortedBy") != null ? result.get("sortedBy").toString() : "";
                String sortOrder = result.get("sortOrder") != null ? result.get("sortOrder").toString() : "desc";
                String attributeValue = result.get("attributeValue") != null ? result.get("attributeValue").toString() : null;
                int totalAttrFound = getIntValue(result.get("totalFound"));
                
                // Map attributeKey sang tên hiển thị
                String attrDisplayName = switch(sortedBy) {
                    case "battery" -> "pin";
                    case "ram" -> "RAM";
                    case "screen" -> "màn hình";
                    case "storage" -> "bộ nhớ";
                    case "cpu" -> "CPU";
                    case "vga" -> "card đồ họa";
                    default -> sortedBy;
                };
                
                sb.append("🔋 Tôi đã tìm thấy **").append(totalAttrFound).append(" sản phẩm** ");
                if (attributeValue != null && !attributeValue.isEmpty()) {
                    // Filter theo giá trị cụ thể
                    sb.append("có ").append(attrDisplayName).append(" **").append(attributeValue);
                    // Thêm đơn vị nếu cần
                    if ("ram".equals(sortedBy) || "storage".equals(sortedBy)) {
                        sb.append("GB");
                    } else if ("battery".equals(sortedBy)) {
                        sb.append("mAh");
                    } else if ("screen".equals(sortedBy)) {
                        sb.append(" inch");
                    }
                    sb.append("**");
                } else {
                    // Sort theo giá trị lớn nhất/nhỏ nhất
                    sb.append("với ").append(attrDisplayName);
                    if ("desc".equals(sortOrder)) {
                        sb.append(" **lớn nhất**");
                    } else {
                        sb.append(" **nhỏ nhất**");
                    }
                }
                sb.append(":\n\n");
                
                Object attrProductsObj = result.get("products");
                if (attrProductsObj instanceof List) {
                    appendProductListWithAttributes(sb, (List<Map<String, Object>>) attrProductsObj, sortedBy);
                }
                break;

            case "getSimilarProducts":
                sb.append("✨ **Gợi ý sản phẩm tương tự:**\n\n");
                sb.append("Dựa trên sản phẩm bạn đang quan tâm, tôi gợi ý các lựa chọn sau:\n\n");
                Object similarProductsObj = result.get("products");
                if (similarProductsObj instanceof List) {
                    appendProductList(sb, (List<Map<String, Object>>) similarProductsObj);
                }
                break;

            case "getProductDetails":
                sb.append("📦 **Thông tin chi tiết sản phẩm:**\n\n");
                if (result.containsKey("name")) {
                    sb.append("**").append(result.get("name")).append("**\n\n");
                }
                if (result.containsKey("description")) {
                    sb.append(result.get("description")).append("\n\n");
                }
                if (result.containsKey("price")) {
                    sb.append("💰 **Giá:** ");
                    if (result.containsKey("finalPrice") && result.get("finalPrice") != null) {
                        sb.append("~~").append(formatCurrency((Double) result.get("price"))).append("~~ ");
                        sb.append(formatCurrency((Double) result.get("finalPrice")));
                        if (result.containsKey("saleOff") && result.get("saleOff") != null) {
                            sb.append(" (-").append(result.get("saleOff")).append("%)");
                        }
                    } else {
                        sb.append(formatCurrency((Double) result.get("price")));
                    }
                    sb.append("\n");
                }
                if (result.containsKey("stockQuantity")) {
                    sb.append("📦 **Tồn kho:** ").append(result.get("stockQuantity")).append("\n");
                }
                if (result.containsKey("averageRating")) {
                    sb.append("⭐ **Đánh giá:** ").append(result.get("averageRating")).append("/5");
                    if (result.containsKey("reviewCount")) {
                        sb.append(" (").append(result.get("reviewCount")).append(" đánh giá)");
                    }
                    sb.append("\n");
                }
                if (result.containsKey("attributes")) {
                    sb.append("\n**Thông số kỹ thuật:**\n");
                    List<Map<String, String>> attrs = (List<Map<String, String>>) result.get("attributes");
                    for (Map<String, String> attr : attrs) {
                        sb.append("- ").append(attr.get("name")).append(": ").append(attr.get("value"));
                        if (attr.get("unit") != null && !attr.get("unit").isEmpty()) {
                            sb.append(" ").append(attr.get("unit"));
                        }
                        sb.append("\n");
                    }
                }
                break;

            case "compareProducts":
                sb.append("⚖️ **So sánh sản phẩm:**\n");
                sb.append("Dưới đây là bảng so sánh chi tiết giữa các sản phẩm:");
                break;

            case "getRevenueStats":
                sb.append("📊 **Báo cáo doanh thu:**\n");
                if (result.get("totalRevenue") != null) {
                    sb.append("- 💰 Tổng doanh thu: **").append(formatCurrency((Double) result.get("totalRevenue"))).append("**\n");
                }
                if (result.get("totalOrders") != null) {
                    sb.append("- 📦 Tổng đơn hàng: **").append(result.get("totalOrders")).append("**\n");
                }
                if (result.get("avgOrderValue") != null) {
                    sb.append("- 📈 Giá trị đơn trung bình: **").append(formatCurrency((Double) result.get("avgOrderValue"))).append("**");
                }
                break;

            case "getLowStockProducts":
                int totalCount = getIntValue(result.get("totalCount"));
                int criticalCount = getIntValue(result.get("criticalCount"));
                sb.append("⚠️ **Cảnh báo tồn kho thấp:**\n");
                sb.append("- Tổng sản phẩm cần chú ý: **").append(totalCount).append("**\n");
                sb.append("- Sản phẩm hết hàng: **").append(criticalCount).append("**");
                if (totalCount > 0) {
                    sb.append("\n\nDanh sách chi tiết:");
                }
                break;

            case "suggestCoupons":
                sb.append("💡 **Phân tích và gợi ý chiến dịch khuyến mãi:**\n\n");
                formatCouponSuggestions(sb, result);
                break;

            case "getOrderAnalytics":
                sb.append("📈 **Phân tích chi tiết đơn hàng:**\n\n");
                formatOrderAnalytics(sb, result);
                break;

            case "getSystemRevenue":
                sb.append("💼 **Báo cáo doanh thu toàn hệ thống:**\n\n");
                formatSystemRevenue(sb, result);
                break;

            case "getSegmentAnalytics":
                sb.append("👥 **Phân tích chi tiết phân khúc khách hàng:**\n\n");
                formatSegmentAnalytics(sb, result);
                break;

            case "suggestSegmentAdjustments":
                sb.append("🎯 **Phân tích và gợi ý điều chỉnh phân khúc khách hàng:**\n\n");
                formatSegmentAdjustments(sb, result);
                break;

            case "getSystemOverview":
                sb.append("🏢 **Tổng quan hệ thống:**\n\n");
                formatSystemOverview(sb, result);
                break;

            case "suggestVendorBusinessStrategy":
                sb.append("🎯 **Chiến lược kinh doanh toàn diện cho shop của bạn:**\n\n");
                formatVendorBusinessStrategy(sb, result);
                break;

            case "suggestAdminBusinessStrategy":
                sb.append("🚀 **Chiến lược phát triển toàn hệ thống:**\n\n");
                formatAdminBusinessStrategy(sb, result);
                break;

            case "getPersonalizedRecommendations":
                sb.append("🎁 **Gợi ý dành riêng cho bạn:**\n\n");
                sb.append("Dựa trên lịch sử mua sắm và sở thích của bạn, tôi nghĩ bạn sẽ thích những sản phẩm này:\n\n");
                Object personalizedProductsObj = result.get("products");
                if (personalizedProductsObj instanceof List) {
                    appendProductList(sb, (List<Map<String, Object>>) personalizedProductsObj);
                }
                break;

            case "getTopSellingProducts":
                sb.append("🏆 **Top sản phẩm bán chạy:**\n\n");
                Object topProductsObj = result.get("products");
                if (topProductsObj instanceof List) {
                    List<Map<String, Object>> topProducts = (List<Map<String, Object>>) topProductsObj;
                    if (topProducts != null && !topProducts.isEmpty()) {
                        for (int i = 0; i < topProducts.size(); i++) {
                            Map<String, Object> p = topProducts.get(i);
                            sb.append((i + 1)).append(". **").append(p.get("name")).append("**\n");
                            if (p.get("price") != null) {
                                sb.append("   - Giá: ").append(formatCurrency((Double) p.get("price"))).append("\n");
                            }
                            if (p.get("purchaseCount") != null) {
                                sb.append("   - Đã bán: ").append(p.get("purchaseCount")).append(" sản phẩm\n");
                            }
                            if (p.get("revenue") != null) {
                                sb.append("   - Doanh thu: ").append(formatCurrency((Double) p.get("revenue"))).append("\n");
                            }
                            sb.append("\n");
                        }
                    } else {
                        sb.append("Chưa có sản phẩm nào được bán.");
                    }
                } else {
                    sb.append("Chưa có sản phẩm nào được bán.");
                }
                break;

            default:
                sb.append("✅ Đã xử lý yêu cầu thành công.");
        }

        return sb.toString();
    }

    private String formatCurrency(Double amount) {
        if (amount == null) return "0đ";
        return String.format("%,.0fđ", amount);
    }

    /**
     * Helper method để format danh sách sản phẩm thành text
     */
    @SuppressWarnings("unchecked")
    private void appendProductList(StringBuilder sb, List<Map<String, Object>> products) {
        if (products == null || products.isEmpty()) {
            sb.append("Không tìm thấy sản phẩm nào phù hợp.");
            return;
        }

        for (int i = 0; i < products.size(); i++) {
            Map<String, Object> p = products.get(i);
            sb.append((i + 1)).append(". **").append(p.get("name")).append("**\n");
            
            // Giá
            if (p.get("finalPrice") != null) {
                Double finalPrice = (Double) p.get("finalPrice");
                Double originalPrice = (Double) p.get("price");
                
                if (originalPrice != null && !originalPrice.equals(finalPrice)) {
                    sb.append("   - Giá: ~~").append(formatCurrency(originalPrice)).append("~~ **")
                      .append(formatCurrency(finalPrice)).append("**");
                    if (p.get("saleOff") != null) {
                        sb.append(" (-").append(p.get("saleOff")).append("%)");
                    }
                    sb.append("\n");
                } else {
                    sb.append("   - Giá: **").append(formatCurrency(finalPrice)).append("**\n");
                }
            } else if (p.get("price") != null) {
                sb.append("   - Giá: **").append(formatCurrency((Double) p.get("price"))).append("**\n");
            }
            
            // Rating
            if (p.get("averageRating") != null) {
                sb.append("   - Đánh giá: ⭐ ").append(p.get("averageRating")).append("/5");
                if (p.get("reviewCount") != null) {
                    sb.append(" (").append(p.get("reviewCount")).append(" đánh giá)");
                }
                sb.append("\n");
            }
            
            // Stock
            if (p.get("stockQuantity") != null) {
                Integer stock = (Integer) p.get("stockQuantity");
                sb.append("   - Tồn kho: ").append(stock);
                if (stock < 10) {
                    sb.append(" ⚠️ Sắp hết hàng");
                }
                sb.append("\n");
            }
            
            sb.append("\n");
        }
    }

    /**
     * Helper method để format danh sách sản phẩm với attribute nổi bật
     */
    @SuppressWarnings("unchecked")
    private void appendProductListWithAttributes(StringBuilder sb, List<Map<String, Object>> products, String highlightAttr) {
        if (products == null || products.isEmpty()) {
            sb.append("Không tìm thấy sản phẩm nào phù hợp.");
            return;
        }

        for (int i = 0; i < products.size(); i++) {
            Map<String, Object> p = products.get(i);
            sb.append((i + 1)).append(". **").append(p.get("name")).append("**\n");
            
            // Hiển thị attribute được highlight (now keyAttributes is a String)
            if (p.get("keyAttributes") != null && !p.get("keyAttributes").toString().equals("N/A")) {
                String keyAttrsStr = p.get("keyAttributes").toString();
                sb.append("   - ").append(keyAttrsStr).append("\n");
            }
            
            // Giá
            if (p.get("finalPrice") != null) {
                Double finalPrice = (Double) p.get("finalPrice");
                Double originalPrice = (Double) p.get("price");
                
                if (originalPrice != null && !originalPrice.equals(finalPrice)) {
                    sb.append("   - Giá: ~~").append(formatCurrency(originalPrice)).append("~~ **")
                      .append(formatCurrency(finalPrice)).append("**");
                    if (p.get("saleOff") != null) {
                        sb.append(" (-").append(p.get("saleOff")).append("%)");
                    }
                    sb.append("\n");
                } else {
                    sb.append("   - Giá: **").append(formatCurrency(finalPrice)).append("**\n");
                }
            } else if (p.get("price") != null) {
                sb.append("   - Giá: **").append(formatCurrency((Double) p.get("price"))).append("**\n");
            }
            
            // Rating
            if (p.get("averageRating") != null) {
                sb.append("   - Đánh giá: ⭐ ").append(p.get("averageRating")).append("/5");
                if (p.get("reviewCount") != null) {
                    sb.append(" (").append(p.get("reviewCount")).append(" đánh giá)");
                }
                sb.append("\n");
            }
            
            sb.append("\n");
        }
    }

    private AIChatResponse buildResponse(AIConversation conversation, AIMessage message,
                                         String responseText, AIMetadata metadata) {
        AIResponseType responseType = AIResponseType.TEXT;

        if (metadata != null) {
            if (metadata.getProductIds() != null && !metadata.getProductIds().isEmpty()) {
                responseType = AIResponseType.PRODUCT_LIST;
            } else if (metadata.getChartData() != null) {
                responseType = AIResponseType.CHART;
            } else if (metadata.getTableData() != null && !metadata.getTableData().isEmpty()) {
                responseType = AIResponseType.TABLE;
            } else if (metadata.getCouponSuggestions() != null && !metadata.getCouponSuggestions().isEmpty()) {
                responseType = AIResponseType.COUPON;
            }
        }

        return AIChatResponse.builder()
            .message(responseText)
            .conversationId(conversation.getId())
            .metadata(metadata)
            .responseType(responseType)
            .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
            .build();
    }

    // ==================== INTELLIGENT FORMATTING METHODS ====================

    /**
     * Format phân tích và gợi ý điều chỉnh phân khúc khách hàng - intelligent response
     */
    @SuppressWarnings("unchecked")
    private void formatSegmentAdjustments(StringBuilder sb, Map<String, Object> result) {
        // 1. Tổng quan dữ liệu
        Integer totalUsers = (Integer) result.get("totalUsers");
        int userCount = totalUsers != null ? totalUsers : 0;
        
        sb.append("📊 **Tổng quan:**\n");
        sb.append("- Tổng số khách hàng trong hệ thống: **").append(userCount).append(" users**\n");
        
        if (userCount == 0) {
            sb.append("\n⚠️ **Hệ thống chưa có khách hàng nào.**\n\n");
            sb.append("💡 **Gợi ý:**\n");
            sb.append("- Chưa thể phân tích phân khúc khi chưa có dữ liệu khách hàng\n");
            sb.append("- Tập trung vào việc thu hút người dùng đăng ký trước\n");
            sb.append("- Sau khi có đủ khách hàng (ít nhất 20-30), hãy quay lại phân tích để tối ưu phân khúc\n");
            return;
        } else if (userCount < 10) {
            sb.append("⚠️ *Lưu ý: Chỉ có ").append(userCount).append(" khách hàng - dữ liệu có thể chưa đủ để phân tích chính xác*\n");
        }
        sb.append("\n");

        // 2. Phân tích phân phối chi tiêu
        if (result.containsKey("spendDistribution")) {
            Map<String, Object> dist = (Map<String, Object>) result.get("spendDistribution");
            sb.append("💰 **Phân phối chi tiêu khách hàng:**\n");
            sb.append("- Chi tiêu thấp nhất: ").append(formatCurrency(getDoubleValue(dist.get("min")))).append("\n");
            sb.append("- Chi tiêu cao nhất: ").append(formatCurrency(getDoubleValue(dist.get("max")))).append("\n");
            sb.append("- Trung vị (Median): ").append(formatCurrency(getDoubleValue(dist.get("median")))).append("\n");
            sb.append("- Phân vị 25% (Q1): ").append(formatCurrency(getDoubleValue(dist.get("q1")))).append("\n");
            sb.append("- Phân vị 75% (Q3): ").append(formatCurrency(getDoubleValue(dist.get("q3")))).append("\n\n");

            // Phân tích thông minh về phân phối
            Double max = getDoubleValue(dist.get("max"));
            Double median = getDoubleValue(dist.get("median"));
            if (max > 0 && median == 0) {
                sb.append("⚠️ **Nhận xét:** Đa số khách hàng chưa có chi tiêu, nhưng có một số khách hàng VIP đã chi tiêu cao. ");
                sb.append("Cần thiết kế phân khúc phù hợp hơn với thực tế.\n\n");
            } else if (max > median * 10 && median > 0) {
                sb.append("📈 **Nhận xét:** Phân phối chi tiêu có độ lệch cao, một số ít khách hàng đóng góp phần lớn doanh thu. ");
                sb.append("Nên tập trung chăm sóc nhóm VIP này.\n\n");
            }
        }

        // 3. Các vấn đề và gợi ý cụ thể
        List<Map<String, Object>> suggestions = (List<Map<String, Object>>) result.get("suggestions");
        if (suggestions != null && !suggestions.isEmpty()) {
            sb.append("🔍 **Phân tích chi tiết từng phân khúc:**\n\n");

            int criticalIssues = 0;
            int warningIssues = 0;

            for (int i = 0; i < suggestions.size(); i++) {
                Map<String, Object> s = suggestions.get(i);
                String segmentName = (String) s.get("segmentName");
                String issue = (String) s.get("issue");
                String suggestion = (String) s.get("suggestion");

                // Phân loại mức độ nghiêm trọng
                boolean isCritical = issue != null && issue.contains("0 users");
                if (isCritical) criticalIssues++;
                else warningIssues++;

                String icon = isCritical ? "🔴" : "🟡";
                sb.append(icon).append(" **").append((i + 1)).append(". ").append(segmentName).append("**\n");
                sb.append("   - **Vấn đề:** ").append(issue != null ? issue : "Không xác định").append("\n");
                sb.append("   - **Khuyến nghị:** ").append(suggestion != null ? suggestion : "Cần xem xét lại").append("\n\n");
            }

            // 4. Tổng kết và đề xuất hành động
            sb.append("---\n");
            sb.append("📋 **Tổng kết & Đề xuất hành động:**\n\n");

            if (criticalIssues > 0) {
                sb.append("⚠️ Có **").append(criticalIssues).append(" phân khúc** đang không có khách hàng nào - cần điều chỉnh ngay:\n");
                sb.append("   1. Xem xét hạ ngưỡng `minSpend` để phân khúc bao phủ được nhiều khách hàng hơn\n");
                sb.append("   2. Hoặc gộp các phân khúc nhỏ lại với nhau\n\n");
            }

            if (warningIssues > 0) {
                sb.append("💡 Có **").append(warningIssues).append(" phân khúc** có ít khách hàng:\n");
                sb.append("   1. Tổ chức các chương trình khuyến mãi để thu hút khách hàng chi tiêu nhiều hơn\n");
                sb.append("   2. Xem xét điều chỉnh tiêu chí phân khúc phù hợp với hành vi thực tế\n\n");
            }

            // Gợi ý cụ thể dựa trên dữ liệu
            if (result.containsKey("spendDistribution")) {
                Map<String, Object> dist = (Map<String, Object>) result.get("spendDistribution");
                Double q3 = getDoubleValue(dist.get("q3"));
                Double max = getDoubleValue(dist.get("max"));

                if (max > 0) {
                    sb.append("💎 **Gợi ý ngưỡng phân khúc dựa trên dữ liệu thực tế:**\n");
                    sb.append("   - Bronze: Chi tiêu dưới ").append(formatCurrency(q3 > 0 ? q3 * 0.5 : max * 0.1)).append("\n");
                    sb.append("   - Silver: Chi tiêu từ ").append(formatCurrency(q3 > 0 ? q3 * 0.5 : max * 0.1))
                      .append(" - ").append(formatCurrency(q3 > 0 ? q3 : max * 0.3)).append("\n");
                    sb.append("   - Gold: Chi tiêu từ ").append(formatCurrency(q3 > 0 ? q3 : max * 0.3))
                      .append(" - ").append(formatCurrency(max * 0.7)).append("\n");
                    sb.append("   - Diamond: Chi tiêu trên ").append(formatCurrency(max * 0.7)).append("\n");
                }
            }
        } else {
            sb.append("✅ **Tất cả phân khúc đang hoạt động tốt.** Không có vấn đề cần điều chỉnh.\n");
        }
    }

    /**
     * Format phân tích phân khúc khách hàng chi tiết
     */
    @SuppressWarnings("unchecked")
    private void formatSegmentAnalytics(StringBuilder sb, Map<String, Object> result) {
        Object segmentsObj = result.get("segments");
        List<Map<String, Object>> segments = null;
        if (segmentsObj instanceof List) {
            segments = (List<Map<String, Object>>) segmentsObj;
        }

        if (segments == null || segments.isEmpty()) {
            sb.append("📭 Chưa có phân khúc khách hàng nào được thiết lập.\n");
            sb.append("\n💡 **Gợi ý:** Hãy tạo các phân khúc khách hàng (Bronze, Silver, Gold, Diamond) để quản lý và marketing hiệu quả hơn.");
            return;
        }

        // Tính tổng
        int totalCustomers = 0;
        double totalRevenue = 0;
        for (Map<String, Object> seg : segments) {
            // AIToolsService trả về "userCount" chứ không phải "customerCount"
            totalCustomers += getIntValue(seg.get("userCount"));
            // AIToolsService trả về "totalSpend" chứ không phải "totalRevenue"
            totalRevenue += getDoubleValue(seg.get("totalSpend"));
        }

        sb.append("📊 **Tổng quan các phân khúc:**\n");
        sb.append("- Tổng số khách hàng: **").append(totalCustomers).append(" người**\n");
        sb.append("- Tổng doanh thu từ các phân khúc: **").append(formatCurrency(totalRevenue)).append("**\n\n");

        sb.append("📋 **Chi tiết từng phân khúc:**\n\n");

        // Sắp xếp theo doanh thu giảm dần
        segments.sort((a, b) -> Double.compare(getDoubleValue(b.get("totalSpend")), getDoubleValue(a.get("totalSpend"))));

        for (int i = 0; i < segments.size(); i++) {
            Map<String, Object> seg = segments.get(i);
            String name = (String) seg.get("segmentName");
            int count = getIntValue(seg.get("userCount"));
            double revenue = getDoubleValue(seg.get("totalSpend"));
            double avgSpend = count > 0 ? revenue / count : 0;
            double revenuePercent = totalRevenue > 0 ? (revenue / totalRevenue * 100) : 0;

            String icon = getSegmentIcon(name);
            sb.append(icon).append(" **").append((i + 1)).append(". ").append(name).append("**\n");
            sb.append("   - Số khách hàng: ").append(count).append(" (")
              .append(String.format("%.1f", totalCustomers > 0 ? (double) count / totalCustomers * 100 : 0)).append("%)\n");
            sb.append("   - Doanh thu: ").append(formatCurrency(revenue)).append(" (")
              .append(String.format("%.1f", revenuePercent)).append("% tổng doanh thu)\n");
            sb.append("   - Chi tiêu TB/khách: ").append(formatCurrency(avgSpend)).append("\n");

            // Nhận xét về hiệu quả
            if (count == 0) {
                sb.append("   - ⚠️ *Phân khúc trống - cần xem xét điều chỉnh tiêu chí*\n");
            } else if (revenuePercent > 50) {
                sb.append("   - ⭐ *Phân khúc VIP - đóng góp chính cho doanh thu*\n");
            } else if (count > totalCustomers * 0.5 && revenuePercent < 20) {
                sb.append("   - 💡 *Nhiều khách hàng nhưng chi tiêu thấp - tiềm năng upsell*\n");
            }
            sb.append("\n");
        }

        // Insight và đề xuất
        sb.append("---\n");
        sb.append("💡 **Insights & Đề xuất chiến lược:**\n\n");

        // Tìm phân khúc tốt nhất và kém nhất
        if (!segments.isEmpty()) {
            Map<String, Object> topSegment = segments.get(0);
            Map<String, Object> bottomSegment = segments.get(segments.size() - 1);

            sb.append("📈 Phân khúc **").append(topSegment.get("segmentName")).append("** đang hoạt động tốt nhất với ")
              .append(formatCurrency(getDoubleValue(topSegment.get("totalSpend")))).append(" doanh thu.\n");
            sb.append("   → Tiếp tục chăm sóc và duy trì loyalty program cho nhóm này.\n\n");

            if (getDoubleValue(bottomSegment.get("totalSpend")) == 0) {
                sb.append("⚠️ Phân khúc **").append(bottomSegment.get("segmentName")).append("** chưa có doanh thu.\n");
                sb.append("   → Cần xem xét lại tiêu chí phân khúc hoặc tạo chiến dịch kích cầu.\n");
            }
        }
    }

    /**
     * Format tổng quan hệ thống
     */
    @SuppressWarnings("unchecked")
    private void formatSystemOverview(StringBuilder sb, Map<String, Object> result) {
        // Đếm số field có dữ liệu
        int todayDataCount = 0;
        if (result.containsKey("todayRevenue")) todayDataCount++;
        if (result.containsKey("todayOrders")) todayDataCount++;
        if (result.containsKey("newUsers")) todayDataCount++;
        
        // Số liệu hôm nay
        sb.append("📈 **Số liệu hôm nay:**\n");
        boolean hasTodayData = false;
        
        if (result.containsKey("todayRevenue")) {
            double revenue = getDoubleValue(result.get("todayRevenue"));
            sb.append("- Doanh thu hôm nay: **").append(formatCurrency(revenue)).append("**\n");
            hasTodayData = true;
        }
        if (result.containsKey("todayOrders")) {
            int orders = getIntValue(result.get("todayOrders"));
            sb.append("- Đơn hàng mới: **").append(orders).append(" đơn**\n");
            hasTodayData = true;
        }
        if (result.containsKey("newUsers")) {
            int users = getIntValue(result.get("newUsers"));
            sb.append("- Khách hàng mới: **").append(users).append(" người**\n");
            hasTodayData = true;
        }
        
        if (!hasTodayData || todayDataCount < 2) {
            sb.append("*Dữ liệu hôm nay chưa đầy đủ - hệ thống đang thu thập thông tin.*\n");
        }
        sb.append("\n");

        // Tổng quan toàn bộ
        sb.append("🏪 **Thống kê toàn hệ thống:**\n");
        int totalProducts = getIntValue(result.get("totalProducts"));
        int totalUsers = getIntValue(result.get("totalUsers"));
        int totalOrders = getIntValue(result.get("totalOrders"));
        int totalShops = getIntValue(result.get("totalShops"));
        double totalRevenue = getDoubleValue(result.get("totalRevenue"));
        
        if (totalShops > 0) {
            sb.append("- Tổng số cửa hàng: **").append(totalShops).append("**\n");
        }
        if (totalProducts > 0) {
            sb.append("- Tổng số sản phẩm: **").append(totalProducts).append("**\n");
        } else {
            sb.append("- Tổng số sản phẩm: **0** ⚠️ *Chưa có sản phẩm nào được thêm vào hệ thống*\n");
        }
        
        if (totalUsers > 0) {
            sb.append("- Tổng khách hàng: **").append(totalUsers).append(" người**\n");
        } else {
            sb.append("- Tổng khách hàng: **0** ⚠️ *Hệ thống mới, chưa có người dùng*\n");
        }
        
        if (totalOrders > 0) {
            sb.append("- Tổng đơn hàng: **").append(totalOrders).append("**\n");
        } else {
            sb.append("- Tổng đơn hàng: **0** ⚠️ *Chưa có đơn hàng nào được tạo*\n");
        }
        
        if (totalRevenue > 0) {
            sb.append("- Tổng doanh thu: **").append(formatCurrency(totalRevenue)).append("**\n");
        } else if (totalOrders == 0) {
            sb.append("- Tổng doanh thu: **0đ** *(Chưa có đơn hàng hoàn thành)*\n");
        }
        sb.append("\n");

        // So sánh với hôm qua nếu có
        if (result.containsKey("yesterdayRevenue") && result.containsKey("todayRevenue")) {
            double today = getDoubleValue(result.get("todayRevenue"));
            double yesterday = getDoubleValue(result.get("yesterdayRevenue"));
            if (yesterday > 0) {
                double changePercent = (today - yesterday) / yesterday * 100;
                String trend = changePercent >= 0 ? "📈 +" : "📉 ";
                sb.append("**So với hôm qua:** ").append(trend).append(String.format("%.1f", Math.abs(changePercent))).append("%\n");
                
                // Phân tích insight
                if (changePercent > 20) {
                    sb.append("   ⭐ *Tuyệt vời! Doanh thu tăng trưởng mạnh.*\n");
                } else if (changePercent > 0) {
                    sb.append("   ✅ *Tốt! Doanh thu đang tăng.*\n");
                } else if (changePercent < -20) {
                    sb.append("   ⚠️ *Cảnh báo! Doanh thu giảm đáng kể - cần xem xét nguyên nhân.*\n");
                } else if (changePercent < 0) {
                    sb.append("   💡 *Doanh thu giảm nhẹ - theo dõi thêm.*\n");
                }
                sb.append("\n");
            }
        }

        // Trạng thái đơn hàng
        if (result.containsKey("pendingOrders") || result.containsKey("processingOrders")) {
            sb.append("📦 **Đơn hàng cần xử lý:**\n");
            if (result.containsKey("pendingOrders")) {
                int pending = getIntValue(result.get("pendingOrders"));
                sb.append("- Chờ xác nhận: **").append(pending).append("**");
                if (pending > 20) {
                    sb.append(" ⚠️⚠️ *Số lượng đơn chờ rất cao!*");
                } else if (pending > 10) {
                    sb.append(" ⚠️ *Cần xử lý nhanh*");
                } else if (pending > 0) {
                    sb.append(" ✅");
                } else {
                    sb.append(" *Tuyệt vời! Không có đơn chờ*");
                }
                sb.append("\n");
            }
            if (result.containsKey("processingOrders")) {
                int processing = getIntValue(result.get("processingOrders"));
                sb.append("- Đang xử lý: **").append(processing).append("**");
                if (processing > 0) sb.append(" 🔄");
                sb.append("\n");
            }
            sb.append("\n");
        }

        // Insight thông minh
        sb.append("---\n");
        sb.append("💡 **Phân tích & Đề xuất:**\n\n");
        
        // Phân tích tổng thể
        if (totalProducts == 0 && totalOrders == 0 && totalUsers == 0) {
            sb.append("🆕 **Hệ thống mới hoặc đang trong giai đoạn khởi tạo:**\n");
            sb.append("- Bước 1: Thêm sản phẩm vào hệ thống\n");
            sb.append("- Bước 2: Mời người dùng tham gia\n");
            sb.append("- Bước 3: Khởi động chiến dịch marketing để thu hút khách hàng\n");
        } else if (totalProducts > 0 && totalUsers > 0 && totalOrders == 0) {
            sb.append("📢 **Đã có sản phẩm và người dùng nhưng chưa có đơn hàng:**\n");
            sb.append("- Xem xét giảm giá hoặc tạo chương trình khuyến mãi đầu tiên\n");
            sb.append("- Kiểm tra trải nghiệm người dùng và quy trình đặt hàng\n");
            sb.append("- Tạo urgency với countdown deals hoặc limited stock\n");
        } else if (totalOrders > 0) {
            sb.append("✅ **Hệ thống đang hoạt động:**\n");
            
            if (result.containsKey("todayRevenue")) {
                double todayRev = getDoubleValue(result.get("todayRevenue"));
                if (todayRev > 0) {
                    sb.append("- Hôm nay đã có doanh thu ").append(formatCurrency(todayRev)).append("\n");
                } else {
                    sb.append("- Hôm nay chưa có đơn hàng mới - có thể do cuối tuần hoặc cần kích hoạt marketing\n");
                }
            }
            
            if (result.containsKey("pendingOrders")) {
                int pending = getIntValue(result.get("pendingOrders"));
                if (pending > 10) {
                    sb.append("- **Ưu tiên:** Xử lý ").append(pending).append(" đơn hàng đang chờ để tránh mất khách\n");
                }
            }
            
            // Tính conversion rate nếu có đủ dữ liệu
            if (totalUsers > 0 && totalOrders > 0) {
                double conversionRate = (double) totalOrders / totalUsers * 100;
                sb.append("- Tỷ lệ chuyển đổi: ~").append(String.format("%.1f", conversionRate)).append("%");
                if (conversionRate < 5) {
                    sb.append(" ⚠️ *Thấp - cần cải thiện UX và marketing*");
                } else if (conversionRate > 20) {
                    sb.append(" ⭐ *Tuyệt vời!*");
                }
                sb.append("\n");
            }
            
            // Phân tích giá trị đơn hàng trung bình
            if (totalOrders > 0 && totalRevenue > 0) {
                double avgOrderValue = totalRevenue / totalOrders;
                sb.append("- Giá trị đơn TB: ").append(formatCurrency(avgOrderValue));
                if (avgOrderValue < 300000) {
                    sb.append(" 💡 *Có thể tăng qua bundle deals*");
                } else if (avgOrderValue > 1000000) {
                    sb.append(" 🎯 *Cao - khách hàng có sức mua tốt*");
                }
                sb.append("\n");
            }
        }
        
        // Gợi ý cụ thể cho từng tình huống
        if (totalProducts < 10 && totalProducts > 0) {
            sb.append("\n📦 **Lưu ý:** Chỉ có ").append(totalProducts).append(" sản phẩm - nên mở rộng danh mục để tăng lựa chọn cho khách hàng.\n");
        }
    }

    /**
     * Format phân tích đơn hàng
     */
    @SuppressWarnings("unchecked")
    private void formatOrderAnalytics(StringBuilder sb, Map<String, Object> result) {
        // Tổng quan
        int totalOrders = getIntValue(result.get("totalOrders"));
        double totalRevenue = getDoubleValue(result.get("totalRevenue"));
        double avgOrderValue = getDoubleValue(result.get("avgOrderValue"));
        
        sb.append("📊 **Tổng quan đơn hàng:**\n");
        
        if (totalOrders == 0) {
            sb.append("⚠️ **Chưa có đơn hàng nào trong khoảng thời gian này.**\n\n");
            sb.append("💡 **Gợi ý:**\n");
            sb.append("- Kiểm tra lại khoảng thời gian phân tích\n");
            sb.append("- Nếu là hệ thống mới, hãy tập trung vào marketing để thu hút đơn hàng đầu tiên\n");
            sb.append("- Xem xét tạo chương trình khuyến mãi hoặc giảm giá để kích hoạt mua hàng\n");
            return;
        }
        
        sb.append("- Tổng số đơn: **").append(totalOrders).append("**");
        if (totalOrders < 5) {
            sb.append(" ⚠️ *Số lượng đơn còn ít*");
        } else if (totalOrders > 50) {
            sb.append(" ⭐ *Hoạt động tốt!*");
        }
        sb.append("\n");
        
        if (totalRevenue > 0) {
            sb.append("- Tổng doanh thu: **").append(formatCurrency(totalRevenue)).append("**\n");
        }
        
        if (avgOrderValue > 0) {
            sb.append("- Giá trị đơn trung bình: **").append(formatCurrency(avgOrderValue)).append("**");
            if (avgOrderValue < 200000) {
                sb.append(" 💡 *Thấp - có thể tăng bằng upsell/cross-sell*");
            } else if (avgOrderValue > 1000000) {
                sb.append(" ⭐ *Rất tốt!*");
            }
            sb.append("\n");
        }
        sb.append("\n");

        // Phân loại theo trạng thái
        if (result.containsKey("ordersByStatus")) {
            Map<String, Integer> statusMap = (Map<String, Integer>) result.get("ordersByStatus");
            sb.append("📋 **Phân loại theo trạng thái:**\n");
            for (Map.Entry<String, Integer> entry : statusMap.entrySet()) {
                String status = translateOrderStatus(entry.getKey());
                String icon = getStatusIcon(entry.getKey());
                sb.append("- ").append(icon).append(" ").append(status).append(": **").append(entry.getValue()).append("**\n");
            }
            sb.append("\n");
        }

        // Tỷ lệ hoàn thành
        if (result.containsKey("completionRate")) {
            Double rate = getDoubleValue(result.get("completionRate"));
            sb.append("✅ **Tỷ lệ đơn thành công:** ").append(String.format("%.1f", rate)).append("%\n");
            if (rate < 80) {
                sb.append("   ⚠️ *Tỷ lệ thấp - cần xem xét nguyên nhân hủy đơn*\n");
            }
            sb.append("\n");
        }

        // Doanh thu theo ngày nếu có
        if (result.containsKey("revenueByDay")) {
            Object revenueByDayObj = result.get("revenueByDay");
            if (revenueByDayObj instanceof List) {
                List<Map<String, Object>> dailyData = (List<Map<String, Object>>) revenueByDayObj;
                if (dailyData != null && !dailyData.isEmpty()) {
                    sb.append("📈 **Xu hướng doanh thu:**\n");
                    double maxRevenue = 0;
                    String peakDay = "";
                    for (Map<String, Object> day : dailyData) {
                        double revenue = getDoubleValue(day.get("revenue"));
                        if (revenue > maxRevenue) {
                            maxRevenue = revenue;
                            peakDay = (String) day.get("date");
                        }
                    }
                    if (!peakDay.isEmpty()) {
                        sb.append("- Ngày cao điểm: **").append(peakDay).append("** (")
                          .append(formatCurrency(maxRevenue)).append(")\n");
                    }
                    sb.append("\n");
                }
            }
        }

        // Gợi ý
        sb.append("---\n");
        sb.append("💡 **Đề xuất:**\n");
        if (result.containsKey("cancelledOrders") && getIntValue(result.get("cancelledOrders")) > 0) {
            sb.append("- Có ").append(result.get("cancelledOrders")).append(" đơn bị hủy - nên phân tích nguyên nhân\n");
        }
        if (result.containsKey("avgOrderValue")) {
            sb.append("- Giá trị đơn TB là ").append(formatCurrency(getDoubleValue(result.get("avgOrderValue"))))
              .append(" - có thể tăng bằng combo deals hoặc free shipping threshold\n");
        }
    }

    /**
     * Format gợi ý coupon
     */
    @SuppressWarnings("unchecked")
    private void formatCouponSuggestions(StringBuilder sb, Map<String, Object> result) {
        Object suggestionsObj = result.get("suggestions");
        List<Map<String, Object>> suggestions = null;
        if (suggestionsObj instanceof List) {
            suggestions = (List<Map<String, Object>>) suggestionsObj;
        }

        if (suggestions == null || suggestions.isEmpty()) {
            sb.append("📭 **Hiện tại không có gợi ý coupon tự động.**\n\n");
            sb.append("💡 **Phân tích tình huống:**\n\n");
            
            // Kiểm tra xem có data nào không
            Object productsObj = result.get("totalProducts");
            int totalProducts = productsObj != null ? getIntValue(productsObj) : 0;
            
            if (totalProducts == 0) {
                sb.append("⚠️ **Chưa có sản phẩm nào trong shop:**\n");
                sb.append("- Thêm sản phẩm trước khi tạo coupon\n");
                sb.append("- Đảm bảo có đủ thông tin về giá, tồn kho\n");
            } else {
                sb.append("✅ **Các sản phẩm hiện tại đang hoạt động tốt:**\n");
                sb.append("- Sản phẩm bán chạy không cần giảm giá\n");
                sb.append("- Tồn kho đang ở mức hợp lý\n");
                sb.append("- Có thể tự tạo coupon cho sản phẩm mới hoặc theo chiến dịch marketing\n");
            }
            
            sb.append("\n📌 **Gợi ý tạo coupon thủ công:**\n");
            sb.append("- Coupon chào mừng khách hàng mới (10-15%)\n");
            sb.append("- Flash sale cuối tuần\n");
            sb.append("- Combo deals cho 2+ sản phẩm\n");
            sb.append("- Freeship cho đơn từ 500k\n");
            return;
        }

        sb.append("Dựa trên phân tích dữ liệu bán hàng, tôi đề xuất các chiến dịch sau:\n\n");

        for (int i = 0; i < suggestions.size(); i++) {
            Map<String, Object> s = suggestions.get(i);
            sb.append("**").append((i + 1)).append(". ").append(s.get("productName")).append("**\n");
            if (s.containsKey("reason")) {
                sb.append("   - 📌 Lý do: ").append(s.get("reason")).append("\n");
            }
            if (s.containsKey("suggestedDiscount")) {
                sb.append("   - 🏷️ Mức giảm đề xuất: **").append(s.get("suggestedDiscount")).append("%**\n");
            }
            if (s.containsKey("expectedImpact")) {
                sb.append("   - 📈 Tác động dự kiến: ").append(s.get("expectedImpact")).append("\n");
            }
            if (s.containsKey("currentStock")) {
                sb.append("   - 📦 Tồn kho hiện tại: ").append(s.get("currentStock")).append("\n");
            }
            sb.append("\n");
        }

        // Gợi ý chung
        sb.append("---\n");
        sb.append("💡 **Lưu ý khi tạo coupon:**\n");
        sb.append("- Đặt thời hạn hợp lý (7-14 ngày) để tạo urgency\n");
        sb.append("- Giới hạn số lượng sử dụng để kiểm soát chi phí\n");
        sb.append("- Theo dõi hiệu quả và điều chỉnh kịp thời\n");
    }

    /**
     * Format báo cáo doanh thu hệ thống
     */
    @SuppressWarnings("unchecked")
    private void formatSystemRevenue(StringBuilder sb, Map<String, Object> result) {
        // Tổng quan
        double totalRevenue = getDoubleValue(result.get("totalRevenue"));
        int totalOrders = getIntValue(result.get("totalOrders"));
        double avgOrderValue = getDoubleValue(result.get("avgOrderValue"));
        
        sb.append("💰 **Báo cáo doanh thu toàn hệ thống:**\n");
        
        if (totalRevenue == 0 && totalOrders == 0) {
            sb.append("\n⚠️ **Chưa có doanh thu trong khoảng thời gian này.**\n\n");
            sb.append("💡 **Phân tích:**\n");
            sb.append("- Có thể là giai đoạn khởi đầu hoặc thời gian phân tích chưa có đơn hàng hoàn thành\n");
            sb.append("- Kiểm tra xem có đơn hàng đang chờ xử lý không\n");
            sb.append("- Xem xét các chiến dịch marketing để kích hoạt mua sắm\n");
            return;
        }
        
        sb.append("- Tổng doanh thu: **").append(formatCurrency(totalRevenue)).append("**");
        if (totalRevenue < 10000000) {
            sb.append(" 💡 *Còn thấp - tiềm năng tăng trưởng lớn*");
        } else if (totalRevenue > 100000000) {
            sb.append(" ⭐ *Xuất sắc!*");
        }
        sb.append("\n");
        
        sb.append("- Tổng đơn hàng: **").append(totalOrders).append("**\n");
        
        if (avgOrderValue > 0) {
            sb.append("- Giá trị đơn trung bình: **").append(formatCurrency(avgOrderValue)).append("**\n");
        }
        sb.append("\n");

        // Doanh thu theo shop nếu có
        if (result.containsKey("revenueByShop")) {
            Object revenueByShopObj = result.get("revenueByShop");
            if (revenueByShopObj instanceof List) {
                List<Map<String, Object>> shopData = (List<Map<String, Object>>) revenueByShopObj;
                if (shopData != null && !shopData.isEmpty()) {
                    sb.append("🏪 **Top cửa hàng theo doanh thu:**\n");
                    int count = Math.min(5, shopData.size());
                    for (int i = 0; i < count; i++) {
                        Map<String, Object> shop = shopData.get(i);
                        sb.append((i + 1)).append(". ").append(shop.get("shopName")).append(": ")
                          .append(formatCurrency(getDoubleValue(shop.get("revenue")))).append("\n");
                    }
                    sb.append("\n");
                }
            }
        }

        // Doanh thu theo danh mục nếu có
        if (result.containsKey("revenueByCategory")) {
            Object revenueByCategoryObj = result.get("revenueByCategory");
            if (revenueByCategoryObj instanceof List) {
                List<Map<String, Object>> catData = (List<Map<String, Object>>) revenueByCategoryObj;
                if (catData != null && !catData.isEmpty()) {
                    sb.append("📁 **Doanh thu theo danh mục:**\n");
                    for (Map<String, Object> cat : catData) {
                        double percent = getDoubleValue(cat.get("percentage"));
                        sb.append("- ").append(cat.get("categoryName")).append(": ")
                          .append(formatCurrency(getDoubleValue(cat.get("revenue"))))
                          .append(" (").append(String.format("%.1f", percent)).append("%)\n");
                    }
                    sb.append("\n");
                }
            }
        }

        // Insight
        sb.append("---\n");
        sb.append("💡 **Nhận xét & Đề xuất:**\n");
        if (result.containsKey("totalRevenue") && result.containsKey("totalOrders")) {
            double avgValue = getDoubleValue(result.get("avgOrderValue"));
            if (avgValue < 500000) {
                sb.append("- Giá trị đơn TB còn thấp - nên tăng qua combo deals, upsell\n");
            } else {
                sb.append("- Giá trị đơn tốt - duy trì và cải thiện tỷ lệ chuyển đổi\n");
            }
        }
        if (result.containsKey("growth")) {
            double growth = getDoubleValue(result.get("growth"));
            if (growth > 0) {
                sb.append("- Tăng trưởng ").append(String.format("%.1f", growth)).append("% so với kỳ trước 📈\n");
            } else if (growth < 0) {
                sb.append("- Giảm ").append(String.format("%.1f", Math.abs(growth))).append("% so với kỳ trước - cần chiến lược kích cầu 📉\n");
            }
        }
    }

    // ==================== UTILITY METHODS ====================

    private Double getDoubleValue(Object obj) {
        if (obj == null) return 0.0;
        if (obj instanceof Double) return (Double) obj;
        if (obj instanceof Integer) return ((Integer) obj).doubleValue();
        if (obj instanceof Long) return ((Long) obj).doubleValue();
        if (obj instanceof Number) return ((Number) obj).doubleValue();
        try {
            return Double.parseDouble(obj.toString());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private Integer getIntValue(Object obj) {
        if (obj == null) return 0;
        if (obj instanceof Integer) return (Integer) obj;
        if (obj instanceof Long) return ((Long) obj).intValue();
        if (obj instanceof Double) return ((Double) obj).intValue();
        if (obj instanceof Number) return ((Number) obj).intValue();
        try {
            return Integer.parseInt(obj.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String getSegmentIcon(String segmentName) {
        if (segmentName == null) return "📊";
        String lower = segmentName.toLowerCase();
        if (lower.contains("diamond") || lower.contains("vip")) return "💎";
        if (lower.contains("gold")) return "🥇";
        if (lower.contains("silver")) return "🥈";
        if (lower.contains("bronze")) return "🥉";
        return "📊";
    }

    private String getStatusIcon(String status) {
        if (status == null) return "📋";
        switch (status.toUpperCase()) {
            case "PENDING": return "⏳";
            case "PROCESSING": return "🔄";
            case "SHIPPED": return "🚚";
            case "DELIVERED": return "✅";
            case "COMPLETED": return "✅";
            case "CANCELLED": return "❌";
            case "REFUNDED": return "💸";
            default: return "📋";
        }
    }

    private String translateOrderStatus(String status) {
        if (status == null) return "Không xác định";
        switch (status.toUpperCase()) {
            case "PENDING": return "Chờ xác nhận";
            case "PROCESSING": return "Đang xử lý";
            case "SHIPPED": return "Đang giao";
            case "DELIVERED": return "Đã giao";
            case "COMPLETED": return "Hoàn thành";
            case "CANCELLED": return "Đã hủy";
            case "REFUNDED": return "Hoàn tiền";
            default: return status;
        }
    }
    
    /**
     * Format vendor business strategy
     */
    @SuppressWarnings("unchecked")
    private void formatVendorBusinessStrategy(StringBuilder sb, Map<String, Object> result) {
        // Key insights
        if (result.containsKey("keyInsights")) {
            List<String> insights = (List<String>) result.get("keyInsights");
            sb.append("📊 **Tổng quan tình hình:**\n");
            for (String insight : insights) {
                sb.append(insight).append("\n");
            }
            sb.append("\n");
        }
        
        // Strategies
        if (result.containsKey("strategies")) {
            List<Map<String, Object>> strategies = (List<Map<String, Object>>) result.get("strategies");
            sb.append("---\n\n");
            sb.append("## 🎯 Các chiến lược đề xuất:\n\n");
            
            for (int i = 0; i < strategies.size(); i++) {
                Map<String, Object> strategy = strategies.get(i);
                String priority = (String) strategy.get("priority");
                String priorityEmoji = priority.equals("CRITICAL") ? "🔴" : priority.equals("HIGH") ? "🟠" : "🟢";
                
                sb.append("### ").append(i + 1).append(". ").append(priorityEmoji).append(" ")
                  .append(strategy.get("category")).append("\n");
                sb.append("**Mức độ ưu tiên:** ").append(priority).append("\n\n");
                
                if (strategy.containsKey("actions")) {
                    sb.append("**Hành động cụ thể:**\n");
                    List<String> actions = (List<String>) strategy.get("actions");
                    for (String action : actions) {
                        sb.append("- ").append(action).append("\n");
                    }
                    sb.append("\n");
                }
                
                if (strategy.containsKey("expectedImpact")) {
                    sb.append("**Kết quả kỳ vọng:** ").append(strategy.get("expectedImpact")).append("\n");
                }
                sb.append("\n");
            }
        }
        
        sb.append("---\n\n");
        sb.append("💡 **Lưu ý:** Hãy triển khai các chiến lược theo thứ tự ưu tiên. ");
        sb.append("Bắt đầu với các action CRITICAL và HIGH trước, sau đó mới đến MEDIUM. ");
        sb.append("Theo dõi kết quả sau 1-2 tuần để điều chỉnh nếu cần.");
    }
    
    /**
     * Format admin business strategy
     */
    @SuppressWarnings("unchecked")
    private void formatAdminBusinessStrategy(StringBuilder sb, Map<String, Object> result) {
        // Key insights
        if (result.containsKey("keyInsights")) {
            List<String> insights = (List<String>) result.get("keyInsights");
            sb.append("📊 **Phân tích toàn cảnh:**\n");
            for (String insight : insights) {
                sb.append(insight).append("\n");
            }
            sb.append("\n");
        }
        
        // Top products
        if (result.containsKey("topProducts")) {
            List<Map<String, Object>> topProducts = (List<Map<String, Object>>) result.get("topProducts");
            if (!topProducts.isEmpty()) {
                sb.append("🏆 **Top 5 sản phẩm toàn hệ thống:**\n");
                for (int i = 0; i < topProducts.size(); i++) {
                    Map<String, Object> p = topProducts.get(i);
                    sb.append((i + 1)).append(". ").append(p.get("name"))
                      .append(" - ").append(p.get("sales")).append(" lượt bán")
                      .append(" (").append(formatCurrency(((Number)p.get("revenue")).doubleValue())).append(")\n");
                }
                sb.append("\n");
            }
        }
        
        // Strategies
        if (result.containsKey("strategies")) {
            List<Map<String, Object>> strategies = (List<Map<String, Object>>) result.get("strategies");
            sb.append("---\n\n");
            sb.append("## 🚀 Các chiến lược phát triển hệ thống:\n\n");
            
            for (int i = 0; i < strategies.size(); i++) {
                Map<String, Object> strategy = strategies.get(i);
                String priority = (String) strategy.get("priority");
                String priorityEmoji = priority.equals("CRITICAL") ? "🔴" : priority.equals("HIGH") ? "🟠" : "🟢";
                
                sb.append("### ").append(i + 1).append(". ").append(priorityEmoji).append(" ")
                  .append(strategy.get("category")).append("\n");
                sb.append("**Mức độ ưu tiên:** ").append(priority).append("\n\n");
                
                if (strategy.containsKey("actions")) {
                    sb.append("**Hành động cụ thể:**\n");
                    List<String> actions = (List<String>) strategy.get("actions");
                    for (String action : actions) {
                        sb.append("- ").append(action).append("\n");
                    }
                    sb.append("\n");
                }
                
                if (strategy.containsKey("expectedImpact")) {
                    sb.append("**Tác động kỳ vọng:** ").append(strategy.get("expectedImpact")).append("\n");
                }
                sb.append("\n");
            }
        }
        
        sb.append("---\n\n");
        sb.append("🎯 **Roadmap đề xuất:**\n");
        sb.append("- **Q1 (ngay):** Triển khai các chiến lược CRITICAL và HIGH priority\n");
        sb.append("- **Q2:** Đo lường KPIs, tối ưu dựa trên data thực tế\n");
        sb.append("- **Q3-Q4:** Scale thành công và mở rộng thị trường\n\n");
        sb.append("💼 Với roadmap này, hệ thống có thể đạt tăng trưởng 50-70% GMV trong năm tới.");
    }
    
    /**
     * Phát hiện intent và suggest function nếu AI không gọi
     */
    private String detectMissingFunctionCall(String userMessage, String role) {
        String lower = userMessage.toLowerCase();
        
        if (role.equals("VENDOR")) {
            if (lower.contains("tồn kho") && (lower.contains("thấp") || lower.contains("ít") || lower.contains("sắp hết"))) {
                return "getLowStockProducts";
            }
            if ((lower.contains("bán chạy") || lower.contains("top") || lower.contains("phổ biến")) && lower.contains("sản phẩm")) {
                return "getTopSellingProducts";
            }
            if (lower.contains("doanh thu") || lower.contains("revenue") || lower.contains("thu nhập")) {
                return "getRevenueStats";
            }
            if (lower.contains("đơn hàng") && (lower.contains("mới") || lower.contains("gần đây"))) {
                return "getRecentOrders";
            }
        } else if (role.equals("ADMIN")) {
            if (lower.contains("phân khúc") && (lower.contains("điều chỉnh") || lower.contains("tối ưu"))) {
                return "suggestSegmentAdjustments";
            }
            if (lower.contains("phân tích") && lower.contains("phân khúc")) {
                return "getSegmentAnalytics";
            }
            if (lower.contains("tổng quan") || lower.contains("overview")) {
                return "getSystemOverview";
            }
        }
        
        return null;
    }
}
