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
import com.example.cellex.models.product.Product;
import com.example.cellex.models.segment.CustomerSegment;
import com.example.cellex.models.shop.Shop;
import com.example.cellex.models.user.User;
import com.example.cellex.repositories.category.CategoryAttributeRepository;
import com.example.cellex.repositories.category.CategoryRepository;
import com.example.cellex.repositories.coupon.CampaignDistributionLogRepository;
import com.example.cellex.repositories.coupon.CouponCampaignRepository;
import com.example.cellex.repositories.coupon.SegmentCouponRepository;
import com.example.cellex.repositories.coupon.UserCouponRepository;
import com.example.cellex.repositories.product.ProductRepository;
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
        //mongoTemplate.getDb().drop();

        seedUsersAndShop();
        Map<String, String> categoryIdByName = seedCategories();
        Map<String, List<CategoryAttribute>> attributesByCategoryId = seedCategoryAttributes(categoryIdByName);
        seedProducts(attributesByCategoryId);
        Map<String, CustomerSegment> segmentsByName = seedCustomerSegments();
        seedSegmentCoupons(segmentsByName);
        seedCouponCampaign();
        seedUserCoupons();
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

    private void seedUsersAndShop() {
        // Admin
        userRepository.findByEmail("admin@gmail.com").orElseGet(() -> userRepository.save(User.builder()
                .fullName("Admin User")
                .email("admin@gmail.com")
                .password(passwordEncoder.encode("123"))
                .phoneNumber(randomPhone())
                .avatarUrl(AVATAR_URL)
                .role(Role.ADMIN)
                .isActive(true)
                .totalSpend(0.0)
                .address(sampleUserAddress())
                .build()));

        // Vendor
        User vendor = userRepository.findByEmail("vendor@gmail.com").orElseGet(() -> userRepository.save(User.builder()
                .fullName("Vendor User")
                .email("vendor@gmail.com")
                .password(passwordEncoder.encode("123"))
                .phoneNumber(randomPhone())
                .avatarUrl(AVATAR_URL)
                .role(Role.VENDOR)
                .isActive(true)
                .totalSpend(0.0)
                .address(sampleUserAddress())
                .build()));

        // Normal User
        userRepository.findByEmail("user@gmail.com").orElseGet(() -> userRepository.save(User.builder()
                .fullName("Normal User")
                .email("user@gmail.com")
                .password(passwordEncoder.encode("123"))
                .phoneNumber(randomPhone())
                .avatarUrl(AVATAR_URL)
                .role(Role.USER)
                .isActive(true)
                .totalSpend(randomBetween(0, 5_000_000))
                .address(sampleUserAddress())
                .build()));

        // Create a Shop for vendor if not exists
        if (shopRepository.findAll().stream().noneMatch(s -> Objects.equals(s.getVendorId(), vendor.getId()))) {
            Shop shop = Shop.builder()
                    .vendorId(vendor.getId())
                    .shopName("Vendor Tech Store")
                    .description("Cửa hàng chuyên đồ công nghệ, điện thoại, phụ kiện.")
                    .logoUrl(AVATAR_URL)
                    .address(sampleShopAddress())
                    .phoneNumber(randomPhone())
                    .email("vendor@gmail.com")
                    .status(ShopStatus.APPROVED)
                    .rating(4.6)
                    .build();
            shopRepository.save(shop);
        }
    }

    private Map<String, String> seedCategories() {
        if (categoryRepository.count() > 0) {
            return categoryRepository.findAll().stream().collect(Collectors.toMap(Category::getName, Category::getId));
        }
        List<Category> categories = new ArrayList<>();
        categories.add(Category.builder().name("Điện thoại").slug(slugify("Điện thoại")).imageUrl(CATEGORY_IMAGE_URL).description("Danh mục điện thoại thông minh").isActive(true).build());
        categories.add(Category.builder().name("Phụ kiện").slug(slugify("Phụ kiện")).imageUrl(CATEGORY_IMAGE_URL).description("Phụ kiện đi kèm thiết bị").isActive(true).build());
        categories.add(Category.builder().name("Máy tính bảng").slug(slugify("Máy tính bảng")).imageUrl(CATEGORY_IMAGE_URL).description("Tablet các loại").isActive(true).build());
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

        List<CategoryAttribute> all = new ArrayList<>();
        all.addAll(phoneAttrs);
        all.addAll(accessoryAttrs);
        all.addAll(tabletAttrs);
        categoryAttributeRepository.saveAll(all);
        all.forEach(attr -> result.computeIfAbsent(attr.getCategoryId(), k -> new ArrayList<>()).add(attr));
        return result;
    }

    private void seedProducts(Map<String, List<CategoryAttribute>> attributesByCategoryId) {
        if (productRepository.count() > 0) return;
        // Use first shop
        Optional<Shop> shopOpt = shopRepository.findAll().stream().findFirst();
        if (shopOpt.isEmpty()) return;
        Shop shop = shopOpt.get();

        List<Category> categories = categoryRepository.findAll();
        List<Product> products = new ArrayList<>();

        for (Category category : categories) {
            List<CategoryAttribute> attrs = attributesByCategoryId.getOrDefault(category.getId(), Collections.emptyList());
            int productCount = switch (category.getName()) {
                case "Điện thoại" -> 6;
                case "Phụ kiện" -> 4;
                default -> 3;
            };
            for (int i = 1; i <= productCount; i++) {
                double price = randomBetween(500_000, 30_000_000);
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
                        .name(category.getName() + " mẫu " + i)
                        .description("Sản phẩm thuộc danh mục " + category.getName())
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
        String s = input.toLowerCase(Locale.ROOT)
                .replaceAll("[áàảãạăắằẳẵặâấầẩẫậ]", "a")
                .replaceAll("[éèẻẽẹêếềểễệ]", "e")
                .replaceAll("[íìỉĩị]", "i")
                .replaceAll("[óòỏõọôốồổỗộơớờởỡợ]", "o")
                .replaceAll("[úùủũụưứừửữự]", "u")
                .replaceAll("[ýỳỷỹỵ]", "y")
                .replaceAll("đ", "d")
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        return s;
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
        if (key.equals("ram")) return String.valueOf((List.of(4, 6, 8, 12)).get(ThreadLocalRandom.current().nextInt(4)));
        if (key.equals("storage")) return String.valueOf((List.of(64, 128, 256, 512)).get(ThreadLocalRandom.current().nextInt(4)));
        if (key.equals("battery")) return String.valueOf((List.of(4000, 4500, 5000, 6000)).get(ThreadLocalRandom.current().nextInt(4)));
        if (key.equals("type")) return (List.of("Sạc", "Ốp lưng", "Tai nghe")).get(ThreadLocalRandom.current().nextInt(3));
        if (key.equals("brand")) return (List.of("Apple", "Samsung", "Xiaomi", "Baseus")).get(ThreadLocalRandom.current().nextInt(4));
        return "N/A";
    }
}

