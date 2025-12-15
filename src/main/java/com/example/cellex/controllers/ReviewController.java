package com.example.cellex.controllers;

import com.example.cellex.dtos.request.review.CreateReviewRequest;
import com.example.cellex.dtos.request.review.VendorResponseRequest;
import com.example.cellex.dtos.response.ApiResponse;
import com.example.cellex.dtos.response.PageResponse;
import com.example.cellex.dtos.response.review.ReviewResponse;
import com.example.cellex.dtos.response.review.ReviewStatsResponse;
import com.example.cellex.services.review.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "15. Review Management", description = "APIs for managing product reviews")
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(
            summary = "Create a review",
            description = "Customer creates a review for a delivered product",
            requestBody = @RequestBody(
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE
                    )
            )
    )
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @ModelAttribute CreateReviewRequest request
    ) {
        String userId = ((com.example.cellex.models.user.User) userDetails).getId();
        ReviewResponse review = reviewService.createReview(userId, request);

        return ResponseEntity.ok(ApiResponse.<ReviewResponse>builder()
                .code(2000)
                .message("Tạo đánh giá thành công")
                .result(review)
                .build());
    }

    @Operation(summary = "Add vendor response", description = "Vendor adds a response to a review")
    @PostMapping("/{reviewId}/vendor-response")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<ApiResponse<ReviewResponse>> addVendorResponse(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String reviewId,
            @Valid @RequestBody VendorResponseRequest request
    ) {
        String vendorId = ((com.example.cellex.models.user.User) userDetails).getId();
        ReviewResponse review = reviewService.addVendorResponse(vendorId, reviewId, request);

        return ResponseEntity.ok(ApiResponse.<ReviewResponse>builder()
                .code(2000)
                .message("Thêm phản hồi thành công")
                .result(review)
                .build());
    }

    @Operation(summary = "Delete vendor response", description = "Vendor deletes their response to a review")
    @DeleteMapping("/{reviewId}/vendor-response")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<ApiResponse<Void>> deleteVendorResponse(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String reviewId
    ) {
        String vendorId = ((com.example.cellex.models.user.User) userDetails).getId();
        reviewService.deleteVendorResponse(vendorId, reviewId);

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .code(2000)
                .message("Xóa phản hồi thành công")
                .build());
    }

    @Operation(summary = "Get product reviews", description = "Get all reviews for a specific product")
    @GetMapping("/product/{productId}")
    public ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> getProductReviews(
            @PathVariable String productId,
            @io.swagger.v3.oas.annotations.Parameter(description = "Số trang (bắt đầu từ 1)") @RequestParam(defaultValue = "1") Integer page,
            @io.swagger.v3.oas.annotations.Parameter(description = "Số lượng mỗi trang") @RequestParam(defaultValue = "10") Integer limit,
            @io.swagger.v3.oas.annotations.Parameter(description = "Sắp xếp theo (createdAt, rating)") @RequestParam(defaultValue = "createdAt") String sortBy,
            @io.swagger.v3.oas.annotations.Parameter(description = "Kiểu sắp xếp (asc/desc)") @RequestParam(defaultValue = "desc") String sortType
    ) {
        int pageNumber = Math.max(page - 1, 0);
        org.springframework.data.domain.Sort.Direction direction = "asc".equalsIgnoreCase(sortType)
                ? org.springframework.data.domain.Sort.Direction.ASC
                : org.springframework.data.domain.Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(pageNumber, limit, org.springframework.data.domain.Sort.by(direction, sortBy));
        PageResponse<ReviewResponse> reviews = reviewService.getProductReviews(productId, pageable);

        return ResponseEntity.ok(ApiResponse.<PageResponse<ReviewResponse>>builder()
                .code(2000)
                .message("Lấy danh sách đánh giá thành công")
                .result(reviews)
                .build());
    }

    @Operation(summary = "Get shop reviews", description = "Get all reviews for products in a specific shop")
    @GetMapping("/shop/{shopId}")
    public ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> getShopReviews(
            @PathVariable String shopId,
            @io.swagger.v3.oas.annotations.Parameter(description = "Số trang (bắt đầu từ 1)") @RequestParam(defaultValue = "1") Integer page,
            @io.swagger.v3.oas.annotations.Parameter(description = "Số lượng mỗi trang") @RequestParam(defaultValue = "10") Integer limit,
            @io.swagger.v3.oas.annotations.Parameter(description = "Sắp xếp theo (createdAt, rating)") @RequestParam(defaultValue = "createdAt") String sortBy,
            @io.swagger.v3.oas.annotations.Parameter(description = "Kiểu sắp xếp (asc/desc)") @RequestParam(defaultValue = "desc") String sortType
    ) {
        int pageNumber = Math.max(page - 1, 0);
        org.springframework.data.domain.Sort.Direction direction = "asc".equalsIgnoreCase(sortType)
                ? org.springframework.data.domain.Sort.Direction.ASC
                : org.springframework.data.domain.Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(pageNumber, limit, org.springframework.data.domain.Sort.by(direction, sortBy));
        PageResponse<ReviewResponse> reviews = reviewService.getShopReviews(shopId, pageable);

        return ResponseEntity.ok(ApiResponse.<PageResponse<ReviewResponse>>builder()
                .code(2000)
                .message("Lấy danh sách đánh giá thành công")
                .result(reviews)
                .build());
    }

    @Operation(summary = "Get user reviews", description = "Get all reviews created by a specific user")
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> getUserReviews(
            @PathVariable String userId,
            @io.swagger.v3.oas.annotations.Parameter(description = "Số trang (bắt đầu từ 1)") @RequestParam(defaultValue = "1") Integer page,
            @io.swagger.v3.oas.annotations.Parameter(description = "Số lượng mỗi trang") @RequestParam(defaultValue = "10") Integer limit,
            @io.swagger.v3.oas.annotations.Parameter(description = "Sắp xếp theo (createdAt, rating)") @RequestParam(defaultValue = "createdAt") String sortBy,
            @io.swagger.v3.oas.annotations.Parameter(description = "Kiểu sắp xếp (asc/desc)") @RequestParam(defaultValue = "desc") String sortType
    ) {
        int pageNumber = Math.max(page - 1, 0);
        org.springframework.data.domain.Sort.Direction direction = "asc".equalsIgnoreCase(sortType)
                ? org.springframework.data.domain.Sort.Direction.ASC
                : org.springframework.data.domain.Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(pageNumber, limit, org.springframework.data.domain.Sort.by(direction, sortBy));
        PageResponse<ReviewResponse> reviews = reviewService.getUserReviews(userId, pageable);

        return ResponseEntity.ok(ApiResponse.<PageResponse<ReviewResponse>>builder()
                .code(2000)
                .message("Lấy danh sách đánh giá thành công")
                .result(reviews)
                .build());
    }

    @Operation(summary = "Get my reviews", description = "Get all reviews created by the authenticated user")
    @GetMapping("/my-reviews")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> getMyReviews(
            @AuthenticationPrincipal UserDetails userDetails,
            @io.swagger.v3.oas.annotations.Parameter(description = "Số trang (bắt đầu từ 1)") @RequestParam(defaultValue = "1") Integer page,
            @io.swagger.v3.oas.annotations.Parameter(description = "Số lượng mỗi trang") @RequestParam(defaultValue = "10") Integer limit,
            @io.swagger.v3.oas.annotations.Parameter(description = "Sắp xếp theo (createdAt, rating)") @RequestParam(defaultValue = "createdAt") String sortBy,
            @io.swagger.v3.oas.annotations.Parameter(description = "Kiểu sắp xếp (asc/desc)") @RequestParam(defaultValue = "desc") String sortType
    ) {
        String userId = ((com.example.cellex.models.user.User) userDetails).getId();
        int pageNumber = Math.max(page - 1, 0);
        org.springframework.data.domain.Sort.Direction direction = "asc".equalsIgnoreCase(sortType)
                ? org.springframework.data.domain.Sort.Direction.ASC
                : org.springframework.data.domain.Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(pageNumber, limit, org.springframework.data.domain.Sort.by(direction, sortBy));
        PageResponse<ReviewResponse> reviews = reviewService.getUserReviews(userId, pageable);

        return ResponseEntity.ok(ApiResponse.<PageResponse<ReviewResponse>>builder()
                .code(2000)
                .message("Lấy danh sách đánh giá của bạn thành công")
                .result(reviews)
                .build());
    }

    @Operation(summary = "Get review by ID", description = "Get detailed information of a specific review")
    @GetMapping("/{reviewId}")
    public ResponseEntity<ApiResponse<ReviewResponse>> getReviewById(@PathVariable String reviewId) {
        ReviewResponse review = reviewService.getReviewById(reviewId);

        return ResponseEntity.ok(ApiResponse.<ReviewResponse>builder()
                .code(2000)
                .message("Lấy thông tin đánh giá thành công")
                .result(review)
                .build());
    }

    @Operation(summary = "Get product review statistics", description = "Get rating distribution and statistics for a product")
    @GetMapping("/product/{productId}/stats")
    public ResponseEntity<ApiResponse<ReviewStatsResponse>> getProductReviewStats(@PathVariable String productId) {
        ReviewStatsResponse stats = reviewService.getProductReviewStats(productId);

        return ResponseEntity.ok(ApiResponse.<ReviewStatsResponse>builder()
                .code(2000)
                .message("Lấy thống kê đánh giá thành công")
                .result(stats)
                .build());
    }
}
