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
