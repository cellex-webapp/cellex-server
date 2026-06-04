package com.example.cellex.services.product;

import com.example.cellex.config.MLServiceConfig;
import com.example.cellex.models.product.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Service tích hợp Image Search với ML service (FastAPI).
 *
 * <p>Đây là lớp trung gian giữa Spring Boot và Python ML service.
 * Tất cả method index là @Async để không block luồng lưu sản phẩm của Admin.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ImageSearchService {

    private final WebClient mlServiceWebClient;
    private final MLServiceConfig mlServiceConfig;

    // ── Index ─────────────────────────────────────────────────────────────────

    /**
     * Index tất cả ảnh của sản phẩm vào pgvector (bất đồng bộ).
     * Gọi sau khi sản phẩm được tạo mới hoặc cập nhật ảnh.
     * Chạy trên thread pool riêng, không block Admin request.
     */
    @Async("imageSearchExecutor")
    public void indexProductAsync(Product product) {
        if (!mlServiceConfig.isEnabled()) {
            return;
        }
        if (product.getImages() == null || product.getImages().isEmpty()) {
            log.debug("Sản phẩm {} không có ảnh, bỏ qua index.", product.getId());
            return;
        }

        try {
            log.info("Bắt đầu index ảnh cho product: {}", product.getId());

            Map<String, Object> requestBody = Map.of(
                    "product_id", product.getId(),
                    "image_urls", product.getImages()
            );

            Map<?, ?> response = mlServiceWebClient
                    .post()
                    .uri("/api/v1/ml/image-search/index")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse ->
                            clientResponse.bodyToMono(String.class).flatMap(body -> {
                                log.error("ML service lỗi khi index product {}: {}", product.getId(), body);
                                return Mono.empty();
                            })
                    )
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(60))
                    .onErrorResume(e -> {
                        log.warn("Lỗi index ảnh product {}: {}", product.getId(), e.getMessage());
                        return Mono.empty();
                    })
                    .block();

            if (response != null) {
                log.info("Index ảnh product {} OK: indexed={}, failed={}",
                        product.getId(),
                        response.get("indexed_count"),
                        response.get("failed_count"));
            }

        } catch (Exception e) {
            log.error("Exception không mong đợi khi index product {}: {}", product.getId(), e.getMessage());
        }
    }

    /**
     * Xóa embeddings của sản phẩm khỏi pgvector (bất đồng bộ).
     * Gọi sau khi sản phẩm bị xóa.
     */
    @Async("imageSearchExecutor")
    public void deleteProductEmbeddingsAsync(String productId) {
        if (!mlServiceConfig.isEnabled()) {
            return;
        }
        try {
            log.info("Xóa embeddings của product: {}", productId);

            mlServiceWebClient
                    .delete()
                    .uri("/api/v1/ml/image-search/" + productId)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse ->
                            clientResponse.bodyToMono(String.class).flatMap(body -> {
                                log.error("ML service lỗi khi xóa embeddings {}: {}", productId, body);
                                return Mono.empty();
                            })
                    )
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(30))
                    .onErrorResume(e -> {
                        log.warn("Lỗi xóa embeddings product {}: {}", productId, e.getMessage());
                        return Mono.empty();
                    })
                    .block();

        } catch (Exception e) {
            log.error("Exception khi xóa embeddings product {}: {}", productId, e.getMessage());
        }
    }

    // ── Search ────────────────────────────────────────────────────────────────

    /**
     * Tìm kiếm sản phẩm tương tự bằng ảnh upload.
     * Gửi file ảnh sang ML service, nhận về danh sách productId theo thứ tự tương đồng.
     *
     * @param imageFile File ảnh từ người dùng upload
     * @param topK      Số kết quả tối đa
     * @return List SearchResult (productId + similarityScore)
     */
    @SuppressWarnings("unchecked")
    public List<ImageSearchResult> searchByImage(MultipartFile imageFile, int topK) {
        if (!mlServiceConfig.isEnabled()) {
            log.warn("ML service bị tắt, không thể tìm kiếm bằng ảnh.");
            return Collections.emptyList();
        }

        try {
            log.info("Tìm kiếm bằng ảnh, size={}bytes, topK={}",
                    imageFile.getSize(), topK);

            // Build multipart request
            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            builder.part("file", new ByteArrayResource(imageFile.getBytes()) {
                @Override
                public String getFilename() {
                    String original = imageFile.getOriginalFilename();
                    return (original != null && !original.isEmpty()) ? original : "image.jpg";
                }
            }).contentType(MediaType.parseMediaType(
                    imageFile.getContentType() != null
                            ? imageFile.getContentType()
                            : "image/jpeg"
            ));

            Map<?, ?> response = mlServiceWebClient
                    .post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/ml/image-search/search")
                            .queryParam("top_k", topK)
                            .build())
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(builder.build()))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse ->
                            clientResponse.bodyToMono(String.class).flatMap(body -> {
                                log.error("ML service lỗi tìm kiếm ảnh: {}", body);
                                return Mono.error(new RuntimeException("ML service error: " + body));
                            })
                    )
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

            if (response == null || !Boolean.TRUE.equals(response.get("success"))) {
                log.warn("ML service trả về kết quả rỗng hoặc thất bại.");
                return Collections.emptyList();
            }

            Object resultsObj = response.get("results");
            List<Map<String, Object>> rawResults = resultsObj instanceof List 
                    ? (List<Map<String, Object>>) resultsObj 
                    : Collections.emptyList();

            return rawResults.stream()
                    .map(r -> new ImageSearchResult(
                            (String) r.get("product_id"),
                            ((Number) r.getOrDefault("similarity_score", 0.0)).doubleValue(),
                            ((Number) r.getOrDefault("rank", 0)).intValue()
                    ))
                    .toList();

        } catch (Exception e) {
            log.error("Lỗi tìm kiếm bằng ảnh: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    // ── Inner record ──────────────────────────────────────────────────────────

    public record ImageSearchResult(String productId, double similarityScore, int rank) {}
}
