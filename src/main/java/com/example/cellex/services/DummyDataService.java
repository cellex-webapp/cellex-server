package com.example.cellex.services;

import com.example.cellex.enums.*;
import com.example.cellex.models.address.Commune;
import com.example.cellex.models.address.Province;
import com.example.cellex.models.category.Category;
import com.example.cellex.models.category.CategoryAttribute;
import com.example.cellex.models.coupon.CampaignDistributionLog;
import com.example.cellex.models.coupon.CouponCampaign;
import com.example.cellex.models.coupon.SegmentCoupon;
import com.example.cellex.models.coupon.UserCoupon;
import com.example.cellex.models.order.Order;
import com.example.cellex.models.order.OrderItem;
import com.example.cellex.models.product.Product;
import com.example.cellex.models.recommendation.ProductSimilarity;
import com.example.cellex.models.recommendation.Recommendation;
import com.example.cellex.models.recommendation.UserInteraction;
import com.example.cellex.models.review.Review;
import com.example.cellex.models.review.VendorResponse;
import com.example.cellex.models.segment.CustomerSegment;
import com.example.cellex.models.shop.Shop;
import com.example.cellex.models.user.User;
import com.example.cellex.repositories.category.CategoryAttributeRepository;
import com.example.cellex.repositories.category.CategoryRepository;
import com.example.cellex.repositories.coupon.CampaignDistributionLogRepository;
import com.example.cellex.repositories.coupon.CouponCampaignRepository;
import com.example.cellex.repositories.coupon.SegmentCouponRepository;
import com.example.cellex.repositories.coupon.UserCouponRepository;
import com.example.cellex.repositories.order.OrderRepository;
import com.example.cellex.repositories.product.ProductRepository;
import com.example.cellex.repositories.recommendation.ProductSimilarityRepository;
import com.example.cellex.repositories.recommendation.RecommendationRepository;
import com.example.cellex.repositories.recommendation.UserInteractionRepository;
import com.example.cellex.repositories.review.ReviewRepository;
import com.example.cellex.repositories.segment.CustomerSegmentRepository;
import com.example.cellex.repositories.shop.ShopRepository;
import com.example.cellex.repositories.user.UserRepository;
import lombok.RequiredArgsConstructor;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Profile({"default", "dev"})
public class DummyDataService implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ShopRepository shopRepository;
    private final CategoryRepository categoryRepository;
    private final CategoryAttributeRepository categoryAttributeRepository;
    private final ProductRepository productRepository;
    private final CustomerSegmentRepository customerSegmentRepository;
    private final SegmentCouponRepository segmentCouponRepository;
    private final CouponCampaignRepository couponCampaignRepository;
    private final UserCouponRepository userCouponRepository;
    private final CampaignDistributionLogRepository campaignDistributionLogRepository;
    private final OrderRepository orderRepository;
    private final ReviewRepository reviewRepository;
    private final UserInteractionRepository userInteractionRepository;
    private final ProductSimilarityRepository productSimilarityRepository;
    private final RecommendationRepository recommendationRepository;
    private final PasswordEncoder passwordEncoder;
    private final MongoTemplate mongoTemplate;

    private static final String AVATAR_URL = "https://res.cloudinary.com/dr8ez6ua8/image/upload/v1761917096/avatar_pz0phg.avif";
    private static final String CATEGORY_IMAGE_URL = "https://res.cloudinary.com/dr8ez6ua8/image/upload/v1761917097/smartphone_yvztzo.png";
    private static final String PRODUCT_IMAGE_URL = "https://res.cloudinary.com/dr8ez6ua8/image/upload/v1761917096/ihpone_blgear.jpg";

    @Override
    public void run(String... args) {
        // Load địa chỉ từ file JSON
        loadLocations();

         if (hasExistingDummyData()) {
           return;
         }

        // Drop toàn bộ database trước khi seed (khi chưa có dummy data)
        mongoTemplate.getDb().drop();

        List<User> users = seedUsersAndShop();
        List<Shop> shops = shopRepository.findAll();
        Map<String, String> categoryIdByName = seedCategories();
        Map<String, List<CategoryAttribute>> attributesByCategoryId = seedCategoryAttributes(categoryIdByName);
        List<Product> products = seedProducts(attributesByCategoryId);
        Map<String, CustomerSegment> segmentsByName = seedCustomerSegments();
        seedSegmentCoupons(segmentsByName);
        seedCouponCampaign();
        seedUserCoupons();
        
        // Seed important data
        List<Order> orders = seedOrders(users, shops, products);
        seedReviews(users, shops, products, orders);
        seedUserInteractions(users, products);
        seedProductSimilarities(products);
        seedRecommendations(users, products);
    }

    private boolean hasExistingDummyData() {
        boolean hasAdmin = userRepository.findByEmail("admin@gmail.com").isPresent();
        boolean hasVendor = userRepository.findByEmail("vendor@gmail.com").isPresent();
        boolean hasUser = userRepository.findByEmail("user@gmail.com").isPresent();
        boolean hasCategories = categoryRepository.count() > 0;
        boolean hasProducts = productRepository.count() > 0;
        return (hasAdmin && hasVendor && hasUser) || (hasCategories && hasProducts);
    }

    private List<Province> provinces = new ArrayList<>();
    private List<Commune> communes = new ArrayList<>();

    private void loadLocations() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ClassPathResource provincesRes = new ClassPathResource("data/provinces.json");
            ClassPathResource communesRes = new ClassPathResource("data/communes.json");
            if (provincesRes.exists()) {
                provinces = mapper.readValue(provincesRes.getInputStream(), new TypeReference<List<Province>>(){});
            }
            if (communesRes.exists()) {
                communes = mapper.readValue(communesRes.getInputStream(), new TypeReference<List<Commune>>(){});
            }
        } catch (Exception e) {
            // fallback to empty lists; address builders will use defaults
            provinces = new ArrayList<>();
            communes = new ArrayList<>();
        }
    }

    private List<User> seedUsersAndShop() {
        List<User> allUsers = new ArrayList<>();
        
        // Admin
        User admin = userRepository.findByEmail("admin@gmail.com").orElseGet(() -> {
            User u = User.builder()
                .fullName("Admin User")
                .email("admin@gmail.com")
                .password(passwordEncoder.encode("123"))
                .phoneNumber(randomPhone())
                .avatarUrl(AVATAR_URL)
                .role(Role.ADMIN)
                .isActive(true)
                .totalSpendDecimal(java.math.BigDecimal.ZERO)
                .build();
            u.setAddress(sampleUserAddress());
            return userRepository.save(u);
        });
        allUsers.add(admin);

        // Vendor
        User vendor = userRepository.findByEmail("vendor@gmail.com").orElseGet(() -> {
            User u = User.builder()
                .fullName("Vendor User")
                .email("vendor@gmail.com")
                .password(passwordEncoder.encode("123"))
                .phoneNumber(randomPhone())
                .avatarUrl(AVATAR_URL)
                .role(Role.VENDOR)
                .isActive(true)
                .totalSpendDecimal(java.math.BigDecimal.ZERO)
                .build();
            u.setAddress(sampleUserAddress());
            return userRepository.save(u);
        });
        allUsers.add(vendor);

        // Normal User
        User normalUser = userRepository.findByEmail("user@gmail.com").orElseGet(() -> {
            User u = User.builder()
                .fullName("Normal User")
                .email("user@gmail.com")
                .password(passwordEncoder.encode("123"))
                .phoneNumber(randomPhone())
                .avatarUrl(AVATAR_URL)
                .role(Role.USER)
                .isActive(true)
                .totalSpendDecimal(java.math.BigDecimal.valueOf(randomBetween(0, 5_000_000)))
                .build();
            u.setAddress(sampleUserAddress());
            return userRepository.save(u);
        });
        allUsers.add(normalUser);

        // Create additional vendors
        String[] vendorNames = {"TechStore Hà Nội", "Điện Máy Xanh", "CellphoneS", "FPT Shop", "Thế Giới Di Động"};
        List<User> vendors = new ArrayList<>();
        vendors.add(vendor);
        
        for (int i = 0; i < vendorNames.length - 1; i++) {
            String email = "vendor" + (i + 2) + "@gmail.com";
            int finalI = i;
            User v = userRepository.findByEmail(email).orElseGet(() -> {
                User u = User.builder()
                    .fullName("Vendor " + (finalI + 2))
                    .email(email)
                    .password(passwordEncoder.encode("123"))
                    .phoneNumber(randomPhone())
                    .avatarUrl(AVATAR_URL)
                    .role(Role.VENDOR)
                    .isActive(true)
                    .totalSpendDecimal(java.math.BigDecimal.ZERO)
                    .build();
                u.setAddress(sampleUserAddress());
                return userRepository.save(u);
            });
            vendors.add(v);
            allUsers.add(v);
        }

        // Create additional normal users (20-25 users)
        String[] userPrefixes = {"Nguyễn Văn", "Trần Thị", "Lê Văn", "Phạm Thị", "Hoàng Văn", "Vũ Thị", "Đặng Văn", "Bùi Thị"};
        String[] userSuffixes = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J"};
        
        for (int i = 0; i < 20; i++) {
            String prefix = userPrefixes[i % userPrefixes.length];
            String suffix = userSuffixes[i % userSuffixes.length];
            String email = "user" + (i + 2) + "@gmail.com";
            
            User u2 = userRepository.findByEmail(email).orElseGet(() -> {
                User u = User.builder()
                    .fullName(prefix + " " + suffix)
                    .email(email)
                    .password(passwordEncoder.encode("123"))
                    .phoneNumber(randomPhone())
                    .avatarUrl(AVATAR_URL)
                    .role(Role.USER)
                    .isActive(ThreadLocalRandom.current().nextDouble() > 0.1)
                    .totalSpendDecimal(java.math.BigDecimal.valueOf(randomBetween(0, 10_000_000)))
                    .build();
                u.setAddress(sampleUserAddress());
                return userRepository.save(u);
            });
            allUsers.add(u2);
        }

        // Create shops for all vendors
        for (int i = 0; i < vendors.size(); i++) {
            User v = vendors.get(i);
            if (shopRepository.findAll().stream().noneMatch(s -> Objects.equals(s.getVendorId(), v.getId()))) {
                Shop shop = Shop.builder()
                        .ownerUuid(v.getUuid())
                        .shopName(vendorNames[i])
                        .description("Cửa hàng chuyên " + getShopDescription(i))
                        .logoUrl(AVATAR_URL)
                        .address(sampleShopAddress())
                        .phoneNumber(randomPhone())
                        .email(v.getEmail())
                        .status(ShopStatus.APPROVED)
                        .rating(randomBetween(3.5, 5.0))
                        .build();
                shopRepository.save(shop);
            }
        }
        
        return allUsers;
    }
    
    private String getShopDescription(int index) {
        String[] descriptions = {
                "đồ công nghệ, điện thoại, phụ kiện",
                "điện máy, điện tử, gia dụng",
                "điện thoại di động, tablet, laptop",
                "máy tính, linh kiện, phụ kiện công nghệ",
                "thiết bị di động, phụ kiện thông minh"
        };
        return descriptions[index % descriptions.length];
    }

    private Map<String, String> seedCategories() {
        if (categoryRepository.count() > 0) {
            return categoryRepository.findAll().stream().collect(Collectors.toMap(Category::getName, Category::getId));
        }
        List<Category> categories = new ArrayList<>();
        categories.add(Category.builder().name("Điện thoại").slug(slugify("Điện thoại")).imageUrl(CATEGORY_IMAGE_URL).description("Danh mục điện thoại thông minh").isActive(true).build());
        categories.add(Category.builder().name("Phụ kiện").slug(slugify("Phụ kiện")).imageUrl(CATEGORY_IMAGE_URL).description("Phụ kiện đi kèm thiết bị").isActive(true).build());
        categories.add(Category.builder().name("Máy tính bảng").slug(slugify("Máy tính bảng")).imageUrl(CATEGORY_IMAGE_URL).description("Tablet các loại").isActive(true).build());
        categories.add(Category.builder().name("Laptop").slug(slugify("Laptop")).imageUrl(CATEGORY_IMAGE_URL).description("Máy tính xách tay").isActive(true).build());
        categories.add(Category.builder().name("Đồng hồ thông minh").slug(slugify("Đồng hồ thông minh")).imageUrl(CATEGORY_IMAGE_URL).description("Smartwatch và phụ kiện").isActive(true).build());
        categories.add(Category.builder().name("Tai nghe").slug(slugify("Tai nghe")).imageUrl(CATEGORY_IMAGE_URL).description("Tai nghe có dây và không dây").isActive(true).build());
        categories.add(Category.builder().name("Sạc dự phòng").slug(slugify("Sạc dự phòng")).imageUrl(CATEGORY_IMAGE_URL).description("Pin sạc dự phòng").isActive(true).build());
        categories.add(Category.builder().name("Camera").slug(slugify("Camera")).imageUrl(CATEGORY_IMAGE_URL).description("Camera an ninh và hành trình").isActive(true).build());
        categoryRepository.saveAll(categories);
        return categories.stream().collect(Collectors.toMap(Category::getName, Category::getId));
    }

    private Map<String, List<CategoryAttribute>> seedCategoryAttributes(Map<String, String> categoryIdByName) {
        Map<String, List<CategoryAttribute>> result = new HashMap<>();
        if (categoryAttributeRepository.count() > 0) {
            categoryAttributeRepository.findAll().forEach(attr -> result.computeIfAbsent(attr.getCategoryId(), k -> new ArrayList<>()).add(attr));
            return result;
        }

        String phoneCategoryId = categoryIdByName.get("Điện thoại");
        List<CategoryAttribute> phoneAttrs = List.of(
                CategoryAttribute.builder().categoryId(phoneCategoryId).attributeName("Màn hình").attributeKey("screen").dataType("TEXT").unit("inch").isRequired(true).isHighlight(true).sortOrder(1).description("Kích thước màn hình").build(),
                CategoryAttribute.builder().categoryId(phoneCategoryId).attributeName("RAM").attributeKey("ram").dataType("NUMBER").unit("GB").isRequired(true).isHighlight(true).sortOrder(2).build(),
                CategoryAttribute.builder().categoryId(phoneCategoryId).attributeName("Bộ nhớ").attributeKey("storage").dataType("NUMBER").unit("GB").isRequired(true).isHighlight(true).sortOrder(3).build(),
                CategoryAttribute.builder().categoryId(phoneCategoryId).attributeName("Pin").attributeKey("battery").dataType("NUMBER").unit("mAh").isRequired(false).isHighlight(false).sortOrder(4).build()
        );

        String accessoryCategoryId = categoryIdByName.get("Phụ kiện");
        List<CategoryAttribute> accessoryAttrs = List.of(
                CategoryAttribute.builder().categoryId(accessoryCategoryId).attributeName("Loại phụ kiện").attributeKey("type").dataType("SELECT").selectOptions(List.of("Sạc", "Ốp lưng", "Tai nghe")).isRequired(true).isHighlight(true).sortOrder(1).build(),
                CategoryAttribute.builder().categoryId(accessoryCategoryId).attributeName("Thương hiệu").attributeKey("brand").dataType("TEXT").isRequired(false).isHighlight(false).sortOrder(2).build()
        );

        String tabletCategoryId = categoryIdByName.get("Máy tính bảng");
        List<CategoryAttribute> tabletAttrs = List.of(
                CategoryAttribute.builder().categoryId(tabletCategoryId).attributeName("Màn hình").attributeKey("screen").dataType("TEXT").unit("inch").isRequired(true).isHighlight(true).sortOrder(1).build(),
                CategoryAttribute.builder().categoryId(tabletCategoryId).attributeName("RAM").attributeKey("ram").dataType("NUMBER").unit("GB").isRequired(true).isHighlight(true).sortOrder(2).build(),
                CategoryAttribute.builder().categoryId(tabletCategoryId).attributeName("Bộ nhớ").attributeKey("storage").dataType("NUMBER").unit("GB").isRequired(true).isHighlight(true).sortOrder(3).build()
        );
        
        String laptopCategoryId = categoryIdByName.get("Laptop");
        List<CategoryAttribute> laptopAttrs = List.of(
                CategoryAttribute.builder().categoryId(laptopCategoryId).attributeName("CPU").attributeKey("cpu").dataType("TEXT").isRequired(true).isHighlight(true).sortOrder(1).build(),
                CategoryAttribute.builder().categoryId(laptopCategoryId).attributeName("RAM").attributeKey("ram").dataType("NUMBER").unit("GB").isRequired(true).isHighlight(true).sortOrder(2).build(),
                CategoryAttribute.builder().categoryId(laptopCategoryId).attributeName("Ổ cứng").attributeKey("storage").dataType("TEXT").isRequired(true).isHighlight(true).sortOrder(3).build(),
                CategoryAttribute.builder().categoryId(laptopCategoryId).attributeName("Card đồ họa").attributeKey("gpu").dataType("TEXT").isRequired(false).isHighlight(true).sortOrder(4).build()
        );
        
        String smartwatchCategoryId = categoryIdByName.get("Đồng hồ thông minh");
        List<CategoryAttribute> smartwatchAttrs = List.of(
                CategoryAttribute.builder().categoryId(smartwatchCategoryId).attributeName("Màn hình").attributeKey("screen").dataType("TEXT").unit("inch").isRequired(true).isHighlight(true).sortOrder(1).build(),
                CategoryAttribute.builder().categoryId(smartwatchCategoryId).attributeName("Thời lượng pin").attributeKey("battery_life").dataType("TEXT").unit("ngày").isRequired(false).isHighlight(true).sortOrder(2).build(),
                CategoryAttribute.builder().categoryId(smartwatchCategoryId).attributeName("Chống nước").attributeKey("water_resistance").dataType("TEXT").isRequired(false).isHighlight(false).sortOrder(3).build()
        );
        
        String headphoneCategoryId = categoryIdByName.get("Tai nghe");
        List<CategoryAttribute> headphoneAttrs = List.of(
                CategoryAttribute.builder().categoryId(headphoneCategoryId).attributeName("Loại kết nối").attributeKey("connection_type").dataType("SELECT").selectOptions(List.of("Bluetooth", "Có dây", "USB-C")).isRequired(true).isHighlight(true).sortOrder(1).build(),
                CategoryAttribute.builder().categoryId(headphoneCategoryId).attributeName("Chống ồn").attributeKey("noise_cancelling").dataType("SELECT").selectOptions(List.of("Có", "Không")).isRequired(false).isHighlight(true).sortOrder(2).build()
        );

        List<CategoryAttribute> all = new ArrayList<>();
        all.addAll(phoneAttrs);
        all.addAll(accessoryAttrs);
        all.addAll(tabletAttrs);
        all.addAll(laptopAttrs);
        all.addAll(smartwatchAttrs);
        all.addAll(headphoneAttrs);
        categoryAttributeRepository.saveAll(all);
        all.forEach(attr -> result.computeIfAbsent(attr.getCategoryId(), k -> new ArrayList<>()).add(attr));
        return result;
    }

    private List<Product> seedProducts(Map<String, List<CategoryAttribute>> attributesByCategoryId) {
        if (productRepository.count() > 0) return new ArrayList<>(productRepository.findAll());
        
        List<Shop> shops = shopRepository.findAll();
        if (shops.isEmpty()) return new ArrayList<>();

        List<Category> categories = categoryRepository.findAll();
        List<Product> products = new ArrayList<>();

        String[] phoneNames = {"iPhone 14 Pro", "Samsung Galaxy S23", "Xiaomi 13 Pro", "Oppo Find X6", "Vivo X90", "Realme GT 3", "iPhone 13", "Samsung A54"};
        String[] tabletNames = {"iPad Pro 2023", "Samsung Tab S8", "Xiaomi Pad 6", "Lenovo Tab P11"};
        String[] laptopNames = {"MacBook Pro M2", "Dell XPS 15", "HP Pavilion 15", "Asus ROG Strix", "Lenovo ThinkPad X1", "Acer Swift 3"};
        String[] smartwatchNames = {"Apple Watch Series 8", "Samsung Galaxy Watch 5", "Xiaomi Mi Watch", "Amazfit GTR 4"};
        String[] headphoneNames = {"AirPods Pro", "Sony WH-1000XM5", "JBL Tune 500BT", "Samsung Buds Pro"};

        for (Category category : categories) {
            List<CategoryAttribute> attrs = attributesByCategoryId.getOrDefault(category.getId(), Collections.emptyList());
            String[] productNames = getProductNames(category.getName());
            int productCount = productNames.length;
            
            for (int i = 0; i < productCount; i++) {
                Shop shop = shops.get(ThreadLocalRandom.current().nextInt(shops.size()));
                double price = getPriceRange(category.getName());
                double saleOff = ThreadLocalRandom.current().nextBoolean() ? randomBetween(0, 30) : 0;
                double finalPrice = Math.round(price * (100 - saleOff)) / 100.0;
                List<String> images = List.of(PRODUCT_IMAGE_URL);

                List<Product.ProductAttributeValue> values = attrs.stream().map(a -> Product.ProductAttributeValue.builder()
                        .attributeId(a.getId())
                        .attributeKey(a.getAttributeKey())
                        .attributeName(a.getAttributeName())
                        .value(fakeValueForAttribute(a))
                        .unit(a.getUnit())
                        .dataType(a.getDataType())
                        .build()).collect(Collectors.toList());

                Product p = Product.builder()
                        .shopId(shop.getId())
                        .categoryId(category.getId())
                        .name(productNames[i])
                        .description("Sản phẩm " + productNames[i] + " chính hãng, đầy đủ phụ kiện")
                        .images(images)
                        .price(price)
                        .saleOff(saleOff)
                        .finalPrice(finalPrice)
                        .stockQuantity((int) randomBetween(5, 300))
                        .attributeValues(values)
                        .averageRating(randomBetween(3, 5))
                        .reviewCount((int) randomBetween(0, 500))
                        .purchaseCount((int) randomBetween(0, 1000))
                        .isPublished(true)
                        .build();
                products.add(p);
            }
        }
        productRepository.saveAll(products);
        return products;
    }
    
    private String[] getProductNames(String categoryName) {
        return switch (categoryName) {
            case "Điện thoại" -> new String[]{"iPhone 14 Pro", "Samsung Galaxy S23", "Xiaomi 13 Pro", "Oppo Find X6", "Vivo X90", "Realme GT 3", "iPhone 13", "Samsung A54", "iPhone 15", "Xiaomi 14"};
            case "Máy tính bảng" -> new String[]{"iPad Pro 2023", "Samsung Tab S8", "Xiaomi Pad 6", "Lenovo Tab P11", "iPad Air", "Samsung Tab A8"};
            case "Laptop" -> new String[]{"MacBook Pro M2", "Dell XPS 15", "HP Pavilion 15", "Asus ROG Strix", "Lenovo ThinkPad X1", "Acer Swift 3", "MSI Gaming GF63", "MacBook Air M2"};
            case "Đồng hồ thông minh" -> new String[]{"Apple Watch Series 8", "Samsung Galaxy Watch 5", "Xiaomi Mi Watch", "Amazfit GTR 4", "Huawei Watch GT 3"};
            case "Tai nghe" -> new String[]{"AirPods Pro", "Sony WH-1000XM5", "JBL Tune 500BT", "Samsung Buds Pro", "Bose QuietComfort", "Beats Studio 3"};
            case "Sạc dự phòng" -> new String[]{"Anker 20000mAh", "Xiaomi 10000mAh", "Samsung 25W", "Baseus 30000mAh"};
            case "Camera" -> new String[]{"Camera Ezviz C6N", "Camera Xiaomi 360", "Camera Imou Ranger 2", "Camera Hikvision"};
            default -> new String[]{"Phụ kiện điện thoại", "Ốp lưng chống sốc", "Cáp sạc nhanh", "Miếng dán màn hình"};
        };
    }
    
    private double getPriceRange(String categoryName) {
        return switch (categoryName) {
            case "Điện thoại" -> randomBetween(3_000_000, 30_000_000);
            case "Laptop" -> randomBetween(10_000_000, 50_000_000);
            case "Máy tính bảng" -> randomBetween(5_000_000, 25_000_000);
            case "Đồng hồ thông minh" -> randomBetween(2_000_000, 15_000_000);
            case "Tai nghe" -> randomBetween(500_000, 8_000_000);
            default -> randomBetween(100_000, 2_000_000);
        };
    }

    private Map<String, CustomerSegment> seedCustomerSegments() {
        if (customerSegmentRepository.count() > 0) {
            return customerSegmentRepository.findAll().stream().collect(Collectors.toMap(CustomerSegment::getName, s -> s));
        }
        List<CustomerSegment> segments = List.of(
                CustomerSegment.builder().name("Bronze").minSpend(0.0).maxSpend(1_000_000.0).level(1).description("Khách mới/chi tiêu thấp").build(),
                CustomerSegment.builder().name("Silver").minSpend(1_000_000.0).maxSpend(5_000_000.0).level(2).description("Khách thường xuyên").build(),
                CustomerSegment.builder().name("Gold").minSpend(5_000_000.0).maxSpend(null).level(3).description("Khách VIP").build()
        );
        customerSegmentRepository.saveAll(segments);
        return segments.stream().collect(Collectors.toMap(CustomerSegment::getName, s -> s));
    }

    private void seedSegmentCoupons(Map<String, CustomerSegment> segmentsByName) {
        if (segmentCouponRepository.count() > 0) return;
        List<SegmentCoupon> list = new ArrayList<>();
        list.add(SegmentCoupon.builder()
                .segmentId(segmentsByName.get("Silver").getId())
                .codePrefix("SILV")
                .title("Ưu đãi Silver 10%")
                .description("Giảm 10% cho khách Silver")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(10.0)
                .minOrderAmount(200_000.0)
                .validHours(72)
                .isActive(true)
                .scheduleFrequency(ScheduleFrequency.NONE)
                .build());

        list.add(SegmentCoupon.builder()
                .segmentId(segmentsByName.get("Gold").getId())
                .codePrefix("GOLD")
                .title("Ưu đãi Gold 100k")
                .description("Giảm 100.000đ cho đơn từ 500.000đ")
                .discountType(DiscountType.FIXED)
                .discountValue(100_000.0)
                .minOrderAmount(500_000.0)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(1))
                .scheduleFrequency(ScheduleFrequency.MONTHLY)
                .scheduleDayOfMonth(1)
                .scheduleTime(LocalTime.of(0, 0))
                .isActive(true)
                .build());
        segmentCouponRepository.saveAll(list);
    }

    private void seedCouponCampaign() {
        if (couponCampaignRepository.count() > 0) return;
        List<String> anyCategoryIds = categoryRepository.findAll().stream().map(Category::getId).limit(1).collect(Collectors.toList());
        CouponCampaign campaign = CouponCampaign.builder()
                .title("Tuần lễ công nghệ")
                .description("Giảm giá 15% cho một số danh mục")
                .codeTemplate("TECHWEEK15")
                .couponType(CouponType.PERCENTAGE)
                .discountValue(15.0)
                .minOrderAmount(300_000.0)
                .applicableCategoryIds(anyCategoryIds)
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusDays(14))
                .distributionType(DistributionType.SHARED_CODE)
                .maxTotalIssuance(10_000)
                .perUserLimit(3)
                .status(CampaignStatus.ACTIVE)
                .isActive(true)
                .createdBy(userRepository.findByEmail("admin@gmail.com").map(User::getId).orElse(null))
                .note("Seeded data")
                .build();
        couponCampaignRepository.save(campaign);

        campaignDistributionLogRepository.save(CampaignDistributionLog.builder()
                .campaignId(campaign.getId())
                .adminId(userRepository.findByEmail("admin@gmail.com").map(User::getId).orElse(null))
                .filterCriteria(Map.of("categoryIds", anyCategoryIds))
                .recipientsCount(1000)
                .successCount(980)
                .failedCount(20)
                .errorSummary(null)
                .executionTimeMs(1500L)
                .build());
    }

    private void seedUserCoupons() {
        if (userCouponRepository.count() > 0) return;
        Optional<User> userOpt = userRepository.findByEmail("user@gmail.com");
        if (userOpt.isEmpty()) return;
        User user = userOpt.get();

        // Coupon from segment
        segmentCouponRepository.findAll().stream().findFirst().ifPresent(seg -> {
            UserCoupon uc = UserCoupon.builder()
                    .userId(user.getId())
                    .segmentCouponId(seg.getId())
                    .code(seg.getCodePrefix() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                    .title(seg.getTitle())
                    .description(seg.getDescription())
                    .couponType(seg.getDiscountType() == DiscountType.PERCENTAGE ? CouponType.PERCENTAGE : CouponType.FIXED)
                    .discountValue(seg.getDiscountValue())
                    .minOrderAmount(seg.getMinOrderAmount())
                    .issuedDate(LocalDateTime.now())
                    .expiresAt(LocalDateTime.now().plusDays(7))
                    .status(CouponStatus.ACTIVE)
                    .issuedVia(IssuedVia.AUTO_ON_UPGRADE)
                    .build();
            userCouponRepository.save(uc);
        });

        // Coupon from campaign (shared code)
        couponCampaignRepository.findAll().stream().findFirst().ifPresent(campaign -> {
            UserCoupon uc = UserCoupon.builder()
                    .userId(user.getId())
                    .campaignId(campaign.getId())
                    .code(campaign.getCodeTemplate())
                    .title(campaign.getTitle())
                    .description(campaign.getDescription())
                    .couponType(CouponType.PERCENTAGE)
                    .discountValue(campaign.getDiscountValue())
                    .minOrderAmount(campaign.getMinOrderAmount())
                    .applicableCategoryIds(campaign.getApplicableCategoryIds())
                    .issuedDate(LocalDateTime.now())
                    .expiresAt(campaign.getEndDate())
                    .status(CouponStatus.ACTIVE)
                    .issuedVia(IssuedVia.CAMPAIGN)
                    .build();
            userCouponRepository.save(uc);
        });
    }

    private String randomPhone() {
        StringBuilder sb = new StringBuilder("09");
        for (int i = 0; i < 8; i++) sb.append(ThreadLocalRandom.current().nextInt(0, 10));
        return sb.toString();
    }

    private double randomBetween(double min, double max) {
        return Math.round((ThreadLocalRandom.current().nextDouble(min, max)) * 100.0) / 100.0;
    }

    private String slugify(String input) {
        return com.example.cellex.utils.SlugUtil.toSlug(input);
    }

    private User.Address sampleUserAddress() {
        // Chọn ngẫu nhiên 1 tỉnh, sau đó chọn 1 xã/phường thuộc tỉnh đó
        Province p = provinces.isEmpty() ? new Province("79", "Hồ Chí Minh") : provinces.get(ThreadLocalRandom.current().nextInt(provinces.size()));
        List<Commune> inProvince = communes.stream().filter(c -> Objects.equals(c.getProvinceCode(), p.getCode())).collect(Collectors.toList());
        Commune c = inProvince.isEmpty() ? new Commune("760101", "Phường Bến Nghé", p.getCode()) : inProvince.get(ThreadLocalRandom.current().nextInt(inProvince.size()));
        String detail = "Số " + (int) randomBetween(1, 200) + " Đường A";
        String full = detail + ", " + c.getName() + ", " + p.getName();
        return User.Address.builder()
                .provinceCode(p.getCode())
                .provinceName(p.getName())
                .communeCode(c.getCode())
                .communeName(c.getName())
                .detailAddress(detail)
                .fullAddress(full)
                .build();
    }

    private Shop.Address sampleShopAddress() {
        Province p = provinces.isEmpty() ? new Province("79", "Hồ Chí Minh") : provinces.get(ThreadLocalRandom.current().nextInt(provinces.size()));
        List<Commune> inProvince = communes.stream().filter(c -> Objects.equals(c.getProvinceCode(), p.getCode())).collect(Collectors.toList());
        Commune c = inProvince.isEmpty() ? new Commune("760102", "Phường Bến Thành", p.getCode()) : inProvince.get(ThreadLocalRandom.current().nextInt(inProvince.size()));
        String detail = "Số " + (int) randomBetween(10, 300) + " Đường B";
        String full = detail + ", " + c.getName() + ", " + p.getName();
        return Shop.Address.builder()
                .street(detail)
                .commune(c.getName())
                .province(p.getName())
                .country("Việt Nam")
                .fullAddress(full)
                .isDefault(false)
                .build();
    }

    private String fakeValueForAttribute(CategoryAttribute attr) {
        String key = Optional.ofNullable(attr.getAttributeKey()).orElse("");
        if (key.equals("screen")) return "6." + ThreadLocalRandom.current().nextInt(0, 9) + "\"";
        if (key.equals("ram")) return String.valueOf((List.of(4, 6, 8, 12, 16, 32)).get(ThreadLocalRandom.current().nextInt(6)));
        if (key.equals("storage")) return String.valueOf((List.of(64, 128, 256, 512, 1024)).get(ThreadLocalRandom.current().nextInt(5)));
        if (key.equals("battery")) return String.valueOf((List.of(4000, 4500, 5000, 6000)).get(ThreadLocalRandom.current().nextInt(4)));
        if (key.equals("type")) return (List.of("Sạc", "Ốp lưng", "Tai nghe")).get(ThreadLocalRandom.current().nextInt(3));
        if (key.equals("brand")) return (List.of("Apple", "Samsung", "Xiaomi", "Baseus")).get(ThreadLocalRandom.current().nextInt(4));
        if (key.equals("cpu")) return (List.of("Intel Core i5", "Intel Core i7", "AMD Ryzen 5", "AMD Ryzen 7", "Apple M1", "Apple M2")).get(ThreadLocalRandom.current().nextInt(6));
        if (key.equals("gpu")) return (List.of("Intel Iris Xe", "NVIDIA RTX 3050", "NVIDIA RTX 3060", "AMD Radeon")).get(ThreadLocalRandom.current().nextInt(4));
        if (key.equals("battery_life")) return String.valueOf((List.of(3, 5, 7, 10, 14)).get(ThreadLocalRandom.current().nextInt(5)));
        if (key.equals("water_resistance")) return (List.of("IP67", "IP68", "5ATM", "10ATM")).get(ThreadLocalRandom.current().nextInt(4));
        if (key.equals("connection_type")) return (List.of("Bluetooth", "Có dây", "USB-C")).get(ThreadLocalRandom.current().nextInt(3));
        if (key.equals("noise_cancelling")) return (List.of("Có", "Không")).get(ThreadLocalRandom.current().nextInt(2));
        return "N/A";
    }
    
    // ================ SEED METHODS ================
    
    private List<Order> seedOrders(List<User> users, List<Shop> shops, List<Product> products) {
        if (orderRepository.count() > 0) return new ArrayList<>(orderRepository.findAll());
        if (products.isEmpty() || shops.isEmpty()) return new ArrayList<>();
        
        List<Order> orders = new ArrayList<>();
        List<User> normalUsers = users.stream().filter(u -> u.getRole() == Role.USER).collect(Collectors.toList());
        
        OrderStatus[] statuses = {OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderStatus.SHIPPING, OrderStatus.DELIVERED, OrderStatus.CANCELLED};
        PaymentMethod[] paymentMethods = {PaymentMethod.COD, PaymentMethod.VNPAY};
        
        // Tạo 30-50 đơn hàng
        for (int i = 0; i < 40; i++) {
            User user = normalUsers.get(ThreadLocalRandom.current().nextInt(normalUsers.size()));
            Shop shop = shops.get(ThreadLocalRandom.current().nextInt(shops.size()));
            
            List<OrderItem> items = new ArrayList<>();
            double subtotal = 0.0;
            
            int itemCount = (int) randomBetween(1, 4);
            for (int j = 0; j < itemCount; j++) {
                Product product = products.stream()
                        .filter(p -> p.getShopId().equals(shop.getId()))
                        .findAny()
                        .orElse(products.get(0));
                
                // Ensure product has ID
                if (product == null || product.getId() == null) {
                    continue;
                }
                        
                int quantity = (int) randomBetween(1, 3);
                double price = product.getFinalPrice();
                
                OrderItem item = OrderItem.builder()
                        .productId(product.getId())
                        .productName(product.getName())
                        .productImage(product.getImages().isEmpty() ? "" : product.getImages().get(0))
                        .priceDecimal(BigDecimal.valueOf(price))
                        .quantity(quantity)
                        .subtotalDecimal(BigDecimal.valueOf(price * quantity))
                        .build();
                
                items.add(item);
                subtotal += item.getSubtotal();
            }
            
            double shippingFee = randomBetween(15000, 50000);
            double discountAmount = ThreadLocalRandom.current().nextBoolean() ? randomBetween(10000, 100000) : 0.0;
            double totalAmount = subtotal + shippingFee - discountAmount;
            
            OrderStatus status = statuses[ThreadLocalRandom.current().nextInt(statuses.length)];
            PaymentMethod paymentMethod = paymentMethods[ThreadLocalRandom.current().nextInt(paymentMethods.length)];
            LocalDateTime createdAt = LocalDateTime.now().minusDays(ThreadLocalRandom.current().nextInt(1, 90));
            
            List<Order.StatusHistory> statusHistory = new ArrayList<>();
            statusHistory.add(Order.StatusHistory.builder()
                    .status(OrderStatus.PENDING)
                    .note("Đơn hàng đã được tạo")
                    .updatedBy(user.getId())
                    .updatedAt(createdAt)
                    .build());
            
            Order order = Order.builder()
                    .orderCode("ORD" + System.currentTimeMillis() + i)
                    .userUuid(UUID.fromString(user.getId()))
                    .shopUuid(UUID.fromString(shop.getId()))
                    .shopName(shop.getShopName())
                    .shippingAddress(convertToOrderAddress(user.getAddress()))
                    .subtotalDecimal(BigDecimal.valueOf(subtotal))
                    .shippingFeeDecimal(BigDecimal.valueOf(shippingFee))
                    .discountAmountDecimal(BigDecimal.valueOf(discountAmount))
                    .totalAmountDecimal(BigDecimal.valueOf(totalAmount))
                    .paymentMethod(paymentMethod)
                    .isPaid(paymentMethod == PaymentMethod.VNPAY || status == OrderStatus.DELIVERED)
                    .paidAt(paymentMethod == PaymentMethod.VNPAY ? createdAt.plusMinutes(5) : null)
                    .status(status)
                    .statusHistory(statusHistory)
                    .isFromCart(ThreadLocalRandom.current().nextBoolean())
                    .createdAt(createdAt)
                    .updatedAt(LocalDateTime.now())
                    .build();

            // Set bidirectional relationship for JPA
            for (OrderItem item : items) { item.setOrder(order); }
            order.setItems(items);
            
            if (status == OrderStatus.DELIVERED) {
                order.setDeliveredAt(createdAt.plusDays(ThreadLocalRandom.current().nextInt(3, 10)));
            } else if (status == OrderStatus.CANCELLED) {
                order.setCancelledAt(createdAt.plusHours(ThreadLocalRandom.current().nextInt(1, 48)));
                order.setCancelReason("Khách hàng đổi ý");
            }
            
            orders.add(order);
        }
        
        orderRepository.saveAll(orders);
        return orders;
    }
    
    private Order.ShippingAddress convertToOrderAddress(User.Address address) {
        if (address == null) return null;
        return Order.ShippingAddress.builder()
                .provinceCode(address.getProvinceCode())
                .provinceName(address.getProvinceName())
                .communeCode(address.getCommuneCode())
                .communeName(address.getCommuneName())
                .detailAddress(address.getDetailAddress())
                .fullAddress(address.getFullAddress())
                .build();
    }
    
    private void seedReviews(List<User> users, List<Shop> shops, List<Product> products, List<Order> orders) {
        if (reviewRepository.count() > 0) return;
        if (orders.isEmpty()) return;
        
        List<Review> reviews = new ArrayList<>();
        String[] comments = {
                "Sản phẩm rất tốt, đúng như mô tả",
                "Chất lượng OK, giao hàng nhanh",
                "Hàng đẹp, đóng gói cẩn thận",
                "Sản phẩm chất lượng, giá hợp lý",
                "Rất hài lòng với sản phẩm này",
                "Giao hàng chậm nhưng sản phẩm tốt",
                "Không đúng như mô tả, hơi thất vọng",
                "Tạm được, giá hơi cao so với chất lượng"
        };
        
        // Tạo review cho 30% đơn hàng đã giao
        List<Order> deliveredOrders = orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.DELIVERED)
                .collect(Collectors.toList());
        
        for (int i = 0; i < deliveredOrders.size() * 0.3; i++) {
            if (i >= deliveredOrders.size()) break;
            Order order = deliveredOrders.get(i);
            OrderItem item = order.getItems().get(0);
            
            User user = users.stream().filter(u -> u.getId().equals(order.getUserId())).findFirst().orElse(null);
            if (user == null) continue;
            
            int rating = ThreadLocalRandom.current().nextInt(3, 6); // 3-5 sao
            boolean hasVendorResponse = ThreadLocalRandom.current().nextBoolean();
            
            Review review = Review.builder()
                    .productId(item.getProductId())
                    .userId(user.getId())
                    .userName(user.getFullName())
                    .userAvatar(user.getAvatarUrl())
                    .orderId(order.getId())
                    .orderItemId(item.getProductId())
                    .shopId(order.getShopId())
                    .rating(rating)
                    .comment(comments[ThreadLocalRandom.current().nextInt(comments.length)])
                    .images(ThreadLocalRandom.current().nextBoolean() ? List.of(PRODUCT_IMAGE_URL) : List.of())
                    .videos(List.of())
                    .isVerifiedPurchase(true)
                    .helpfulCount((int) randomBetween(0, 50))
                    .status(ReviewStatus.APPROVED)
                    .createdAt(order.getDeliveredAt().plusDays(ThreadLocalRandom.current().nextInt(1, 15)))
                    .updatedAt(LocalDateTime.now())
                    .build();
            
            if (hasVendorResponse) {
                review.setVendorResponse(VendorResponse.builder()
                        .vendorId(order.getShopId())
                        .vendorName("Vendor")
                        .comment("Cảm ơn bạn đã ủng hộ shop!")
                        .createdAt(review.getCreatedAt().plusDays(1))
                        .build());
            }
            
            reviews.add(review);
        }
        
        reviewRepository.saveAll(reviews);
    }
    
    private void seedUserInteractions(List<User> users, List<Product> products) {
        if (userInteractionRepository.count() > 0) return;
        if (products.isEmpty()) return;
        
        List<UserInteraction> interactions = new ArrayList<>();
        List<User> normalUsers = users.stream().filter(u -> u.getRole() == Role.USER).collect(Collectors.toList());
        
        for (User user : normalUsers) {
            int interactionCount = ThreadLocalRandom.current().nextInt(5, 20);
            for (int i = 0; i < interactionCount; i++) {
                Product product = products.get(ThreadLocalRandom.current().nextInt(products.size()));
                
                int viewCount = ThreadLocalRandom.current().nextInt(1, 10);
                int cartCount = ThreadLocalRandom.current().nextBoolean() ? ThreadLocalRandom.current().nextInt(0, 3) : 0;
                int purchaseCount = ThreadLocalRandom.current().nextBoolean() ? ThreadLocalRandom.current().nextInt(0, 2) : 0;
                int reviewCount = purchaseCount > 0 && ThreadLocalRandom.current().nextBoolean() ? 1 : 0;
                
                double score = (viewCount * 1.0) + (cartCount * 3.0) + (purchaseCount * 5.0) + (reviewCount * 4.0);
                
                UserInteraction interaction = UserInteraction.builder()
                        .userId(user.getId())
                        .productId(product.getId())
                        .categoryId(product.getCategoryId())
                        .viewCount(viewCount)
                        .cartCount(cartCount)
                        .purchaseCount(purchaseCount)
                        .reviewCount(reviewCount)
                        .totalScore(score)
                        .createdAt(LocalDateTime.now().minusDays(ThreadLocalRandom.current().nextInt(1, 90)))
                        .updatedAt(LocalDateTime.now())
                        .build();
                interactions.add(interaction);
            }
        }
        
        userInteractionRepository.saveAll(interactions);
    }
    
    private void seedProductSimilarities(List<Product> products) {
        if (productSimilarityRepository.count() > 0) return;
        if (products.size() < 2) return;
        
        List<ProductSimilarity> similarities = new ArrayList<>();
        
        // Tạo similarity giữa các sản phẩm cùng category
        Map<String, List<Product>> productsByCategory = products.stream()
                .collect(Collectors.groupingBy(Product::getCategoryId));
        
        for (Map.Entry<String, List<Product>> entry : productsByCategory.entrySet()) {
            List<Product> categoryProducts = entry.getValue();
            if (categoryProducts.size() < 2) continue;
            
            for (int i = 0; i < categoryProducts.size(); i++) {
                Product product1 = categoryProducts.get(i);
                
                // Lấy 3-5 sản phẩm tương tự
                int similarCount = Math.min(5, categoryProducts.size() - 1);
                for (int j = 0; j < similarCount && j < categoryProducts.size(); j++) {
                    if (i == j) continue;
                    Product product2 = categoryProducts.get(j);
                    
                    ProductSimilarity similarity = ProductSimilarity.builder()
                            .productId(product1.getId())
                            .similarProductId(product2.getId())
                            .similarityScore(randomBetween(0.6, 0.99))
                            .calculationMethod("COLLABORATIVE_FILTERING")
                            .createdAt(LocalDateTime.now().minusDays(ThreadLocalRandom.current().nextInt(1, 30)))
                            .updatedAt(LocalDateTime.now())
                            .build();
                    similarities.add(similarity);
                }
            }
        }
        
        productSimilarityRepository.saveAll(similarities);
    }
    
    private void seedRecommendations(List<User> users, List<Product> products) {
        if (recommendationRepository.count() > 0) return;
        if (products.isEmpty()) return;
        
        List<Recommendation> recommendations = new ArrayList<>();
        List<User> normalUsers = users.stream().filter(u -> u.getRole() == Role.USER).collect(Collectors.toList());
        
        for (User user : normalUsers) {
            // Chọn 5-10 sản phẩm để gợi ý
            int recommendCount = ThreadLocalRandom.current().nextInt(5, 11);
            Set<String> usedProductIds = new HashSet<>();
            
            for (int rank = 0; rank < recommendCount && rank < products.size(); rank++) {
                Product product = products.get(ThreadLocalRandom.current().nextInt(products.size()));
                if (!usedProductIds.contains(product.getId())) {
                    usedProductIds.add(product.getId());
                    
                    String[] reasons = {"CF", "TRENDING", "CONTENT_BASED", "POPULARITY"};
                    String[] explanations = {
                        "Dựa trên lịch sử mua hàng",
                        "Sản phẩm đang được nhiều người quan tâm",
                        "Tương tự sản phẩm bạn đã xem",
                        "Sản phẩm phổ biến trong danh mục"
                    };
                    int reasonIndex = ThreadLocalRandom.current().nextInt(reasons.length);
                    
                    Recommendation recommendation = Recommendation.builder()
                            .userId(user.getId())
                            .productId(product.getId())
                            .recommendationScore(randomBetween(0.5, 0.95))
                            .recommendationReason(reasons[reasonIndex])
                            .explanation(explanations[reasonIndex])
                            .rank(rank + 1)
                            .createdAt(LocalDateTime.now().minusDays(ThreadLocalRandom.current().nextInt(1, 7)))
                            .updatedAt(LocalDateTime.now())
                            .build();
                    recommendations.add(recommendation);
                }
            }
        }
        
        recommendationRepository.saveAll(recommendations);
    }
}