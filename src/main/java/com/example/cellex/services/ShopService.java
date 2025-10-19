package com.example.cellex.services;

import com.example.cellex.dtos.request.ShopVerificationRequest;
import com.example.cellex.dtos.request.VendorRegistrationRequest;
import com.example.cellex.dtos.response.ShopResponse;
import com.example.cellex.enums.Role;
import com.example.cellex.exceptions.AppException;
import com.example.cellex.exceptions.ErrorCode;
import com.example.cellex.models.Shop;
import com.example.cellex.models.User;
import com.example.cellex.repositories.ShopRepository;
import com.example.cellex.repositories.UserRepository;
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

    public ShopResponse registerVendorShop(String vendorId, VendorRegistrationRequest request, MultipartFile logoFile) throws IOException {
        // Kiểm tra user có tồn tại không
        User vendor = userRepository.findById(vendorId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Kiểm tra user đã có shop chưa
        if (shopRepository.existsByVendorId(vendorId)) {
            throw new AppException(ErrorCode.SHOP_ALREADY_EXISTS);
        }

        // Upload logo nếu có
        String logoUrl = null;
        if (logoFile != null && !logoFile.isEmpty()) {
            logoUrl = s3Service.uploadFile(logoFile, "shop-logos");
        }

        // Tạo shop mới
        Shop shop = Shop.builder()
                .vendorId(vendorId)
                .shopName(request.getShopName())
                .description(request.getDescription())
                .logoUrl(logoUrl)
                .address(request.getAddress())
                .phoneNumber(request.getPhoneNumber())
                .email(request.getEmail())
                .isVerified(false)
                .rating(0.0)
                .build();

        Shop savedShop = shopRepository.save(shop);
        return mapToShopResponse(savedShop);
    }

    public ShopResponse verifyShop(ShopVerificationRequest request) {
        Shop shop = shopRepository.findById(request.getShopId())
                .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));

        if ("APPROVE".equals(request.getStatus())) {
            shop.setIsVerified(true);
            shop.setRejectionReason(null);

            // Chuyển role user thành VENDOR khi approve
            User vendor = userRepository.findById(shop.getVendorId())
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
            vendor.setRole(Role.VENDOR);
            userRepository.save(vendor);

        } else if ("REJECT".equals(request.getStatus())) {
            shop.setIsVerified(false);
            shop.setRejectionReason(request.getRejectionReason());
        }

        Shop updatedShop = shopRepository.save(shop);
        return mapToShopResponse(updatedShop);
    }

    public List<ShopResponse> getPendingShops() {
        List<Shop> pendingShops = shopRepository.findByIsVerified(false);
        return pendingShops.stream()
                .map(this::mapToShopResponse)
                .toList();
    }

    public ShopResponse getShopByVendorId(String vendorId) {
        Shop shop = shopRepository.findByVendorId(vendorId)
                .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));
        return mapToShopResponse(shop);
    }

    public ShopResponse getShopById(String shopId) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));
        return mapToShopResponse(shop);
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

        if (request.getAddress() != null && !request.getAddress().trim().isEmpty()) {
            shop.setAddress(request.getAddress().trim());
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
                                                   String address, String phoneNumber, String email,
                                                   MultipartFile logoFile) throws IOException {
        // Kiểm tra user có tồn tại không
        User vendor = userRepository.findById(vendorId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Kiểm tra user đã có shop chưa
        if (shopRepository.existsByVendorId(vendorId)) {
            throw new AppException(ErrorCode.SHOP_ALREADY_EXISTS);
        }

        // Upload logo nếu có
        String logoUrl = null;
        if (logoFile != null && !logoFile.isEmpty()) {
            logoUrl = s3Service.uploadFile(logoFile, "shop-logos");
        }

        // Tạo shop mới
        Shop shop = Shop.builder()
                .vendorId(vendorId)
                .shopName(shopName)
                .description(description)
                .logoUrl(logoUrl)
                .address(address)
                .phoneNumber(phoneNumber)
                .email(email)
                .isVerified(false)
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
                                          String address, String phoneNumber, String email,
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
        if (address != null && !address.trim().isEmpty()) {
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

    private ShopResponse mapToShopResponse(Shop shop) {
        return ShopResponse.builder()
                .id(shop.getId())
                .vendorId(shop.getVendorId())
                .shopName(shop.getShopName())
                .description(shop.getDescription())
                .logoUrl(shop.getLogoUrl())
                .address(shop.getAddress())
                .phoneNumber(shop.getPhoneNumber())
                .email(shop.getEmail())
                .isVerified(shop.getIsVerified())
                .rating(shop.getRating())
                .rejectionReason(shop.getRejectionReason())
                .createdAt(shop.getCreatedAt())
                .updatedAt(shop.getUpdatedAt())
                .build();
    }
}
