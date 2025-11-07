package com.example.cellex.services.shop;

import com.example.cellex.dtos.request.shop.ShopVerificationRequest;
import com.example.cellex.dtos.request.shop.VendorRegistrationRequest;
import com.example.cellex.dtos.response.shop.ShopResponse;
import com.example.cellex.enums.Role;
import com.example.cellex.enums.ShopStatus;
import com.example.cellex.exceptions.AppException;
import com.example.cellex.exceptions.ErrorCode;
import com.example.cellex.models.address.Commune;
import com.example.cellex.models.address.Province;
import com.example.cellex.models.shop.Shop;
import com.example.cellex.models.user.User;
import com.example.cellex.repositories.product.ProductRepository;
import com.example.cellex.repositories.shop.ShopRepository;
import com.example.cellex.repositories.user.UserRepository;
import com.example.cellex.services.S3Service;
import com.example.cellex.services.address.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ShopService {

    private final ShopRepository shopRepository;
    private final UserRepository userRepository;
    private final S3Service s3Service;
    private final AddressService addressService;
    private final ProductRepository productRepository;

    public ShopResponse registerVendorShop(String vendorId, VendorRegistrationRequest request, MultipartFile logoFile) throws IOException {
        // Kiểm tra user có tồn tại không
        User vendor = userRepository.findById(vendorId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Kiểm tra user đã có shop chưa
        if (shopRepository.existsByVendorId(vendorId)) {
            throw new AppException(ErrorCode.SHOP_ALREADY_EXISTS);
        }

        // Validate và lấy tên địa chỉ từ JSON
        Province province = addressService.getProvinceByCode(request.getProvinceCode());
        if (province == null) {
            throw new AppException(ErrorCode.INVALID_INPUT);
        }

        Commune commune = addressService.getCommuneByCode(request.getCommuneCode());
        if (commune == null) {
            throw new AppException(ErrorCode.INVALID_INPUT);
        }

        // Upload logo nếu có
        String logoUrl = null;
        if (logoFile != null && !logoFile.isEmpty()) {
            logoUrl = s3Service.uploadFile(logoFile, "shop-logos");
        }

        // Tạo địa chỉ
        String fullAddress = request.getDetailAddress() + ", " + commune.getName() + ", " + province.getName();
        Shop.Address address = Shop.Address.builder()
                .street(request.getDetailAddress())
                .commune(commune.getName())
                .province(province.getName())
                .country("Việt Nam")
                .fullAddress(fullAddress)
                .isDefault(false)
                .build();

        // Tạo shop mới
        Shop shop = Shop.builder()
                .vendorId(vendorId)
                .shopName(request.getShopName())
                .description(request.getDescription())
                .logoUrl(logoUrl)
                .address(address)
                .phoneNumber(request.getPhoneNumber())
                .email(request.getEmail())
                .status(ShopStatus.PENDING)
                .rating(0.0)
                .build();

        Shop savedShop = shopRepository.save(shop);
        return mapToShopResponse(savedShop);
    }

    public ShopResponse verifyShop(ShopVerificationRequest request) {
        Shop shop = shopRepository.findById(request.getShopId())
                .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));

        if ("APPROVED".equals(request.getStatus())) {
            shop.setStatus(ShopStatus.APPROVED);
            shop.setRejectionReason(null);

            // Chuyển role user thành VENDOR khi approve
            User vendor = userRepository.findById(shop.getVendorId())
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
            vendor.setRole(Role.VENDOR);
            userRepository.save(vendor);

        } else if ("REJECT".equals(request.getStatus())) {
            shop.setStatus(ShopStatus.REJECTED);
            shop.setRejectionReason(request.getRejectionReason());
        }

        Shop updatedShop = shopRepository.save(shop);
        return mapToShopResponse(updatedShop);
    }

    public ShopResponse getShopByVendorId(String vendorId) {
        Shop shop = shopRepository.findByVendorId(vendorId)
                .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));
        return mapToShopResponse(shop);
    }

    public ShopResponse getShopById(String shopId) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));
        int productCount = productRepository.countByShopId(shopId);
        ShopResponse response = mapToShopResponse(shop);
        response.setProductCount(productCount);
        return response;
    }

    public List<ShopResponse> getAllShops(ShopStatus status) {
        List<Shop> shops;
        if (status != null) {
            // Lọc theo trạng thái
            shops = shopRepository.findByStatus(status);
        } else {
            // Lấy tất cả shops
            shops = shopRepository.findAll();
        }
        return shops.stream()
                .map(this::mapToShopResponse)
                .toList();
    }

    // Upload/Update shop logo
    public ShopResponse uploadShopLogo(String shopId, String vendorId, MultipartFile logoFile) throws IOException {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));

        // Kiểm tra quyền sở hữu shop
        if (!shop.getVendorId().equals(vendorId)) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        if (logoFile == null || logoFile.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_INPUT);
        }

        // Delete old logo if exists
        if (shop.getLogoUrl() != null && !shop.getLogoUrl().isEmpty()) {
            s3Service.deleteFile(shop.getLogoUrl());
        }

        // Upload new logo
        String logoUrl = s3Service.uploadFile(logoFile, "shop-logos");
        shop.setLogoUrl(logoUrl);

        Shop savedShop = shopRepository.save(shop);
        return mapToShopResponse(savedShop);
    }

    // Update shop information (JSON data only)
    public ShopResponse updateShop(String shopId, String vendorId, VendorRegistrationRequest request) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));

        // Kiểm tra quyền sở hữu shop
        if (!shop.getVendorId().equals(vendorId)) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        // Update shop information
        if (request.getShopName() != null && !request.getShopName().trim().isEmpty()) {
            shop.setShopName(request.getShopName().trim());
        }

        if (request.getDescription() != null && !request.getDescription().trim().isEmpty()) {
            shop.setDescription(request.getDescription().trim());
        }

        // Update address
        if (request.getProvinceCode() != null && request.getCommuneCode() != null && request.getDetailAddress() != null) {
            // Validate và lấy tên địa chỉ từ JSON
            Province province = addressService.getProvinceByCode(request.getProvinceCode());
            if (province == null) {
                throw new AppException(ErrorCode.INVALID_INPUT);
            }

            Commune commune = addressService.getCommuneByCode(request.getCommuneCode());
            if (commune == null) {
                throw new AppException(ErrorCode.INVALID_INPUT);
            }

            String fullAddress = request.getDetailAddress() + ", " + commune.getName() + ", " + province.getName();
            Shop.Address address = Shop.Address.builder()
                    .street(request.getDetailAddress())
                    .commune(commune.getName())
                    .province(province.getName())
                    .country("Việt Nam")
                    .fullAddress(fullAddress)
                    .isDefault(false)
                    .build();
            shop.setAddress(address);
        }

        if (request.getPhoneNumber() != null && !request.getPhoneNumber().trim().isEmpty()) {
            shop.setPhoneNumber(request.getPhoneNumber().trim());
        }

        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            shop.setEmail(request.getEmail().trim());
        }

        Shop savedShop = shopRepository.save(shop);
        return mapToShopResponse(savedShop);
    }

    // CREATE MULTIPART - Phương thức mới để hỗ trợ multipart form data
    public ShopResponse registerVendorShopMultipart(String vendorId, String shopName, String description,
                                                   String provinceCode, String communeCode,
                                                   String detailAddress, String phoneNumber, String email,
                                                   MultipartFile logoFile) throws IOException {
        // Kiểm tra user có tồn tại không
        User vendor = userRepository.findById(vendorId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Kiểm tra user đã có shop chưa
        if (shopRepository.existsByVendorId(vendorId)) {
            throw new AppException(ErrorCode.SHOP_ALREADY_EXISTS);
        }

        // Validate và lấy tên địa chỉ từ JSON
        Province province = addressService.getProvinceByCode(provinceCode);
        if (province == null) {
            throw new AppException(ErrorCode.INVALID_INPUT);
        }

        Commune commune = addressService.getCommuneByCode(communeCode);
        if (commune == null) {
            throw new AppException(ErrorCode.INVALID_INPUT);
        }

        // Upload logo nếu có
        String logoUrl = null;
        if (logoFile != null && !logoFile.isEmpty()) {
            logoUrl = s3Service.uploadFile(logoFile, "shop-logos");
        }

        // Tạo địa chỉ
        String fullAddress = detailAddress + ", " + commune.getName() + ", " + province.getName();
        Shop.Address address = Shop.Address.builder()
                .street(detailAddress)
                .commune(commune.getName())
                .province(province.getName())
                .country("Việt Nam")
                .fullAddress(fullAddress)
                .isDefault(false)
                .build();

        // Tạo shop mới
        Shop shop = Shop.builder()
                .vendorId(vendorId)
                .shopName(shopName)
                .description(description)
                .logoUrl(logoUrl)
                .address(address)
                .phoneNumber(phoneNumber)
                .email(email)
                .status(ShopStatus.PENDING)
                .rating(0.0)
                .build();

        Shop savedShop = shopRepository.save(shop);

        // Cập nhật role của user thành VENDOR
        vendor.setRole(Role.VENDOR);
        userRepository.save(vendor);

        return mapToShopResponse(savedShop);
    }

    // UPDATE MULTIPART - Phương thức mới để hỗ trợ multipart form data
    public ShopResponse updateShopMultipart(String shopId, String vendorId, String shopName, String description,
                                          String provinceCode, String communeCode,
                                          String detailAddress, String phoneNumber, String email,
                                          MultipartFile logoFile) throws IOException {
        // Tìm shop
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));

        // Kiểm tra quyền sở hữu
        if (!shop.getVendorId().equals(vendorId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        // Update shop name if provided
        if (shopName != null && !shopName.trim().isEmpty()) {
            shop.setShopName(shopName);
        }

        // Update description if provided
        if (description != null) {
            shop.setDescription(description);
        }

        // Update address if provided
        if (provinceCode != null && communeCode != null && detailAddress != null) {
            // Validate và lấy tên địa chỉ từ JSON
            Province province = addressService.getProvinceByCode(provinceCode);
            if (province == null) {
                throw new AppException(ErrorCode.INVALID_INPUT);
            }

            Commune commune = addressService.getCommuneByCode(communeCode);
            if (commune == null) {
                throw new AppException(ErrorCode.INVALID_INPUT);
            }

            String fullAddress = detailAddress + ", " + commune.getName() + ", " + province.getName();
            Shop.Address address = Shop.Address.builder()
                    .street(detailAddress)
                    .commune(commune.getName())
                    .province(province.getName())
                    .country("Việt Nam")
                    .fullAddress(fullAddress)
                    .isDefault(false)
                    .build();
            shop.setAddress(address);
        }

        // Update phone number if provided
        if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
            shop.setPhoneNumber(phoneNumber);
        }

        // Update email if provided
        if (email != null && !email.trim().isEmpty()) {
            shop.setEmail(email);
        }

        // Update logo if provided
        if (logoFile != null && !logoFile.isEmpty()) {
            // Xóa logo cũ nếu có
            if (shop.getLogoUrl() != null && !shop.getLogoUrl().isEmpty()) {
                s3Service.deleteFile(shop.getLogoUrl());
            }
            // Upload logo mới
            String newLogoUrl = s3Service.uploadFile(logoFile, "shop-logos");
            shop.setLogoUrl(newLogoUrl);
        }

        Shop updatedShop = shopRepository.save(shop);
        return mapToShopResponse(updatedShop);
    }

    // UPDATE MY SHOP - Vendor cập nhật shop của mình (lấy shopId từ vendorId)
    public ShopResponse updateMyShop(String vendorId, String shopName, String description,
                                     String provinceCode, String communeCode,
                                     String detailAddress, String phoneNumber, String email,
                                     MultipartFile logoFile) throws IOException {
        // Tìm shop của vendor
        Shop shop = shopRepository.findByVendorId(vendorId)
                .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));

        // Update shop name if provided
        if (shopName != null && !shopName.trim().isEmpty()) {
            shop.setShopName(shopName);
        }

        // Update description if provided
        if (description != null) {
            shop.setDescription(description);
        }

        // Update address if provided
        if (provinceCode != null && communeCode != null && detailAddress != null) {
            Province province = addressService.getProvinceByCode(provinceCode);
            if (province == null) {
                throw new AppException(ErrorCode.INVALID_INPUT);
            }

            Commune commune = addressService.getCommuneByCode(communeCode);
            if (commune == null) {
                throw new AppException(ErrorCode.INVALID_INPUT);
            }

            String fullAddress = detailAddress + ", " + commune.getName() + ", " + province.getName();
            Shop.Address address = Shop.Address.builder()
                    .street(detailAddress)
                    .commune(commune.getName())
                    .province(province.getName())
                    .country("Việt Nam")
                    .fullAddress(fullAddress)
                    .isDefault(false)
                    .build();
            shop.setAddress(address);
        }

        // Update phone number if provided
        if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
            shop.setPhoneNumber(phoneNumber);
        }

        // Update email if provided
        if (email != null && !email.trim().isEmpty()) {
            shop.setEmail(email);
        }

        // Update logo if provided
        if (logoFile != null && !logoFile.isEmpty()) {
            if (shop.getLogoUrl() != null && !shop.getLogoUrl().isEmpty()) {
                s3Service.deleteFile(shop.getLogoUrl());
            }
            String newLogoUrl = s3Service.uploadFile(logoFile, "shop-logos");
            shop.setLogoUrl(newLogoUrl);
        }

        Shop updatedShop = shopRepository.save(shop);
        return mapToShopResponse(updatedShop);
    }

    // UPDATE SHOP BY ADMIN - Admin cập nhật bất kỳ shop nào
    public ShopResponse updateShopByAdmin(String shopId, String shopName, String description,
                                          String provinceCode, String communeCode,
                                          String detailAddress, String phoneNumber, String email,
                                          MultipartFile logoFile) throws IOException {
        // Tìm shop
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));

        // Update shop name if provided
        if (shopName != null && !shopName.trim().isEmpty()) {
            shop.setShopName(shopName);
        }

        // Update description if provided
        if (description != null) {
            shop.setDescription(description);
        }

        // Update address if provided
        if (provinceCode != null && communeCode != null && detailAddress != null) {
            Province province = addressService.getProvinceByCode(provinceCode);
            if (province == null) {
                throw new AppException(ErrorCode.INVALID_INPUT);
            }

            Commune commune = addressService.getCommuneByCode(communeCode);
            if (commune == null) {
                throw new AppException(ErrorCode.INVALID_INPUT);
            }

            String fullAddress = detailAddress + ", " + commune.getName() + ", " + province.getName();
            Shop.Address address = Shop.Address.builder()
                    .street(detailAddress)
                    .commune(commune.getName())
                    .province(province.getName())
                    .country("Việt Nam")
                    .fullAddress(fullAddress)
                    .isDefault(false)
                    .build();
            shop.setAddress(address);
        }

        // Update phone number if provided
        if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
            shop.setPhoneNumber(phoneNumber);
        }

        // Update email if provided
        if (email != null && !email.trim().isEmpty()) {
            shop.setEmail(email);
        }

        // Update logo if provided
        if (logoFile != null && !logoFile.isEmpty()) {
            if (shop.getLogoUrl() != null && !shop.getLogoUrl().isEmpty()) {
                s3Service.deleteFile(shop.getLogoUrl());
            }
            String newLogoUrl = s3Service.uploadFile(logoFile, "shop-logos");
            shop.setLogoUrl(newLogoUrl);
        }

        Shop updatedShop = shopRepository.save(shop);
        return mapToShopResponse(updatedShop);
    }

    private ShopResponse mapToShopResponse(Shop shop) {
        ShopResponse.AddressInfo addressInfo = null;
        if (shop.getAddress() != null) {
            addressInfo = ShopResponse.AddressInfo.builder()
                    .street(shop.getAddress().getStreet())
                    .commune(shop.getAddress().getCommune())
                    .province(shop.getAddress().getProvince())
                    .country(shop.getAddress().getCountry())
                    .fullAddress(shop.getAddress().getFullAddress())
                    .isDefault(shop.getAddress().isDefault())
                    .build();
        }

        return ShopResponse.builder()
                .id(shop.getId())
                .vendorId(shop.getVendorId())
                .shopName(shop.getShopName())
                .description(shop.getDescription())
                .logoUrl(shop.getLogoUrl())
                .address(addressInfo)
                .phoneNumber(shop.getPhoneNumber())
                .email(shop.getEmail())
                .status(shop.getStatus())
                .rating(shop.getRating())
                .rejectionReason(shop.getRejectionReason())
                .createdAt(shop.getCreatedAt())
                .updatedAt(shop.getUpdatedAt())
                .build();
    }
}
