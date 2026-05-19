package com.example.cellex.services.staff;

import com.example.cellex.enums.Role;
import com.example.cellex.exceptions.AppException;
import com.example.cellex.exceptions.ErrorCode;
import com.example.cellex.models.shop.ShopRole;
import com.example.cellex.models.shop.ShopStaffMember;
import com.example.cellex.models.user.User;
import com.example.cellex.repositories.shop.ShopRoleRepository;
import com.example.cellex.repositories.shop.ShopStaffMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StaffPermissionService {
    private final ShopStaffMemberRepository staffMemberRepository;
    private final ShopRoleRepository shopRoleRepository;

    public List<String> getPermissions(String userId) {
        UUID uid = UUID.fromString(userId);
        ShopStaffMember member = staffMemberRepository.findByUserUuidAndIsActiveTrue(uid).orElse(null);
        if (member == null) return Collections.emptyList();
        ShopRole role = shopRoleRepository.findById(member.getShopRoleUuid()).orElse(null);
        if (role == null || role.getPermissions() == null) return Collections.emptyList();
        return role.getPermissions();
    }

    public boolean hasPermission(String userId, String permissionKey) {
        return getPermissions(userId).contains(permissionKey);
    }

    public String getStaffShopId(String userId) {
        return staffMemberRepository.findByUserUuidAndIsActiveTrue(UUID.fromString(userId))
                .map(ShopStaffMember::getShopId)
                .orElse(null);
    }

    public String resolveShopIdForVendorOrStaff(User user, String requiredPermission) {
        if (user.getRole() == Role.VENDOR) {
            return null;
        }
        if (user.getRole() != Role.STAFF) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        if (!hasPermission(user.getId(), requiredPermission)) {
            throw new AppException(ErrorCode.INSUFFICIENT_STAFF_PERMISSION);
        }
        String shopId = getStaffShopId(user.getId());
        if (shopId == null) {
            throw new AppException(ErrorCode.STAFF_MEMBER_NOT_FOUND);
        }
        return shopId;
    }
}
