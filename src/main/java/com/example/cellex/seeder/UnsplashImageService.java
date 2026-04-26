package com.example.cellex.seeder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@Slf4j
@Profile({"default", "dev"})
public class UnsplashImageService {

    private static final String UNSPLASH_SEARCH_URL = "https://api.unsplash.com/search/photos";
    private static final long RATE_LIMIT_COOLDOWN_MS = 60 * 60 * 1000L;
        private static final int MAX_API_CALLS_PER_RUN = 50;
    private static final List<String> BLACKLIST_TERMS = List.of(
            "galaxy space", "milky way", "astronomy", "cosmos", "nebula"
    );

    @Value("${unsplash.access-key:}")
    private String accessKey;

    @Value("${unsplash.per-page:5}")
    private int perPage;

    @Value("${unsplash.timeout-ms:5000}")
    private int timeoutMs;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final Map<String, String> imageCache = new ConcurrentHashMap<>();
    private final AtomicInteger apiCallCounter = new AtomicInteger(0);
    private volatile long rateLimitCooldownUntilMs = 0L;
    private volatile boolean rateLimitWarned = false;
    private volatile boolean maxCallWarned = false;

    public UnsplashImageService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    @PostConstruct
    public void checkConfig() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeoutMs);
        requestFactory.setReadTimeout(timeoutMs);
        restTemplate.setRequestFactory(requestFactory);

        if (accessKey == null || accessKey.isBlank()) {
            log.warn("UNSPLASH_ACCESS_KEY is not configured. All product images will use fallback URL.");
        }
    }

    public String fetchBestImageUrl(String productName, String categoryName) {
        String cacheKey = Optional.ofNullable(productName).orElse("").toLowerCase(Locale.ROOT).trim();
        if (cacheKey.isEmpty()) {
            return buildPublicSourceUrl("electronics");
        }

        if (imageCache.containsKey(cacheKey)) {
            return imageCache.get(cacheKey);
        }

        String primaryQuery = buildSearchQuery(productName, categoryName);
        String url = callUnsplashApi(primaryQuery);

        if (url == null) {
            String fallbackQuery = removeDiacritics(Optional.ofNullable(categoryName).orElse("")) + " electronics product";
            url = callUnsplashApi(fallbackQuery);
        }

        if (url == null || url.isBlank()) {
            url = buildPublicSourceUrl(primaryQuery);
        }

        imageCache.put(cacheKey, url);

        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Unsplash throttle sleep interrupted");
        }

        return url;
    }

    private String buildPublicSourceUrl(String query) {
        String safeQuery = Optional.ofNullable(query).orElse("electronics").trim();
        if (safeQuery.isBlank()) {
            safeQuery = "electronics";
        }
        return "https://source.unsplash.com/featured/1200x900/?" + URLEncoder.encode(safeQuery, StandardCharsets.UTF_8);
    }

    private String buildSearchQuery(String productName, String categoryName) {
        String rawProductName = Optional.ofNullable(productName).orElse("").trim();
        String rawCategoryName = Optional.ofNullable(categoryName).orElse("").trim();

        String product = removeDiacritics(rawProductName).toLowerCase(Locale.ROOT);
        String category = removeDiacritics(rawCategoryName).toLowerCase(Locale.ROOT);
        String query;

        if (product.contains("airpods")) {
            query = "airpods earbuds";
        } else if (product.contains("bose") || product.contains("sony") || product.contains("jbl") || product.contains("sennheiser")) {
            String brand = extractBrand(product);
            query = brand + " headphones audio";
        } else if (product.contains("tai nghe") || category.equals("tai nghe")) {
            String brand = extractBrand(product);
            query = (brand.isBlank() ? "wireless" : brand) + " headphones";
        } else if (product.contains("galaxy") && !product.contains("watch")) {
            query = rawProductName + " smartphone";
        } else if (product.contains("iphone")) {
            query = rawProductName + " apple smartphone";
        } else if (product.contains("macbook")) {
            query = rawProductName + " apple laptop";
        } else if (product.contains("ipad")) {
            query = rawProductName + " apple tablet";
        } else if (product.contains("laptop") || category.equals("laptop")) {
            query = rawProductName + " laptop computer";
        } else if (product.contains("tablet") || category.equals("may tinh bang")) {
            query = rawProductName + " tablet device";
        } else if (product.contains("watch") || category.equals("dong ho thong minh")) {
            query = rawProductName + " smartwatch wearable";
        } else if (product.contains("sac") || product.contains("power bank") || product.contains("powerbank") || category.equals("sac du phong")) {
            query = rawProductName + " portable charger powerbank";
        } else if (product.contains("camera") || category.equals("camera")) {
            query = rawProductName + " digital camera photography";
        } else {
            query = rawProductName + " " + rawCategoryName + " product electronics";
        }

        String cleaned = removeDiacritics(query).replaceAll("\\s+", " ").trim();
        if (cleaned.length() > 60) {
            return cleaned.substring(0, 60).trim();
        }
        return cleaned;
    }

    private String extractBrand(String normalizedProductName) {
        if (normalizedProductName.contains("bose")) {
            return "bose";
        }
        if (normalizedProductName.contains("sony")) {
            return "sony";
        }
        if (normalizedProductName.contains("jbl")) {
            return "jbl";
        }
        if (normalizedProductName.contains("sennheiser")) {
            return "sennheiser";
        }
        return "";
    }

    private String callUnsplashApi(String query) {
        return callUnsplashApi(query, true);
    }

    private String callUnsplashApi(String query, boolean allowRetry) {
        if (accessKey == null || accessKey.isBlank()) {
            return null;
        }

        if (apiCallCounter.get() >= MAX_API_CALLS_PER_RUN) {
            if (!maxCallWarned) {
                maxCallWarned = true;
                log.warn("Unsplash API call limit reached ({} calls). Use fallback images for remaining products.", MAX_API_CALLS_PER_RUN);
            }
            return null;
        }

        long now = System.currentTimeMillis();
        if (now < rateLimitCooldownUntilMs) {
            return null;
        }
        if (rateLimitWarned) {
            rateLimitWarned = false;
            log.info("Unsplash cooldown finished, resuming API calls");
        }

        try {
            int callNumber = apiCallCounter.incrementAndGet();
            if (callNumber > MAX_API_CALLS_PER_RUN) {
                return null;
            }

            String url = UriComponentsBuilder.fromHttpUrl(UNSPLASH_SEARCH_URL)
                    .queryParam("query", query)
                    .queryParam("per_page", perPage)
                    .queryParam("orientation", "squarish")
                    .queryParam("order_by", "relevance")
                    .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Client-ID " + accessKey);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return null;
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode resultsNode = root.path("results");
            if (!resultsNode.isArray() || resultsNode.isEmpty()) {
                return null;
            }

            List<JsonNode> results = new ArrayList<>();
            resultsNode.forEach(results::add);
            return selectBestImage(results, query);
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode().value() == 429 && allowRetry) {
                log.warn("Unsplash rate limit reached, waiting 2 seconds and retrying once");
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    log.warn("Unsplash retry sleep interrupted");
                    return null;
                }
                return callUnsplashApi(query, false);
            }

            if (isRateLimitExceeded(ex)) {
                rateLimitCooldownUntilMs = System.currentTimeMillis() + RATE_LIMIT_COOLDOWN_MS;
                if (!rateLimitWarned) {
                    rateLimitWarned = true;
                    log.warn("Unsplash rate limit exceeded. Skip API calls for 60 minutes and use fallback images.");
                }
                return null;
            }

            log.warn("Unsplash client error for query '{}': {}", query, ex.getMessage());
            return null;
        } catch (Exception ex) {
            log.warn("Unsplash call failed for query '{}': {}", query, ex.getMessage());
            return null;
        }
    }

    private boolean isRateLimitExceeded(HttpClientErrorException ex) {
        int status = ex.getStatusCode().value();
        if (status == 429) {
            return true;
        }
        if (status != 403) {
            return false;
        }

        String body = Optional.ofNullable(ex.getResponseBodyAsString()).orElse("").toLowerCase(Locale.ROOT);
        String message = Optional.ofNullable(ex.getMessage()).orElse("").toLowerCase(Locale.ROOT);
        return body.contains("rate limit") || message.contains("rate limit") || body.contains("quota") || message.contains("quota");
    }

    private String selectBestImage(List<JsonNode> results, String query) {
        List<String> queryWords = Arrays.stream(Optional.ofNullable(query).orElse("").toLowerCase(Locale.ROOT).split("\\s+"))
                .filter(word -> !word.isBlank())
                .toList();

        int bestScore = Integer.MIN_VALUE;
        String bestUrl = null;

        for (JsonNode result : results) {
            String alt = result.path("alt_description").asText("").toLowerCase(Locale.ROOT);
            String description = result.path("description").asText("").toLowerCase(Locale.ROOT);

            int score = 0;

            for (String word : queryWords) {
                if (alt.contains(word)) {
                    score += 3;
                }
                if (description.contains(word)) {
                    score += 2;
                }
            }

            JsonNode tags = result.path("tags");
            if (tags.isArray()) {
                for (JsonNode tag : tags) {
                    String tagTitle = tag.path("title").asText("").toLowerCase(Locale.ROOT);
                    boolean matched = false;
                    for (String word : queryWords) {
                        if (tagTitle.contains(word)) {
                            matched = true;
                            break;
                        }
                    }
                    if (matched) {
                        score += 2;
                    }
                }
            }

            double width = result.path("width").asDouble(0);
            double height = result.path("height").asDouble(0);
            if (width > 0 && height > 0) {
                double ratio = width / height;
                if (ratio >= 0.8 && ratio <= 1.2) {
                    score += 1;
                }
            }

            if (containsBlacklistTerm(alt)) {
                score -= 10;
            }

            String candidateUrl = result.path("urls").path("regular").asText("");
            if (candidateUrl.isBlank()) {
                candidateUrl = result.path("urls").path("small").asText("");
            }

            if (score > 0 && !candidateUrl.isBlank() && score > bestScore) {
                bestScore = score;
                bestUrl = candidateUrl;
            }
        }

        if (bestUrl != null) {
            return bestUrl;
        }

        for (JsonNode result : results) {
            String alt = result.path("alt_description").asText("").toLowerCase(Locale.ROOT);
            if (containsBlacklistTerm(alt)) {
                continue;
            }
            String regular = result.path("urls").path("regular").asText("");
            if (!regular.isBlank()) {
                return regular;
            }
        }

        JsonNode first = results.get(0);
        String firstAlt = first.path("alt_description").asText("").toLowerCase(Locale.ROOT);
        if (containsBlacklistTerm(firstAlt)) {
            return null;
        }
        String firstRegular = first.path("urls").path("regular").asText("");
        if (!firstRegular.isBlank()) {
            return firstRegular;
        }
        return first.path("urls").path("small").asText("");
    }

    private boolean containsBlacklistTerm(String text) {
        String safeText = Optional.ofNullable(text).orElse("").toLowerCase(Locale.ROOT);
        for (String term : BLACKLIST_TERMS) {
            if (safeText.contains(term)) {
                return true;
            }
        }
        return false;
    }

    private String removeDiacritics(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD);
        return normalized.replaceAll("[^\\p{ASCII}]", "");
    }
}
