package com.example.cellex.examples;

/**
 * HƯỚNG DẪN TÍCH HỢP USER INTERACTION TRACKING
 * 
 * File này chứa các ví dụ về cách tích hợp tracking vào các service hiện có.
 * Không cần compile file này, chỉ để tham khảo.
 */
public class InteractionTrackingIntegrationExample {

    /**
     * EXAMPLE 1: Track Product View
     * 
     * Tích hợp vào ProductController hoặc ProductService
     * Khi user xem chi tiết sản phẩm
     */
    void exampleTrackProductView() {
        // Trong ProductController
        /*
        @Autowired
        private UserInteractionService userInteractionService;
        
        @GetMapping("/{productId}")
        public ResponseEntity<ApiResponse<ProductResponse>> getProductById(
                @PathVariable String productId,
                @AuthenticationPrincipal UserDetails userDetails) {
            
            // Lấy product
            ProductResponse product = productService.getProductById(productId);
            
            // Track view nếu user đã đăng nhập
            if (userDetails != null) {
                String userId = ((User) userDetails).getId();
                try {
                    userInteractionService.recordView(
                        userId, 
                        productId, 
                        product.getCategoryId()
                    );
                } catch (Exception e) {
                    log.warn("Failed to track product view", e);
                }
            }
            
            return ResponseEntity.ok(ApiResponse.<ProductResponse>builder()
                    .code(2000)
                    .message("Success")
                    .result(product)
                    .build());
        }
        */
    }

    /**
     * EXAMPLE 2: Track Add to Cart
     * 
     * Tích hợp vào CartService
     */
    void exampleTrackAddToCart() {
        // Trong CartService.addToCart()
        /*
        @Autowired
        private UserInteractionService userInteractionService;
        @Autowired
        private ProductRepository productRepository;
        
        public CartResponse addToCart(String userId, AddToCartRequest request) {
            // ... existing code ...
            
            // Track add to cart
            try {
                Product product = productRepository.findById(request.getProductId())
                        .orElse(null);
                        
                if (product != null) {
                    userInteractionService.recordAddToCart(
                        userId,
                        request.getProductId(),
                        product.getCategoryId()
                    );
                }
            } catch (Exception e) {
                log.warn("Failed to track add to cart", e);
            }
            
            // ... existing code ...
            return cartResponse;
        }
        */
    }

    /**
     * EXAMPLE 3: Track Purchase
     * 
     * Tích hợp vào OrderService
     * Khi order được tạo thành công (hoặc khi payment success)
     */
    void exampleTrackPurchase() {
        // Trong OrderService.createOrder() hoặc confirmOrder()
        /*
        @Autowired
        private UserInteractionService userInteractionService;
        @Autowired
        private ProductRepository productRepository;
        
        public OrderResponse createOrder(String userId, CreateOrderRequest request) {
            // ... existing code to create order ...
            
            Order savedOrder = orderRepository.save(order);
            
            // Track purchases cho tất cả items
            try {
                for (OrderItem item : savedOrder.getItems()) {
                    Product product = productRepository.findById(item.getProductId())
                            .orElse(null);
                            
                    if (product != null) {
                        userInteractionService.recordPurchase(
                            userId,
                            item.getProductId(),
                            product.getCategoryId()
                        );
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to track purchases", e);
            }
            
            // ... existing code ...
            return orderResponse;
        }
        */
    }

    /**
     * EXAMPLE 4: Track Review
     * 
     * ĐÃ TÍCH HỢP SẴN trong ReviewService.createReview()
     */
    void exampleTrackReview() {
        // Trong ReviewService.createReview()
        /*
        @Autowired
        private UserInteractionService userInteractionService;
        
        public ReviewResponse createReview(String userId, CreateReviewRequest request) {
            // ... existing code ...
            
            Review review = reviewRepository.save(newReview);
            
            // Track review
            try {
                Product product = productRepository.findById(request.getProductId())
                        .orElse(null);
                        
                if (product != null) {
                    userInteractionService.recordReview(
                        userId,
                        request.getProductId(),
                        product.getCategoryId()
                    );
                }
            } catch (Exception e) {
                log.warn("Failed to track review", e);
            }
            
            // ... existing code ...
            return reviewResponse;
        }
        */
    }

    /**
     * EXAMPLE 5: Track Content-based Similarity cho sản phẩm mới
     * 
     * Tích hợp vào ProductService khi tạo sản phẩm mới
     */
    void exampleTrackNewProduct() {
        // Trong ProductService.createProduct()
        /*
        @Autowired
        private ColdStartService coldStartService;
        
        public ProductResponse createProduct(String vendorId, ProductRequest request) {
            // ... existing code to create product ...
            
            Product savedProduct = productRepository.save(product);
            
            // Tính content-based similarity cho sản phẩm mới (async)
            CompletableFuture.runAsync(() -> {
                try {
                    coldStartService.computeContentBasedSimilarity(savedProduct.getId());
                } catch (Exception e) {
                    log.warn("Failed to compute content-based similarity", e);
                }
            });
            
            // ... existing code ...
            return productResponse;
        }
        */
    }

    /**
     * EXAMPLE 6: OPTIONAL - Track thêm các actions khác
     */
    void exampleTrackOtherActions() {
        /*
        // Track search (nếu cần)
        public void trackSearch(String userId, String query) {
            userInteractionService.recordView(userId, productId, categoryId);
        }
        
        // Track wishlist (nếu có feature)
        public void trackWishlist(String userId, String productId) {
            userInteractionService.recordView(userId, productId, categoryId);
        }
        
        // Track share (nếu có feature)
        public void trackShare(String userId, String productId) {
            userInteractionService.recordView(userId, productId, categoryId);
        }
        */
    }

    /**
     * BEST PRACTICES
     */
    void bestPractices() {
        /*
        1. ALWAYS wrap tracking calls trong try-catch
           - Tracking không được làm fail main business logic
           
        2. Track ASYNCHRONOUSLY khi có thể
           - Dùng @Async hoặc CompletableFuture
           - Không block main thread
           
        3. Track CHỈ KHI user authenticated
           - Check userDetails != null
           - Anonymous users không track
           
        4. VALIDATE dữ liệu trước khi track
           - Product phải tồn tại
           - CategoryId phải valid
           
        5. LOG warnings khi tracking fails
           - Giúp debug issues
           - Không throw exceptions
           
        6. BATCH tracking updates (advanced)
           - Gom nhiều actions lại
           - Reduce DB writes
           
        Example with @Async:
        
        @Service
        public class AsyncInteractionService {
            @Autowired
            private UserInteractionService userInteractionService;
            
            @Async
            public CompletableFuture<Void> trackViewAsync(
                    String userId, String productId, String categoryId) {
                try {
                    userInteractionService.recordView(userId, productId, categoryId);
                } catch (Exception e) {
                    log.warn("Failed to track view async", e);
                }
                return CompletableFuture.completedFuture(null);
            }
        }
        */
    }

    /**
     * TESTING RECOMMENDATIONS
     */
    void testingExample() {
        /*
        // 1. Tạo test data
        // - Tạo users
        // - Tạo products
        // - Tạo interactions (views, carts, purchases, reviews)
        
        // 2. Test Cold-start
        curl -X GET "http://localhost:8080/api/v1/recommendations/user/new_user_id"
        // Expected: Trending/Popular products
        
        // 3. Test CF-based
        curl -X GET "http://localhost:8080/api/v1/recommendations/user/user_with_history_id"
        // Expected: CF-based recommendations
        
        // 4. Test Manual Computation
        curl -X POST "http://localhost:8080/api/v1/recommendations/compute/user_id"
        curl -X POST "http://localhost:8080/api/v1/recommendations/compute-all"
        
        // 5. Check Pre-computed
        curl -X GET "http://localhost:8080/api/v1/recommendations/precomputed/user_id"
        
        // 6. Test với JWT (authenticated user)
        curl -X GET "http://localhost:8080/api/v1/recommendations/me" \
          -H "Authorization: Bearer {your_jwt_token}"
        */
    }
}
