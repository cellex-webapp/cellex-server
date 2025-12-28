package com.example.cellex.services.review;

import com.example.cellex.models.review.ModerationResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class OpenAIModerationService {

    private static final String OPENAI_MODERATION_URL = "https://api.openai.com/v1/moderations";

    @Value("${openai.api.key:}")
    private String openaiApiKey;

    @Value("${openai.moderation.enabled:true}")
    private boolean moderationEnabled;

    @Value("${openai.moderation.mock-mode:false}")
    private boolean mockMode;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public OpenAIModerationService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    @Async
    public CompletableFuture<ModerationResult> moderateText(String text) {
        return CompletableFuture.supplyAsync(() -> moderateTextSync(text));
    }

    public ModerationResult moderateTextSync(String text) {
        // Check if moderation is disabled
        if (!moderationEnabled) {
            log.info("OpenAI moderation is disabled, auto-approving review");
            return createModerationDisabledResult();
        }

        // Check if API key is not configured
        if (openaiApiKey == null || openaiApiKey.isEmpty()) {
            log.warn("OpenAI API key not configured, auto-approving review");
            return createNoApiKeyResult();
        }

        int maxRetries = 5;
        int retryCount = 0;
        long waitTime = 10000; // Start with 10 seconds for rate limit

        while (retryCount < maxRetries) {
            try {
                return callModerationApi(text);
            } catch (Exception e) {
                String errorMsg = e.getMessage();
                
                // Check if it's a rate limit error (429)
                if (errorMsg != null && errorMsg.contains("429")) {
                    retryCount++;
                    if (retryCount < maxRetries) {
                        log.warn("Rate limited by OpenAI API, retry {}/{} after {}ms", retryCount, maxRetries, waitTime);
                        try {
                            Thread.sleep(waitTime);
                            waitTime = Math.min(waitTime * 2, 60000); // Exponential backoff, max 60s
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                        continue;
                    }
                }
                
                log.error("Error calling OpenAI Moderation API: {}", errorMsg, e);
                // Return null to indicate API failure - review stays in PENDING_MODERATION
                return null;
            }
        }
        
        log.error("Max retries exceeded for OpenAI Moderation API");
        return null;
    }

    private ModerationResult callModerationApi(String text) throws Exception {
        if (text == null || text.trim().isEmpty()) {
            log.info("Empty text provided for moderation, auto-approving");
            return createEmptyTextResult();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openaiApiKey);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("input", text);
        requestBody.put("model", "omni-moderation-latest");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                OPENAI_MODERATION_URL,
                HttpMethod.POST,
                entity,
                String.class
        );

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return parseModerationResponse(response.getBody());
        } else {
            throw new RuntimeException("OpenAI API returned non-success status: " + response.getStatusCode());
        }
    }

    private ModerationResult parseModerationResponse(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode results = root.get("results");

        if (results == null || !results.isArray() || results.isEmpty()) {
            throw new RuntimeException("Invalid response structure from OpenAI");
        }

        JsonNode firstResult = results.get(0);
        boolean isFlagged = firstResult.get("flagged").asBoolean();

        List<String> flaggedCategories = new ArrayList<>();
        Map<String, Double> categoryScores = new HashMap<>();

        JsonNode categories = firstResult.get("categories");
        JsonNode scores = firstResult.get("category_scores");

        if (categories != null) {
            Iterator<Map.Entry<String, JsonNode>> categoryFields = categories.fields();
            while (categoryFields.hasNext()) {
                Map.Entry<String, JsonNode> field = categoryFields.next();
                if (field.getValue().asBoolean()) {
                    flaggedCategories.add(field.getKey());
                }
            }
        }

        if (scores != null) {
            Iterator<Map.Entry<String, JsonNode>> scoreFields = scores.fields();
            while (scoreFields.hasNext()) {
                Map.Entry<String, JsonNode> field = scoreFields.next();
                categoryScores.put(field.getKey(), field.getValue().asDouble());
            }
        }

        String modelUsed = root.has("model") ? root.get("model").asText() : "omni-moderation-latest";

        log.info("Moderation result - Flagged: {}, Categories: {}", isFlagged, flaggedCategories);

        return ModerationResult.builder()
                .isFlagged(isFlagged)
                .flaggedCategories(flaggedCategories)
                .categoryScores(categoryScores)
                .rawResponse(responseBody)
                .moderatedAt(LocalDateTime.now())
                .modelUsed(modelUsed)
                .build();
    }

    private ModerationResult createFallbackResult(String errorMessage) {
        // On API failure, we don't flag the content but mark it for manual review
        return ModerationResult.builder()
                .isFlagged(false)
                .flaggedCategories(Collections.emptyList())
                .categoryScores(Collections.emptyMap())
                .rawResponse("{\"error\": \"" + errorMessage + "\"}")
                .moderatedAt(LocalDateTime.now())
                .modelUsed("fallback")
                .build();
    }

    private ModerationResult createNoApiKeyResult() {
        return ModerationResult.builder()
                .isFlagged(false)
                .flaggedCategories(Collections.emptyList())
                .categoryScores(Collections.emptyMap())
                .rawResponse("{\"info\": \"API key not configured, moderation skipped\"}")
                .moderatedAt(LocalDateTime.now())
                .modelUsed("none")
                .build();
    }

    private ModerationResult createEmptyTextResult() {
        return ModerationResult.builder()
                .isFlagged(false)
                .flaggedCategories(Collections.emptyList())
                .categoryScores(Collections.emptyMap())
                .rawResponse("{\"info\": \"Empty text, auto-approved\"}")
                .moderatedAt(LocalDateTime.now())
                .modelUsed("none")
                .build();
    }

    private ModerationResult createModerationDisabledResult() {
        return ModerationResult.builder()
                .isFlagged(false)
                .flaggedCategories(Collections.emptyList())
                .categoryScores(Collections.emptyMap())
                .rawResponse("{\"info\": \"Moderation disabled, auto-approved\"}")
                .moderatedAt(LocalDateTime.now())
                .modelUsed("disabled")
                .build();
    }
}
