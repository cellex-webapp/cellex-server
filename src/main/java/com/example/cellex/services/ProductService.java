package com.example.cellex.services;

import com.example.cellex.dtos.request.ProductRequest;
import com.example.cellex.dtos.response.ProductResponse;
import com.example.cellex.exceptions.AppException;
import com.example.cellex.exceptions.ErrorCode;
import com.example.cellex.models.Category;
import com.example.cellex.models.CategoryAttribute;
import com.example.cellex.models.Product;
import com.example.cellex.models.Shop;
import com.example.cellex.repositories.CategoryAttributeRepository;
import com.example.cellex.repositories.CategoryRepository;
import com.example.cellex.repositories.ProductRepository;
import com.example.cellex.repositories.ShopRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final ShopRepository shopRepository;
    private final CategoryRepository categoryRepository;
    private final CategoryAttributeRepository categoryAttributeRepository;
    private final S3Service s3Service; // Assuming you have an S3Service for file uploads

    public ProductResponse createProduct(String vendorId, ProductRequest request) {
        // Kiểm tra shop của vendor có tồn tại và đã được verify chưa
        Shop shop = shopRepository.findByVendorIdAndIsVerifiedTrue(vendorId)
                .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND_OR_NOT_VERIFIED));

        // Kiểm tra category có tồn tại không
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_EXISTED));

        // Lấy danh sách thuộc tính bắt buộc của category
        List<CategoryAttribute> requiredAttributes = categoryAttributeRepository
                .findByCategoryIdAndIsActiveTrueOrderBySortOrderAsc(request.getCategoryId())
                .stream()
                .filter(CategoryAttribute::getIsRequired)
                .collect(Collectors.toList());

        // Validate các thuộc tính bắt buộc đã được cung cấp
        validateRequiredAttributes(requiredAttributes, request.getAttributeValues());

        // Tính final price
        Double finalPrice = request.getPrice();
        if (request.getSaleOff() != null && request.getSaleOff() > 0) {
            finalPrice = request.getPrice() * (100 - request.getSaleOff()) / 100;
        }

        // Validate và map attribute values
        List<Product.ProductAttributeValue> attributeValues = validateAndMapAttributeValues(
                request.getCategoryId(), request.getAttributeValues());

        Product product = Product.builder()
                .shopId(shop.getId())
                .categoryId(request.getCategoryId())
                .name(request.getName())
                .description(request.getDescription())
                .images(List.of()) // Khởi tạo danh sách ảnh trống, sẽ upload riêng sau
                .price(request.getPrice())
                .saleOff(request.getSaleOff())
                .finalPrice(finalPrice)
                .stockQuantity(request.getStockQuantity())
                .attributeValues(attributeValues)
                .isPublished(false) // Mặc định chưa xuất bản, cần vendor tự xuất bản
                .build();

        Product savedProduct = productRepository.save(product);
        log.info("Created product: {} for shop: {}", savedProduct.getId(), shop.getId());

        return mapToResponse(savedProduct, shop, category);
    }

    public ProductResponse getProductById(String productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        Shop shop = shopRepository.findById(product.getShopId()).orElse(null);
        Category category = categoryRepository.findById(product.getCategoryId()).orElse(null);

        return mapToResponse(product, shop, category);
    }

    public Page<ProductResponse> getProductsByCategory(String categoryId, Pageable pageable) {
        Page<Product> products = productRepository.findByCategoryIdAndIsPublishedTrue(categoryId, pageable);
        return products.map(product -> mapToResponse(product, null, null));
    }

    public Page<ProductResponse> getProductsByShop(String shopId, Pageable pageable) {
        Page<Product> products = productRepository.findByShopIdAndIsPublishedTrue(shopId, pageable);
        return products.map(product -> mapToResponse(product, null, null));
    }

    public Page<ProductResponse> searchProducts(String keyword, Pageable pageable) {
        Page<Product> products = productRepository.findByNameContainingIgnoreCaseAndIsPublishedTrue(keyword, pageable);
        return products.map(product -> mapToResponse(product, null, null));
    }

    public ProductResponse updateProduct(String vendorId, String productId, ProductRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        // Kiểm tra quyền sở hữu
        Shop shop = shopRepository.findById(product.getShopId())
                .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));

        if (!shop.getVendorId().equals(vendorId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        // Validate attributes nếu category thay đổi
        if (!product.getCategoryId().equals(request.getCategoryId())) {
            List<CategoryAttribute> requiredAttributes = categoryAttributeRepository
                    .findByCategoryIdAndIsActiveTrueOrderBySortOrderAsc(request.getCategoryId())
                    .stream()
                    .filter(CategoryAttribute::getIsRequired)
                    .collect(Collectors.toList());
            validateRequiredAttributes(requiredAttributes, request.getAttributeValues());
        }

        // Tính lại final price
        Double finalPrice = request.getPrice();
        if (request.getSaleOff() != null && request.getSaleOff() > 0) {
            finalPrice = request.getPrice() * (100 - request.getSaleOff()) / 100;
        }

        // Validate và map attribute values
        List<Product.ProductAttributeValue> attributeValues = validateAndMapAttributeValues(
                request.getCategoryId(), request.getAttributeValues());

        // Update product
        product.setCategoryId(request.getCategoryId());
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setSaleOff(request.getSaleOff());
        product.setFinalPrice(finalPrice);
        product.setStockQuantity(request.getStockQuantity());
        product.setAttributeValues(attributeValues);
        product.setUpdatedAt(LocalDateTime.now());

        Product savedProduct = productRepository.save(product);
        log.info("Updated product: {}", productId);

        Category category = categoryRepository.findById(request.getCategoryId()).orElse(null);
        return mapToResponse(savedProduct, shop, category);
    }

    public void deleteProduct(String vendorId, String productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        Shop shop = shopRepository.findById(product.getShopId())
                .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));

        if (!shop.getVendorId().equals(vendorId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        productRepository.delete(product);
        log.info("Deleted product: {}", productId);
    }

    public ProductResponse togglePublishStatus(String vendorId, String productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        Shop shop = shopRepository.findById(product.getShopId())
                .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));

        if (!shop.getVendorId().equals(vendorId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        product.setIsPublished(!product.getIsPublished());
        product.setUpdatedAt(LocalDateTime.now());

        Product savedProduct = productRepository.save(product);
        log.info("Toggled publish status for product: {} to {}", productId, savedProduct.getIsPublished());

        Category category = categoryRepository.findById(product.getCategoryId()).orElse(null);
        return mapToResponse(savedProduct, shop, category);
    }

    // Upload images for product
    public ProductResponse uploadProductImages(String vendorId, String productId, MultipartFile[] imageFiles) throws IOException {
        Product product = getProductAndValidateOwnership(vendorId, productId);

        if (imageFiles == null || imageFiles.length == 0) {
            throw new AppException(ErrorCode.INVALID_INPUT);
        }

        // Upload all images to Cloudinary
        List<String> imageUrls = new ArrayList<>();
        for (MultipartFile file : imageFiles) {
            if (!file.isEmpty()) {
                String imageUrl = s3Service.uploadFile(file, "products");
                imageUrls.add(imageUrl);
            }
        }

        // Add new images to existing ones
        List<String> currentImages = product.getImages() != null ? new ArrayList<>(product.getImages()) : new ArrayList<>();
        currentImages.addAll(imageUrls);
        product.setImages(currentImages);

        Product savedProduct = productRepository.save(product);

        Shop shop = shopRepository.findById(product.getShopId()).orElse(null);
        Category category = categoryRepository.findById(product.getCategoryId()).orElse(null);

        return mapToResponse(savedProduct, shop, category);
    }

    // Update/Replace all product images
    public ProductResponse updateProductImages(String vendorId, String productId, MultipartFile[] imageFiles) throws IOException {
        Product product = getProductAndValidateOwnership(vendorId, productId);

        if (imageFiles == null || imageFiles.length == 0) {
            throw new AppException(ErrorCode.INVALID_INPUT);
        }

        // Delete old images from Cloudinary
        if (product.getImages() != null && !product.getImages().isEmpty()) {
            for (String oldImageUrl : product.getImages()) {
                try {
                    s3Service.deleteFile(oldImageUrl);
                } catch (Exception e) {
                    log.warn("Failed to delete old image: {}", oldImageUrl, e);
                }
            }
        }

        // Upload new images
        List<String> imageUrls = new ArrayList<>();
        for (MultipartFile file : imageFiles) {
            if (!file.isEmpty()) {
                String imageUrl = s3Service.uploadFile(file, "products");
                imageUrls.add(imageUrl);
            }
        }

        product.setImages(imageUrls);
        Product savedProduct = productRepository.save(product);

        Shop shop = shopRepository.findById(product.getShopId()).orElse(null);
        Category category = categoryRepository.findById(product.getCategoryId()).orElse(null);

        return mapToResponse(savedProduct, shop, category);
    }

    // Helper method to get product and validate ownership
    private Product getProductAndValidateOwnership(String vendorId, String productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        // Kiểm tra quyền sở hữu thông qua shop
        Shop shop = shopRepository.findByVendorIdAndIsVerifiedTrue(vendorId)
                .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND_OR_NOT_VERIFIED));

        if (!product.getShopId().equals(shop.getId())) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        return product;
    }

    private void validateRequiredAttributes(List<CategoryAttribute> requiredAttributes,
                                          List<ProductRequest.ProductAttributeValueRequest> providedValues) {
        Map<String, String> providedAttributeMap = providedValues.stream()
                .collect(Collectors.toMap(
                        ProductRequest.ProductAttributeValueRequest::getAttributeId,
                        ProductRequest.ProductAttributeValueRequest::getValue
                ));

        for (CategoryAttribute requiredAttr : requiredAttributes) {
            if (!providedAttributeMap.containsKey(requiredAttr.getId()) ||
                providedAttributeMap.get(requiredAttr.getId()).trim().isEmpty()) {
                throw new AppException(ErrorCode.REQUIRED_ATTRIBUTE_MISSING);
            }
        }
    }

    private List<Product.ProductAttributeValue> validateAndMapAttributeValues(
            String categoryId, List<ProductRequest.ProductAttributeValueRequest> requestValues) {

        // Lấy tất cả attributes của category
        Map<String, CategoryAttribute> attributeMap = categoryAttributeRepository
                .findByCategoryIdAndIsActiveTrueOrderBySortOrderAsc(categoryId)
                .stream()
                .collect(Collectors.toMap(CategoryAttribute::getId, Function.identity()));

        return requestValues.stream().map(requestValue -> {
            CategoryAttribute attribute = attributeMap.get(requestValue.getAttributeId());
            if (attribute == null) {
                throw new AppException(ErrorCode.CATEGORY_ATTRIBUTE_NOT_FOUND);
            }

            // Validate giá trị theo dataType và validation pattern
            validateAttributeValue(attribute, requestValue.getValue());

            return Product.ProductAttributeValue.builder()
                    .attributeId(attribute.getId())
                    .attributeKey(attribute.getAttributeKey())
                    .attributeName(attribute.getAttributeName())
                    .value(requestValue.getValue())
                    .unit(attribute.getUnit())
                    .dataType(attribute.getDataType())
                    .build();
        }).collect(Collectors.toList());
    }

    private void validateAttributeValue(CategoryAttribute attribute, String value) {
        // Validate theo dataType
        switch (attribute.getDataType()) {
            case "NUMBER":
                try {
                    Double.parseDouble(value);
                } catch (NumberFormatException e) {
                    throw new AppException(ErrorCode.INVALID_ATTRIBUTE_VALUE);
                }
                break;
            case "BOOLEAN":
                if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
                    throw new AppException(ErrorCode.INVALID_ATTRIBUTE_VALUE);
                }
                break;
            case "SELECT":
                if (attribute.getSelectOptions() != null &&
                    !attribute.getSelectOptions().contains(value)) {
                    throw new AppException(ErrorCode.INVALID_ATTRIBUTE_VALUE);
                }
                break;
        }

        // Validate theo pattern nếu có
        if (attribute.getValidationPattern() != null && !attribute.getValidationPattern().isEmpty()) {
            Pattern pattern = Pattern.compile(attribute.getValidationPattern());
            if (!pattern.matcher(value).matches()) {
                throw new AppException(ErrorCode.INVALID_ATTRIBUTE_VALUE);
            }
        }
    }

    private ProductResponse mapToResponse(Product product, Shop shop, Category category) {
        ProductResponse.ProductResponseBuilder builder = ProductResponse.builder()
                .id(product.getId())
                .shopId(product.getShopId())
                .categoryId(product.getCategoryId())
                .name(product.getName())
                .description(product.getDescription())
                .images(product.getImages())
                .price(product.getPrice())
                .saleOff(product.getSaleOff())
                .finalPrice(product.getFinalPrice())
                .stockQuantity(product.getStockQuantity())
                .averageRating(product.getAverageRating())
                .reviewCount(product.getReviewCount())
                .purchaseCount(product.getPurchaseCount())
                .isPublished(product.getIsPublished())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt());

        // Map attribute values
        if (product.getAttributeValues() != null) {
            List<ProductResponse.ProductAttributeValueResponse> attributeResponses =
                    product.getAttributeValues().stream().map(attr ->
                ProductResponse.ProductAttributeValueResponse.builder()
                        .attributeId(attr.getAttributeId())
                        .attributeKey(attr.getAttributeKey())
                        .attributeName(attr.getAttributeName())
                        .value(attr.getValue())
                        .unit(attr.getUnit())
                        .dataType(attr.getDataType())
                        .build()).collect(Collectors.toList());
            builder.attributeValues(attributeResponses);
        }

        // Map shop info nếu có
        if (shop != null) {
            builder.shopInfo(ProductResponse.ShopInfo.builder()
                    .id(shop.getId())
                    .shopName(shop.getShopName())
                    .logoUrl(shop.getLogoUrl())
                    .isVerified(shop.getIsVerified())
                    .rating(shop.getRating())
                    .build());
        }

        // Map category info nếu có
        if (category != null) {
            builder.categoryInfo(ProductResponse.CategoryInfo.builder()
                    .id(category.getId())
                    .name(category.getName())
                    .imageUrl(category.getImageUrl())
                    .build());
        }

        return builder.build();
    }
}
