package com.example.cellex.services.recommendation;

import com.example.cellex.config.MLServiceConfig;
import com.example.cellex.dtos.response.ml.MLModelInfo;
import com.example.cellex.dtos.response.ml.MLRecommendationItem;
import com.example.cellex.dtos.response.ml.MLSimilarProductItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Service de goi ML microservice (FastAPI) cho recommendations
 * Xu ly communication, error handling, va fallback logic
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MLRecommendationService {

    private final WebClient mlServiceWebClient;
    private final MLServiceConfig mlServiceConfig;

    /**
     * Lay hybrid recommendations tu ML service
     * Ket hop SVD++ voi popularity-based scoring
     *
     * @return List MLRecommendationItem hoac empty list neu loi
     */
    public Optional<List<MLRecommendationItem>> getHybridRecommendations(
            String userId, Integer limit, String categoryId) {

        if (!mlServiceConfig.isEnabled()) {
            log.debug("ML service is disabled. Skipping ML recommendations.");
            return Optional.empty();
        }

        try {
            log.info("Calling ML service /hybrid/{} with limit={}, categoryId={}",
                    userId, limit, categoryId);

            List<MLRecommendationItem> recommendations = mlServiceWebClient
                    .get()
                    .uri(uriBuilder -> {
                        var builder = uriBuilder
                                .path("/api/v1/ml/hybrid/{userId}")
                                .queryParam("limit", limit != null ? limit : 20);
                        if (categoryId != null) {
                            builder.queryParam("category_id", categoryId);
                        }
                        return builder.build(userId);
                    })
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<MLRecommendationItem>>() {})
                    .timeout(Duration.ofSeconds(mlServiceConfig.getTimeoutSeconds()))
                    .onErrorResume(error -> {
                        log.warn("ML service call failed for user {}: {}", userId, error.getMessage());
                        return Mono.just(Collections.emptyList());
                    })
                    .block();

            if (recommendations != null && !recommendations.isEmpty()) {
                log.info("ML service returned {} recommendations for user {}",
                        recommendations.size(), userId);
                return Optional.of(recommendations);
            } else {
                log.info("ML service returned empty recommendations for user {}", userId);
                return Optional.empty();
            }

        } catch (WebClientException e) {
            log.error("WebClient error calling ML service for user {}: {}", userId, e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.error("Unexpected error calling ML service for user {}", userId, e);
            return Optional.empty();
        }
    }

    /**
     * Lay similar products tu ML service
     * Su dung SVD++ latent factors cho similarity
     *
     * @return List MLSimilarProductItem hoac empty list neu loi
     */
    public Optional<List<MLSimilarProductItem>> getSimilarProducts(
            String productId, Integer limit, String categoryId) {

        if (!mlServiceConfig.isEnabled()) {
            log.debug("ML service is disabled. Skipping ML similar products.");
            return Optional.empty();
        }

        try {
            log.info("Calling ML service /similar/{} with limit={}, categoryId={}",
                    productId, limit, categoryId);

            List<MLSimilarProductItem> similarProducts = mlServiceWebClient
                    .get()
                    .uri(uriBuilder -> {
                        var builder = uriBuilder
                                .path("/api/v1/ml/similar/{productId}")
                                .queryParam("limit", limit != null ? limit : 10);
                        if (categoryId != null) {
                            builder.queryParam("category_id", categoryId);
                        }
                        return builder.build(productId);
                    })
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<MLSimilarProductItem>>() {})
                    .timeout(Duration.ofSeconds(mlServiceConfig.getTimeoutSeconds()))
                    .onErrorResume(error -> {
                        log.warn("ML service call failed for product {}: {}",
                                productId, error.getMessage());
                        return Mono.just(Collections.emptyList());
                    })
                    .block();

            if (similarProducts != null && !similarProducts.isEmpty()) {
                log.info("ML service returned {} similar products for product {}",
                        similarProducts.size(), productId);
                return Optional.of(similarProducts);
            } else {
                log.info("ML service returned empty similar products for product {}", productId);
                return Optional.empty();
            }

        } catch (WebClientException e) {
            log.error("WebClient error calling ML service for product {}: {}",
                    productId, e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.error("Unexpected error calling ML service for product {}", productId, e);
            return Optional.empty();
        }
    }

    /**
     * Lay thong tin model hien tai
     */
    public Optional<MLModelInfo> getModelInfo() {
        if (!mlServiceConfig.isEnabled()) {
            return Optional.empty();
        }

        try {
            log.debug("Fetching ML model info");

            MLModelInfo modelInfo = mlServiceWebClient
                    .get()
                    .uri("/api/v1/ml/model-info")
                    .retrieve()
                    .bodyToMono(MLModelInfo.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();

            return Optional.ofNullable(modelInfo);

        } catch (Exception e) {
            log.warn("Failed to fetch ML model info: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Kiem tra ML service co available khong
     */
    public boolean isAvailable() {
        if (!mlServiceConfig.isEnabled()) {
            return false;
        }

        try {
            String response = mlServiceWebClient
                    .get()
                    .uri("/health")
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(3))
                    .block();

            return response != null && response.contains("healthy");

        } catch (Exception e) {
            log.debug("ML service health check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Trigger training model (admin only)
     */
    public boolean triggerTraining(boolean useTuning) {
        if (!mlServiceConfig.isEnabled()) {
            log.warn("Cannot trigger training: ML service is disabled");
            return false;
        }

        try {
            log.info("Triggering ML model training (use_tuning={})", useTuning);

            mlServiceWebClient
                    .post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/ml/train-async")
                            .queryParam("use_tuning", useTuning)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();

            log.info("ML training triggered successfully");
            return true;

        } catch (Exception e) {
            log.error("Failed to trigger ML training: {}", e.getMessage());
            return false;
        }
    }
}
