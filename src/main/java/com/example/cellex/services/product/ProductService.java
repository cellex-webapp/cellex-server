package com.example.cellex.services.product;

import com.example.cellex.dtos.request.product.ProductRequest;
import com.example.cellex.dtos.response.PageResponse;
import com.example.cellex.dtos.response.product.ProductResponse;
import com.example.cellex.enums.ShopStatus;
import com.example.cellex.exceptions.AppException;
import com.example.cellex.exceptions.ErrorCode;
import com.example.cellex.models.category.Category;
import com.example.cellex.models.category.CategoryAttribute;
import com.example.cellex.models.product.Product;
import com.example.cellex.models.shop.Shop;
import com.example.cellex.repositories.category.CategoryAttributeRepository;
import com.example.cellex.repositories.category.CategoryRepository;
import com.example.cellex.repositories.product.ProductRepository;
import com.example.cellex.repositories.shop.ShopRepository;
import com.example.cellex.repositories.user.UserRepository;
import com.example.cellex.services.S3Service;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.Optional;
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
    private final UserRepository userRepository;
    private final S3Service s3Service;
    private final ObjectMapper objectMapper;

    public ProductResponse createProduct(String vendorId, ProductRequest request) {
        // Kiểm tra shop của vendor có tồn tại và đã được verify chưa
        Shop shop = shopRepository.findByVendorIdAndStatus(vendorId, ShopStatus.APPROVED)
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

        Shop shop = product.getShopId() != null 
            ? shopRepository.findById(product.getShopId()).orElse(null) 
            : null;
        Category category = product.getCategoryId() != null 
            ? categoryRepository.findById(product.getCategoryId()).orElse(null) 
            : null;

        return mapToResponse(product, shop, category);
    }

    public PageResponse<ProductResponse> getProductsByCategory(String categoryId, Pageable pageable) {
        // Chỉ lấy sản phẩm đã xuất bản
        Page<Product> products = productRepository.findByCategoryIdAndIsPublishedTrue(categoryId, pageable);

        // Map to ProductResponse
        Page<ProductResponse> productResponsePage = products.map(product -> {
            Shop shop = product.getShopId() != null 
                ? shopRepository.findById(product.getShopId()).orElse(null) 
                : null;
            if (shop == null || shop.getStatus() != ShopStatus.APPROVED) {
                return null;
            }
            Category category = product.getCategoryId() != null 
                ? categoryRepository.findById(product.getCategoryId()).orElse(null) 
                : null;
            return mapToResponse(product, shop, category);
        });

        return PageResponse.of(productResponsePage);
    }

    public PageResponse<ProductResponse> getProductsByCategorySlugOrId(String categorySlugOrId, Pageable pageable) {
        // Tìm category theo slug hoặc ID
        Category category = categoryRepository.findBySlug(categorySlugOrId)
                .orElseGet(() -> categoryRepository.findById(categorySlugOrId)
                        .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND)));

        // Lấy sản phẩm theo categoryId
        return getProductsByCategory(category.getId(), pageable);
    }

    public PageResponse<ProductResponse> getProductsByShop(String shopId, Pageable pageable) {
        // Kiểm tra shop phải ở trạng thái APPROVED
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));

        if (shop.getStatus() != ShopStatus.APPROVED) {
            throw new AppException(ErrorCode.SHOP_NOT_VERIFIED);
        }

        Page<Product> products = productRepository.findByShopIdAndIsPublishedTrue(shopId, pageable);
        Page<ProductResponse> productResponsePage = products.map(product -> {
            Category category = product.getCategoryId() != null 
                ? categoryRepository.findById(product.getCategoryId()).orElse(null) 
                : null;
            return mapToResponse(product, shop, category);
        });

        return PageResponse.of(productResponsePage);
    }

    public PageResponse<ProductResponse> searchProducts(String keyword, Pageable pageable) {
        Page<Product> products = productRepository.findByNameContainingIgnoreCaseAndIsPublishedTrue(keyword, pageable);
        Page<ProductResponse> productResponsePage = products.map(this::mapToResponseWithLookup);

        return PageResponse.of(productResponsePage);
    }

    public PageResponse<ProductResponse> getAllProducts(Pageable pageable) {
        Page<Product> products = productRepository.findAllBy(pageable);
        Page<ProductResponse> productResponsePage = products.map(this::mapToResponseWithLookup);

        return PageResponse.of(productResponsePage);
    }

    // GET MY PRODUCTS - Vendor xem tất cả sản phẩm của mình (bao gồm cả chưa xuất bản)
    public PageResponse<ProductResponse> getMyProducts(String vendorId, Pageable pageable) {
        // Tìm shop của vendor
        Shop shop = shopRepository.findByVendorId(vendorId)
                .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));

        // Lấy tất cả sản phẩm của shop (bao gồm cả published và unpublished)
        Page<Product> products = productRepository.findByShopId(shop.getId(), pageable);

        log.info("Vendor {} retrieved {} products from shop {}", vendorId, products.getTotalElements(), shop.getId());

        Page<ProductResponse> productResponsePage = products.map(product -> {
            Category category = categoryRepository.findById(product.getCategoryId()).orElse(null);
            return mapToResponse(product, shop, category);
        });

        return PageResponse.of(productResponsePage);
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

        Category category = product.getCategoryId() != null 
            ? categoryRepository.findById(product.getCategoryId()).orElse(null) 
            : null;
        return mapToResponse(savedProduct, shop, category);
    }

    // Upload images for product
    public ProductResponse uploadProductImages(String vendorId, String productId, MultipartFile[] imageFiles) throws IOException {
        Product product = findProductByVendor(productId, vendorId);

        if (imageFiles == null || imageFiles.length == 0) {
            throw new AppException(ErrorCode.INVALID_INPUT);
        }

        // Upload all images to S3/Cloudinary
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

        Shop shop = product.getShopId() != null 
            ? shopRepository.findById(product.getShopId()).orElse(null) 
            : null;
        Category category = product.getCategoryId() != null 
            ? categoryRepository.findById(product.getCategoryId()).orElse(null) 
            : null;

        return mapToResponse(savedProduct, shop, category);
    }

    // Update/Replace all product images
    public ProductResponse updateProductImages(String vendorId, String productId, MultipartFile[] imageFiles) throws IOException {
        Product product = findProductByVendor(productId, vendorId);

        if (imageFiles == null || imageFiles.length == 0) {
            throw new AppException(ErrorCode.INVALID_INPUT);
        }

        // Delete old images from S3/Cloudinary
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

        Shop shop = product.getShopId() != null 
            ? shopRepository.findById(product.getShopId()).orElse(null) 
            : null;
        Category category = product.getCategoryId() != null 
            ? categoryRepository.findById(product.getCategoryId()).orElse(null) 
            : null;

        return mapToResponse(savedProduct, shop, category);
    }

    // CREATE MULTIPART - Phương thức mới để hỗ trợ multipart form data
    public ProductResponse createProductMultipart(String vendorId, String categoryId, String name, String description,
                                                 String price, String saleOff, String stockQuantity,
                                                 String attributeValues, String isPublished,
                                                 MultipartFile[] images) throws IOException {
        log.info("Creating product for vendorId: {}, categoryId: {}", vendorId, categoryId);

        // Kiểm tra shop của vendor có tồn tại không
        Optional<Shop> shopOptional = shopRepository.findByVendorId(vendorId);
        if (shopOptional.isEmpty()) {
            log.error("No shop found for vendorId: {}", vendorId);
            throw new AppException(ErrorCode.SHOP_NOT_FOUND);
        }

        Shop shop = shopOptional.get();
        log.info("Found shop: {} for vendorId: {}, status: {}", shop.getId(), vendorId, shop.getStatus());

        // Kiểm tra shop đã được verify chưa
        if (shop.getStatus() != ShopStatus.APPROVED) {
            log.error("Shop {} is not approved for vendorId: {}, current status: {}", shop.getId(), vendorId, shop.getStatus());
            throw new AppException(ErrorCode.SHOP_NOT_VERIFIED);
        }

        // Kiểm tra category có tồn tại không
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> {
                    log.error("Category not found: {}", categoryId);
                    return new AppException(ErrorCode.CATEGORY_NOT_EXISTED);
                });

        log.info("Found category: {} - {}", category.getId(), category.getName());

        // Parse và validate các giá trị
        Double priceValue;
        Double saleOffValue;
        Integer stockQuantityValue;
        Boolean isPublishedValue;

        try {
            priceValue = Double.parseDouble(price);
            saleOffValue = saleOff != null && !saleOff.trim().isEmpty() ? Double.parseDouble(saleOff) : 0.0;
            stockQuantityValue = Integer.parseInt(stockQuantity);
            isPublishedValue = isPublished != null && !isPublished.trim().isEmpty() ? Boolean.parseBoolean(isPublished) : false;
        } catch (NumberFormatException e) {
            log.error("Invalid number format in product data: price={}, saleOff={}, stockQuantity={}",
                     price, saleOff, stockQuantity, e);
            throw new AppException(ErrorCode.INVALID_INPUT);
        }

        // Validate giá trị
        if (priceValue <= 0) {
            throw new AppException(ErrorCode.INVALID_INPUT);
        }
        if (saleOffValue < 0 || saleOffValue > 100) {
            throw new AppException(ErrorCode.INVALID_INPUT);
        }
        if (stockQuantityValue < 0) {
            throw new AppException(ErrorCode.INVALID_INPUT);
        }

        // Tính final price
        Double finalPrice = priceValue;
        if (saleOffValue > 0) {
            finalPrice = priceValue * (100 - saleOffValue) / 100;
        }

        // Upload images nếu có
        List<String> imageUrls = new ArrayList<>();
        if (images != null && images.length > 0) {
            log.info("Uploading {} images for product", images.length);
            for (MultipartFile image : images) {
                if (!image.isEmpty()) {
                    try {
                        String imageUrl = s3Service.uploadFile(image, "products");
                        imageUrls.add(imageUrl);
                        log.debug("Uploaded image: {}", imageUrl);
                    } catch (Exception e) {
                        log.error("Failed to upload image: {}", image.getOriginalFilename(), e);
                        throw new AppException(ErrorCode.FILE_UPLOAD_FAILED);
                    }
                }
            }
        }

        // Parse attribute values nếu có
        List<Product.ProductAttributeValue> attributeValueList = new ArrayList<>();
        if (attributeValues != null && !attributeValues.trim().isEmpty()) {
            try {
                attributeValueList = parseAttributeValues(attributeValues);
                log.info("Parsed {} attribute values", attributeValueList.size());
            } catch (Exception e) {
                log.error("Failed to parse attribute values: {}", attributeValues, e);
                throw new AppException(ErrorCode.INVALID_ATTRIBUTE_VALUE);
            }
        }

        // Tạo product
        Product product = Product.builder()
                .shopId(shop.getId())
                .categoryId(categoryId)
                .name(name)
                .description(description)
                .images(imageUrls)
                .price(priceValue)
                .saleOff(saleOffValue)
                .finalPrice(finalPrice)
                .stockQuantity(stockQuantityValue)
                .attributeValues(attributeValueList)
                .isPublished(isPublishedValue)
                .averageRating(0.0)
                .reviewCount(0)
                .purchaseCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Product savedProduct = productRepository.save(product);
        log.info("Created product: {} for shop: {}", savedProduct.getId(), shop.getId());

        return mapToProductResponse(savedProduct, shop, category);
    }

    // UPDATE MULTIPART - Phương thức mới để hỗ trợ multipart form data
    public ProductResponse updateProductMultipart(String vendorId, String productId, String categoryId, String name,
                                                 String description, String price, String saleOff, String stockQuantity,
                                                 String attributeValues, String isPublished,
                                                 MultipartFile[] images) throws IOException {
        log.info("Updating product {} for vendorId: {}", productId, vendorId);

        // Tìm và kiểm tra quyền sở hữu product
        Product product = findProductByVendor(productId, vendorId);
        log.info("Found product: {} belongs to shop: {}", product.getId(), product.getShopId());

        // Update category nếu có
        if (categoryId != null && !categoryId.trim().isEmpty()) {
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> {
                        log.error("Category not found: {}", categoryId);
                        return new AppException(ErrorCode.CATEGORY_NOT_EXISTED);
                    });
            product.setCategoryId(categoryId);
            log.info("Updated category to: {}", categoryId);
        }

        // Update tên nếu có
        if (name != null && !name.trim().isEmpty()) {
            product.setName(name);
            log.info("Updated name to: {}", name);
        }

        // Update mô tả nếu có
        if (description != null) {
            product.setDescription(description);
            log.info("Updated description");
        }

        // Update giá nếu có
        if (price != null && !price.trim().isEmpty()) {
            try {
                Double priceValue = Double.parseDouble(price);
                if (priceValue <= 0) {
                    throw new AppException(ErrorCode.INVALID_INPUT);
                }
                product.setPrice(priceValue);

                // Tính lại final price
                Double saleOffValue = product.getSaleOff() != null ? product.getSaleOff() : 0.0;
                Double finalPrice = priceValue;
                if (saleOffValue > 0) {
                    finalPrice = priceValue * (100 - saleOffValue) / 100;
                }
                product.setFinalPrice(finalPrice);
                log.info("Updated price to: {}, finalPrice: {}", priceValue, finalPrice);
            } catch (NumberFormatException e) {
                log.error("Invalid price format: {}", price, e);
                throw new AppException(ErrorCode.INVALID_INPUT);
            }
        }

        // Update sale off nếu có
        if (saleOff != null && !saleOff.trim().isEmpty()) {
            try {
                Double saleOffValue = Double.parseDouble(saleOff);
                if (saleOffValue < 0 || saleOffValue > 100) {
                    throw new AppException(ErrorCode.INVALID_INPUT);
                }
                product.setSaleOff(saleOffValue);

                // Tính lại final price
                Double priceValue = product.getPrice();
                Double finalPrice = priceValue;
                if (saleOffValue > 0) {
                    finalPrice = priceValue * (100 - saleOffValue) / 100;
                }
                product.setFinalPrice(finalPrice);
                log.info("Updated saleOff to: {}%, finalPrice: {}", saleOffValue, finalPrice);
            } catch (NumberFormatException e) {
                log.error("Invalid saleOff format: {}", saleOff, e);
                throw new AppException(ErrorCode.INVALID_INPUT);
            }
        }

        // Update stock quantity nếu có
        if (stockQuantity != null && !stockQuantity.trim().isEmpty()) {
            try {
                Integer stockQuantityValue = Integer.parseInt(stockQuantity);
                if (stockQuantityValue < 0) {
                    throw new AppException(ErrorCode.INVALID_INPUT);
                }
                product.setStockQuantity(stockQuantityValue);
                log.info("Updated stockQuantity to: {}", stockQuantityValue);
            } catch (NumberFormatException e) {
                log.error("Invalid stockQuantity format: {}", stockQuantity, e);
                throw new AppException(ErrorCode.INVALID_INPUT);
            }
        }

        // Update attribute values nếu có
        if (attributeValues != null && !attributeValues.trim().isEmpty()) {
            try {
                List<Product.ProductAttributeValue> attributeValueList = parseAttributeValues(attributeValues);
                product.setAttributeValues(attributeValueList);
                log.info("Updated {} attribute values", attributeValueList.size());
            } catch (Exception e) {
                log.error("Failed to parse attribute values: {}", attributeValues, e);
                throw new AppException(ErrorCode.INVALID_ATTRIBUTE_VALUE);
            }
        }

        // Update published status nếu có
        if (isPublished != null && !isPublished.trim().isEmpty()) {
            Boolean isPublishedValue = Boolean.parseBoolean(isPublished);
            product.setIsPublished(isPublishedValue);
            log.info("Updated isPublished to: {}", isPublishedValue);
        }

        // Update images nếu có
        if (images != null && images.length > 0) {
            log.info("Updating {} images for product", images.length);

            // Xóa ảnh cũ từ S3/Cloudinary
            if (product.getImages() != null && !product.getImages().isEmpty()) {
                for (String oldImageUrl : product.getImages()) {
                    try {
                        s3Service.deleteFile(oldImageUrl);
                        log.debug("Deleted old image: {}", oldImageUrl);
                    } catch (Exception e) {
                        log.warn("Failed to delete old image: {}", oldImageUrl, e);
                    }
                }
            }

            // Upload ảnh mới
            List<String> imageUrls = new ArrayList<>();
            for (MultipartFile image : images) {
                if (!image.isEmpty()) {
                    try {
                        String imageUrl = s3Service.uploadFile(image, "products");
                        imageUrls.add(imageUrl);
                        log.debug("Uploaded new image: {}", imageUrl);
                    } catch (Exception e) {
                        log.error("Failed to upload image: {}", image.getOriginalFilename(), e);
                        throw new AppException(ErrorCode.FILE_UPLOAD_FAILED);
                    }
                }
            }
            product.setImages(imageUrls);
            log.info("Updated {} images successfully", imageUrls.size());
        }

        product.setUpdatedAt(LocalDateTime.now());
        Product updatedProduct = productRepository.save(product);
        log.info("Product updated successfully with ID: {}", updatedProduct.getId());

        return mapToProductResponse(updatedProduct);
    }

    // Helper method để map Product thành ProductResponse với shop và category info
    private ProductResponse mapToProductResponse(Product product, Shop shop, Category category) {
        return mapToResponse(product, shop, category);
    }

    // Helper method để map Product thành ProductResponse (overload không cần shop và category)
    private ProductResponse mapToProductResponse(Product product) {
        Shop shop = product.getShopId() != null 
            ? shopRepository.findById(product.getShopId()).orElse(null) 
            : null;
        Category category = product.getCategoryId() != null 
            ? categoryRepository.findById(product.getCategoryId()).orElse(null) 
            : null;
        return mapToResponse(product, shop, category);
    }

    // Helper method để parse attribute values từ JSON string với support cho nhiều attributes
    private List<Product.ProductAttributeValue> parseAttributeValues(String attributeValuesJson) {
        List<Product.ProductAttributeValue> attributeValueList = new ArrayList<>();

        if (attributeValuesJson == null || attributeValuesJson.trim().isEmpty()) {
            return attributeValueList;
        }

        try {
            // Parse JSON string thành List của Map để xử linh hoạt
            // Format expected: [{"attributeId": "id1", "value": "value1"}, {"attributeId": "id2", "value": "value2"}]
            List<Map<String, String>> attributeMaps = objectMapper.readValue(
                    attributeValuesJson,
                    new TypeReference<List<Map<String, String>>>() {}
            );

            for (Map<String, String> attrMap : attributeMaps) {
                String attributeId = attrMap.get("attributeId");
                String value = attrMap.get("value");

                if (attributeId != null && value != null && !value.trim().isEmpty()) {
                    // Lấy thông tin attribute từ database
                    CategoryAttribute attribute = categoryAttributeRepository.findById(attributeId)
                            .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_ATTRIBUTE_NOT_FOUND));

                    // Validate giá trị
                    validateAttributeValue(attribute, value);

                    // Tạo ProductAttributeValue
                    Product.ProductAttributeValue productAttrValue = Product.ProductAttributeValue.builder()
                            .attributeId(attribute.getId())
                            .attributeKey(attribute.getAttributeKey())
                            .attributeName(attribute.getAttributeName())
                            .value(value.trim())
                            .unit(attribute.getUnit())
                            .dataType(attribute.getDataType())
                            .build();

                    attributeValueList.add(productAttrValue);
                }
            }

            log.debug("Parsed {} attribute values from JSON", attributeValueList.size());

        } catch (JsonProcessingException e) {
            log.error("Failed to parse attribute values JSON: {}", attributeValuesJson, e);
            throw new AppException(ErrorCode.INVALID_ATTRIBUTE_VALUE);
        }

        return attributeValueList;
    }

    // Alternative method để parse attribute values từ key-value pairs format
    private List<Product.ProductAttributeValue> parseAttributeValuesFromKeyValue(String attributeValuesJson) {
        List<Product.ProductAttributeValue> attributeValueList = new ArrayList<>();

        if (attributeValuesJson == null || attributeValuesJson.trim().isEmpty()) {
            return attributeValueList;
        }

        try {
            // Parse JSON string thành Map để xử lý format: {"attributeId1": "value1", "attributeId2": "value2"}
            Map<String, String> attributeMap = objectMapper.readValue(
                    attributeValuesJson,
                    new TypeReference<Map<String, String>>() {}
            );

            for (Map.Entry<String, String> entry : attributeMap.entrySet()) {
                String attributeId = entry.getKey();
                String value = entry.getValue();

                if (value != null && !value.trim().isEmpty()) {
                    // Lấy thông tin attribute từ database
                    CategoryAttribute attribute = categoryAttributeRepository.findById(attributeId)
                            .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_ATTRIBUTE_NOT_FOUND));

                    // Validate giá trị
                    validateAttributeValue(attribute, value);

                    // Tạo ProductAttributeValue
                    Product.ProductAttributeValue productAttrValue = Product.ProductAttributeValue.builder()
                            .attributeId(attribute.getId())
                            .attributeKey(attribute.getAttributeKey())
                            .attributeName(attribute.getAttributeName())
                            .value(value.trim())
                            .unit(attribute.getUnit())
                            .dataType(attribute.getDataType())
                            .build();

                    attributeValueList.add(productAttrValue);
                }
            }

            log.debug("Parsed {} attribute values from key-value JSON", attributeValueList.size());

        } catch (JsonProcessingException e) {
            log.error("Failed to parse attribute values key-value JSON: {}", attributeValuesJson, e);
            throw new AppException(ErrorCode.INVALID_ATTRIBUTE_VALUE);
        }

        return attributeValueList;
    }

    // Helper method để tìm product và kiểm tra quyền sở hữu
    private Product findProductByVendor(String productId, String vendorId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        // Kiểm tra quyền sở hữu thông qua shop
        Shop shop = shopRepository.findById(product.getShopId())
                .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));

        if (!shop.getVendorId().equals(vendorId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        return product;
    }


    private void validateRequiredAttributes(List<CategoryAttribute> requiredAttributes,
                                          List<ProductRequest.ProductAttributeValueRequest> providedValues) {
        if (providedValues == null || providedValues.isEmpty()) {
            if (!requiredAttributes.isEmpty()) {
                throw new AppException(ErrorCode.REQUIRED_ATTRIBUTE_MISSING);
            }
            return;
        }

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

        if (requestValues == null || requestValues.isEmpty()) {
            return new ArrayList<>();
        }

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

        // Map attribute values - convert to correct response type
        if (product.getAttributeValues() != null && !product.getAttributeValues().isEmpty()) {
            List<ProductResponse.ProductAttributeValueResponse> attributeResponses =
                product.getAttributeValues().stream()
                    .map(attr -> ProductResponse.ProductAttributeValueResponse.builder()
                            .attributeId(attr.getAttributeId())
                            .attributeKey(attr.getAttributeKey())
                            .attributeName(attr.getAttributeName())
                            .value(attr.getValue())
                            .unit(attr.getUnit())
                            .dataType(attr.getDataType())
                            .build())
                    .collect(Collectors.toList());
            builder.attributeValues(attributeResponses);
        }

        // Map shop info nếu có - với tất cả các trường từ ShopResponse
        if (shop != null) {
            ProductResponse.ShopInfo.ShopInfoBuilder shopInfoBuilder = ProductResponse.ShopInfo.builder()
                    .id(shop.getId())
                    .vendorId(shop.getVendorId())
                    .shopName(shop.getShopName())
                    .description(shop.getDescription())
                    .logoUrl(shop.getLogoUrl())
                    .phoneNumber(shop.getPhoneNumber())
                    .email(shop.getEmail())
                    .status(shop.getStatus())
                    .rating(shop.getRating())
                    .rejectionReason(shop.getRejectionReason())
                    .createdAt(shop.getCreatedAt())
                    .updatedAt(shop.getUpdatedAt());

            // Map address nếu có
            if (shop.getAddress() != null) {
                shopInfoBuilder.address(ProductResponse.AddressInfo.builder()
                        .street(shop.getAddress().getStreet())
                        .commune(shop.getAddress().getCommune())
                        .province(shop.getAddress().getProvince())
                        .country(shop.getAddress().getCountry())
                        .fullAddress(shop.getAddress().getFullAddress())
                        .isDefault(shop.getAddress().isDefault())
                        .build());
            }

            builder.shopInfo(shopInfoBuilder.build());
        }

        // Map category info nếu có - với tất cả các trường từ CategoryResponse
        if (category != null) {
            builder.categoryInfo(ProductResponse.CategoryInfo.builder()
                    .id(category.getId())
                    .name(category.getName())
                    .slug(category.getSlug())
                    .parentId(category.getParentId())
                    .imageUrl(category.getImageUrl())
                    .description(category.getDescription())
                    .isActive(category.getIsActive())
                    .build());
        }

        return builder.build();
    }

    // New method to map Product to ProductResponse with lookup for shop and category
    private ProductResponse mapToResponseWithLookup(Product product) {
        // Lấy thông tin shop và category
        Shop shop = product.getShopId() != null 
            ? shopRepository.findById(product.getShopId()).orElse(null) 
            : null;
        Category category = product.getCategoryId() != null 
            ? categoryRepository.findById(product.getCategoryId()).orElse(null) 
            : null;

        return mapToResponse(product, shop, category);
    }
}
