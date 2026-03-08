package com.example.cellex.services.address;

import com.example.cellex.dtos.request.address.CreateUserAddressRequest;
import com.example.cellex.dtos.request.address.UpdateUserAddressRequest;
import com.example.cellex.dtos.response.address.UserAddressResponse;
import com.example.cellex.exceptions.AppException;
import com.example.cellex.exceptions.ErrorCode;
import com.example.cellex.models.address.NewProvince;
import com.example.cellex.models.address.NewWard;
import com.example.cellex.models.jpa.UserAddressEntity;
import com.example.cellex.models.user.User;
import com.example.cellex.repositories.jpa.JpaUserAddressRepository;
import com.example.cellex.repositories.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserAddressService {

    private static final int MAX_ADDRESSES_PER_USER = 10;

    private final JpaUserAddressRepository addressRepository;
    private final UserRepository userRepository;
    private final AddressService addressService;

    /**
     * Get all addresses for the authenticated user.
     */
    public List<UserAddressResponse> getUserAddresses(String userId) {
        UUID userUuid = parseUuid(userId);
        List<UserAddressEntity> addresses = addressRepository.findByUserUuidOrderByIsDefaultDescCreatedAtDesc(userUuid);
        return addresses.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get a single address by ID, verifying ownership.
     */
    public UserAddressResponse getAddressById(String userId, String addressId) {
        UUID userUuid = parseUuid(userId);
        UUID addrUuid = parseUuid(addressId);
        UserAddressEntity address = addressRepository.findByIdAndUserUuid(addrUuid, userUuid)
                .orElseThrow(() -> new AppException(ErrorCode.ADDRESS_NOT_FOUND));
        return mapToResponse(address);
    }

    /**
     * Create a new address for the authenticated user.
     */
    @Transactional
    public UserAddressResponse createAddress(String userId, CreateUserAddressRequest request) {
        UUID userUuid = parseUuid(userId);

        // Check address limit
        long count = addressRepository.countByUserUuid(userUuid);
        if (count >= MAX_ADDRESSES_PER_USER) {
            throw new AppException(ErrorCode.ADDRESS_LIMIT_EXCEEDED);
        }

        User user = userRepository.findById(userUuid)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Resolve address names from codes
        AddressInfo info = resolveAddressInfo(request.getCommuneCode(), request.getProvinceCode(), request.getDetailAddress());

        UserAddressEntity entity = UserAddressEntity.builder()
                .user(user)
                .provinceCode(info.provinceCode)
                .provinceName(info.provinceName)
                .communeCode(info.communeCode)
                .communeName(info.communeName)
                .detailAddress(info.detailAddress)
                .fullAddress(info.fullAddress)
                .tag(request.getTag())
                .isDefault(false)
                .build();

        // Handle default address logic
        if (request.isDefault() || count == 0) {
            // Reset other defaults, then set this one
            addressRepository.resetDefaultAddresses(userUuid);
            entity.setDefault(true);
        }

        UserAddressEntity saved = addressRepository.save(entity);
        log.info("Address created for user {}: {}", userId, saved.getId());
        return mapToResponse(saved);
    }

    /**
     * Update an existing address, verifying ownership.
     */
    @Transactional
    public UserAddressResponse updateAddress(String userId, String addressId, UpdateUserAddressRequest request) {
        UUID userUuid = parseUuid(userId);
        UUID addrUuid = parseUuid(addressId);

        UserAddressEntity entity = addressRepository.findByIdAndUserUuid(addrUuid, userUuid)
                .orElseThrow(() -> new AppException(ErrorCode.ADDRESS_NOT_FOUND));

        // Update commune/province if provided
        if (request.getCommuneCode() != null || request.getProvinceCode() != null || request.getDetailAddress() != null) {
            String newCommuneCode = request.getCommuneCode() != null ? request.getCommuneCode() : entity.getCommuneCode();
            String newProvinceCode = request.getProvinceCode() != null ? request.getProvinceCode() : entity.getProvinceCode();
            String newDetailAddress = request.getDetailAddress() != null ? request.getDetailAddress() : entity.getDetailAddress();

            AddressInfo info = resolveAddressInfo(newCommuneCode, newProvinceCode, newDetailAddress);
            entity.setProvinceCode(info.provinceCode);
            entity.setProvinceName(info.provinceName);
            entity.setCommuneCode(info.communeCode);
            entity.setCommuneName(info.communeName);
            entity.setDetailAddress(info.detailAddress);
            entity.setFullAddress(info.fullAddress);
        }

        // Update tag (allow setting to null)
        if (request.getTag() != null) {
            entity.setTag(request.getTag().trim().isEmpty() ? null : request.getTag().trim());
        }

        // Handle default address logic
        if (Boolean.TRUE.equals(request.getIsDefault()) && !entity.isDefault()) {
            addressRepository.resetDefaultAddresses(userUuid);
            entity.setDefault(true);
        }

        UserAddressEntity saved = addressRepository.save(entity);
        log.info("Address updated for user {}: {}", userId, saved.getId());
        return mapToResponse(saved);
    }

    /**
     * Delete an address, verifying ownership. If deleted address was default,
     * promote the most recent remaining address.
     */
    @Transactional
    public void deleteAddress(String userId, String addressId) {
        UUID userUuid = parseUuid(userId);
        UUID addrUuid = parseUuid(addressId);

        UserAddressEntity entity = addressRepository.findByIdAndUserUuid(addrUuid, userUuid)
                .orElseThrow(() -> new AppException(ErrorCode.ADDRESS_NOT_FOUND));

        boolean wasDefault = entity.isDefault();
        addressRepository.delete(entity);
        log.info("Address deleted for user {}: {}", userId, addressId);

        // If we deleted the default address, promote the newest remaining one
        if (wasDefault) {
            List<UserAddressEntity> remaining = addressRepository.findByUserUuidOrderByIsDefaultDescCreatedAtDesc(userUuid);
            if (!remaining.isEmpty()) {
                UserAddressEntity newDefault = remaining.get(0);
                newDefault.setDefault(true);
                addressRepository.save(newDefault);
                log.info("Promoted address {} as new default for user {}", newDefault.getId(), userId);
            }
        }
    }

    // ==================== Internal helpers ====================

    private AddressInfo resolveAddressInfo(String communeCode, String provinceCode, String detailAddress) {
        AddressInfo info = new AddressInfo();
        info.detailAddress = detailAddress != null ? detailAddress.trim() : null;

        // Resolve from communeCode (new ward code)
        if (communeCode != null && !communeCode.trim().isEmpty()) {
            info.communeCode = communeCode.trim();
            NewWard ward = addressService.getNewWardByCode(communeCode.trim());
            if (ward != null) {
                info.communeName = ward.getName();
                if (ward.getProvinceCode() != null) {
                    NewProvince province = addressService.getNewProvinceByCode(ward.getProvinceCode());
                    if (province != null) {
                        info.provinceCode = ward.getProvinceCode();
                        info.provinceName = province.getName();
                    }
                }
            } else {
                throw new AppException(ErrorCode.INVALID_COMMUNE_CODE);
            }
        } else if (provinceCode != null && !provinceCode.trim().isEmpty()) {
            info.provinceCode = provinceCode.trim();
            NewProvince province = addressService.getNewProvinceByCode(provinceCode.trim());
            if (province != null) {
                info.provinceName = province.getName();
            } else {
                throw new AppException(ErrorCode.INVALID_PROVINCE_CODE);
            }
        }

        // Build full address
        info.fullAddress = buildFullAddress(info.provinceName, info.communeName, info.detailAddress);
        return info;
    }

    private String buildFullAddress(String provinceName, String communeName, String detailAddress) {
        StringBuilder sb = new StringBuilder();
        if (detailAddress != null && !detailAddress.trim().isEmpty()) {
            sb.append(detailAddress.trim());
        }
        if (communeName != null && !communeName.trim().isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(communeName);
        }
        if (provinceName != null && !provinceName.trim().isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(provinceName);
        }
        return sb.toString();
    }

    private UserAddressResponse mapToResponse(UserAddressEntity entity) {
        return UserAddressResponse.builder()
                .id(entity.getId().toString())
                .userId(entity.getUser().getUuid().toString())
                .tag(entity.getTag())
                .street(entity.getDetailAddress())
                .commune(entity.getCommuneName())
                .province(entity.getProvinceName())
                .provinceCode(entity.getProvinceCode())
                .communeCode(entity.getCommuneCode())
                .country("Việt Nam")
                .fullAddress(entity.getFullAddress())
                .isDefault(entity.isDefault())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private UUID parseUuid(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.INVALID_INPUT);
        }
    }

    private static class AddressInfo {
        String provinceCode;
        String provinceName;
        String communeCode;
        String communeName;
        String detailAddress;
        String fullAddress;
    }
}
