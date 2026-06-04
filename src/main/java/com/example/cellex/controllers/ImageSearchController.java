package com.example.cellex.controllers;

import com.example.cellex.dtos.response.PageResponse;
import com.example.cellex.dtos.response.product.ProductResponse;
import com.example.cellex.services.product.ImageSearchService;
import com.example.cellex.services.product.ImageSearchService.ImageSearchResult;
import com.example.cellex.services.product.ProductService;
import com.example.cellex.repositories.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller xử lý Image Search endpoint.
 *
 * <p>POST /api/v1/products/image-search - Tìm kiếm sản phẩm bằng ảnh</p>
 */
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Slf4j
public class ImageSearchController {

    private final ImageSearchService imageSearchService;
    private final ProductService productService;
    private final ProductRepository productRepository;

    /**
     * Tìm kiếm sản phẩm bằng hình ảnh upload.
     *
     * <p>Nhận file ảnh, forward sang ML service để tạo CLIP embedding,
     * tìm sản phẩm tương tự, trả về danh sách ProductResponse theo thứ tự tương đồng.</p>
     *
     * @param file  Ảnh upload từ người dùng (JPEG/PNG/WEBP)
     * @param topK  Số kết quả tối đa (default 20)
     */
    @PostMapping(value = "/image-search", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> searchByImage(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "top_k", defaultValue = "20") int topK
    ) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    Map.of("success", false, "message", "File ảnh không được để trống.")
            );
        }

        // Validate file type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ResponseEntity.badRequest().body(
                    Map.of("success", false, "message", "Chỉ chấp nhận file ảnh.")
            );
        }

        // Validate file size (max 10MB)
        if (file.getSize() > 10 * 1024 * 1024) {
            return ResponseEntity.badRequest().body(
                    Map.of("success", false, "message", "File ảnh quá lớn (tối đa 10MB).")
            );
        }

        try {
            int safeTopK = Math.min(Math.max(topK, 1), 50);

            // 1. Gọi ML service lấy danh sách productId theo thứ tự tương đồng
            List<ImageSearchResult> mlResults = imageSearchService.searchByImage(file, safeTopK);

            if (mlResults.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Không tìm thấy sản phẩm tương tự.",
                        "products", List.of(),
                        "total", 0
                ));
            }

            // 2. Fetch product details từ MongoDB, giữ đúng thứ tự từ ML
            Map<String, ImageSearchResult> resultMap = new LinkedHashMap<>();
            for (ImageSearchResult r : mlResults) {
                resultMap.put(r.productId(), r);
            }

            List<String> productIds = mlResults.stream().map(ImageSearchResult::productId).toList();
            List<com.example.cellex.models.product.Product> products =
                    productRepository.findAllById(productIds);

            // Map về dict để lookup O(1)
            Map<String, com.example.cellex.models.product.Product> productMap = new java.util.HashMap<>();
            for (var p : products) {
                productMap.put(p.getId(), p);
            }

            // 3. Build response theo thứ tự tương đồng giảm dần
            List<Map<String, Object>> responseProducts = new ArrayList<>();
            for (String pid : productIds) {
                var product = productMap.get(pid);
                if (product == null || !Boolean.TRUE.equals(product.getIsPublished())) {
                    continue;
                }
                var mlResult = resultMap.get(pid);
                Map<String, Object> item = new java.util.HashMap<>();
                item.put("id", product.getId());
                item.put("name", product.getName());
                item.put("price", product.getPrice());
                item.put("finalPrice", product.getFinalPrice());
                item.put("images", product.getImages());
                item.put("averageRating", product.getAverageRating());
                item.put("reviewCount", product.getReviewCount());
                item.put("shopId", product.getShopId());
                item.put("categoryId", product.getCategoryId());
                item.put("similarityScore", mlResult != null ? mlResult.similarityScore() : 0.0);
                item.put("rank", mlResult != null ? mlResult.rank() : 0);
                responseProducts.add(item);
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Tìm thấy " + responseProducts.size() + " sản phẩm tương tự.",
                    "products", responseProducts,
                    "total", responseProducts.size()
            ));

        } catch (Exception e) {
            log.error("Lỗi image search: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                    Map.of("success", false, "message", "Lỗi hệ thống khi tìm kiếm.")
            );
        }
    }

    /**
     * Endpoint API tạm thời để backfill/sync tất cả sản phẩm cũ vào pgvector.
     * Quá trình index chạy ngầm (async) qua thread pool.
     */
    @PostMapping("/sync-all")
    public ResponseEntity<?> syncAllProducts() {
        List<com.example.cellex.models.product.Product> products = productRepository.findAll();
        int count = 0;
        for (var p : products) {
            if (p.getImages() != null && !p.getImages().isEmpty()) {
                imageSearchService.indexProductAsync(p);
                count++;
            }
        }
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Đã đẩy lệnh sync " + count + " sản phẩm chạy ngầm."
        ));
    }
}
