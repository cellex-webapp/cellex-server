package com.example.cellex.services.inventory;

import com.example.cellex.dtos.request.inventory.CreateSupplierRequest;
import com.example.cellex.dtos.request.inventory.UpdateSupplierRequest;
import com.example.cellex.dtos.response.PageResponse;
import com.example.cellex.dtos.response.inventory.SupplierResponse;
import com.example.cellex.enums.Role;
import com.example.cellex.exceptions.AppException;
import com.example.cellex.exceptions.ErrorCode;
import com.example.cellex.models.inventory.Supplier;
import com.example.cellex.models.shop.Shop;
import com.example.cellex.models.shop.ShopStaffMember;
import com.example.cellex.repositories.inventory.SupplierRepository;
import com.example.cellex.repositories.shop.ShopRepository;
import com.example.cellex.repositories.shop.ShopStaffMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final ShopRepository shopRepository;
    private final ShopStaffMemberRepository shopStaffMemberRepository;

    public PageResponse<SupplierResponse> getSuppliers(
            String userId,
            Role role,
            String requestedShopId,
            String search,
            Pageable pageable
    ) {
        String shopId = resolveShopId(userId, role, requestedShopId, false);

        Page<Supplier> page;
        if (shopId != null) {
            UUID shopUuid = UUID.fromString(shopId);
            if (search != null && !search.isBlank()) {
                page = supplierRepository.findByShopUuidAndNameContaining(shopUuid, search.trim(), pageable);
            } else {
                page = supplierRepository.findByShopUuidAndIsActiveTrue(shopUuid, pageable);
            }
        } else {
            if (search != null && !search.isBlank()) {
                page = supplierRepository.findByIsActiveTrueAndSupplierNameContainingIgnoreCase(search.trim(), pageable);
            } else {
                page = supplierRepository.findByIsActiveTrue(pageable);
            }
        }

        return PageResponse.of(page, this::toResponse);
    }

    @Transactional
    public SupplierResponse createSupplier(String userId, Role role, CreateSupplierRequest request) {
        String shopId = resolveShopId(userId, role, request.getShopId(), true);
        UUID shopUuid = UUID.fromString(shopId);

        validateDuplicate(shopUuid, request.getPhoneNumber(), request.getTaxCode(), null);

        Supplier supplier = Supplier.builder()
                .shopUuid(shopUuid)
                .supplierName(trimOrNull(request.getSupplierName()))
                .phoneNumber(trimOrNull(request.getPhoneNumber()))
                .email(trimOrNull(request.getEmail()))
                .address(trimOrNull(request.getAddress()))
                .taxCode(trimOrNull(request.getTaxCode()))
                .isActive(true)
                .build();

        Supplier saved = supplierRepository.save(supplier);
        log.info("Created supplier {} for shop {}", saved.getId(), shopId);
        return toResponse(saved);
    }

    @Transactional
    public SupplierResponse updateSupplier(String userId, Role role, String supplierId, UpdateSupplierRequest request) {
        Supplier supplier = findAccessibleSupplier(userId, role, supplierId);

        String nextPhone = request.getPhoneNumber() != null ? trimOrNull(request.getPhoneNumber()) : supplier.getPhoneNumber();
        String nextTaxCode = request.getTaxCode() != null ? trimOrNull(request.getTaxCode()) : supplier.getTaxCode();

        validateDuplicate(supplier.getShopUuid(), nextPhone, nextTaxCode, supplier.getId());

        if (request.getSupplierName() != null) {
            supplier.setSupplierName(trimOrNull(request.getSupplierName()));
        }
        if (request.getPhoneNumber() != null) {
            supplier.setPhoneNumber(nextPhone);
        }
        if (request.getEmail() != null) {
            supplier.setEmail(trimOrNull(request.getEmail()));
        }
        if (request.getAddress() != null) {
            supplier.setAddress(trimOrNull(request.getAddress()));
        }
        if (request.getTaxCode() != null) {
            supplier.setTaxCode(nextTaxCode);
        }

        Supplier saved = supplierRepository.save(supplier);
        log.info("Updated supplier {}", supplierId);
        return toResponse(saved);
    }

    public Supplier findAccessibleSupplier(String userId, Role role, String supplierId) {
        if (role == Role.ADMIN) {
            return supplierRepository.findById(supplierId)
                    .filter(Supplier::getIsActive)
                    .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_OWNED, "Nha cung cap khong ton tai"));
        }

        String shopId = resolveShopId(userId, role, null, true);
        return supplierRepository.findByIdAndShopId(supplierId, shopId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_OWNED, "Nha cung cap khong thuoc cua hang cua ban"));
    }

    private void validateDuplicate(UUID shopUuid, String phoneNumber, String taxCode, String excludingSupplierId) {
        if (phoneNumber == null || phoneNumber.isBlank() || taxCode == null || taxCode.isBlank()) {
            throw new AppException(ErrorCode.FIELD_REQUIRED, "So dien thoai va ma so thue la bat buoc");
        }

        boolean phoneDuplicated;
        boolean taxDuplicated;

        if (excludingSupplierId == null) {
            phoneDuplicated = supplierRepository.existsByShopUuidAndPhoneNumber(shopUuid, phoneNumber);
            taxDuplicated = supplierRepository.existsByShopUuidAndTaxCode(shopUuid, taxCode);
        } else {
            UUID supplierUuid = UUID.fromString(excludingSupplierId);
            phoneDuplicated = supplierRepository.existsDuplicatePhoneForUpdate(shopUuid, phoneNumber, supplierUuid);
            taxDuplicated = supplierRepository.existsDuplicateTaxCodeForUpdate(shopUuid, taxCode, supplierUuid);
        }

        if (phoneDuplicated) {
            throw new AppException(ErrorCode.DUPLICATE_VALUE, "So dien thoai da ton tai trong cua hang");
        }

        if (taxDuplicated) {
            throw new AppException(ErrorCode.DUPLICATE_VALUE, "Ma so thue da ton tai trong cua hang");
        }
    }

    private String resolveShopId(String userId, Role role, String requestedShopId, boolean requireShop) {
        if (role == Role.ADMIN) {
            if (requestedShopId != null && !requestedShopId.isBlank()) {
                shopRepository.findById(requestedShopId)
                        .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));
                return requestedShopId;
            }
            if (requireShop) {
                throw new AppException(ErrorCode.FIELD_REQUIRED, "shopId la bat buoc voi ADMIN");
            }
            return null;
        }

        Shop shop = shopRepository.findByVendorId(userId)
                .orElse(null);
        if (shop != null) return shop.getId();
        return shopStaffMemberRepository.findByUserUuidAndIsActiveTrue(java.util.UUID.fromString(userId))
                .map(ShopStaffMember::getShopId)
                .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));
    }

    private SupplierResponse toResponse(Supplier supplier) {
        return SupplierResponse.builder()
                .id(supplier.getId())
                .shopId(supplier.getShopId())
                .supplierName(supplier.getSupplierName())
                .phoneNumber(supplier.getPhoneNumber())
                .email(supplier.getEmail())
                .address(supplier.getAddress())
                .taxCode(supplier.getTaxCode())
                .createdAt(supplier.getCreatedAt())
                .updatedAt(supplier.getUpdatedAt())
                .build();
    }

    private String trimOrNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
