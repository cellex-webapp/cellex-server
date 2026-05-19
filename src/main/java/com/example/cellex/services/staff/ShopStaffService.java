package com.example.cellex.services.staff;

import com.example.cellex.dtos.response.staff.ShopStaffInvitationResponse;
import com.example.cellex.enums.NotificationType;
import com.example.cellex.enums.Role;
import com.example.cellex.enums.StaffInvitationStatus;
import com.example.cellex.enums.VendorPermission;
import com.example.cellex.exceptions.AppException;
import com.example.cellex.exceptions.ErrorCode;
import com.example.cellex.models.shop.Shop;
import com.example.cellex.models.shop.ShopRole;
import com.example.cellex.models.shop.ShopStaffInvitation;
import com.example.cellex.models.shop.ShopStaffMember;
import com.example.cellex.models.user.User;
import com.example.cellex.repositories.shop.*;
import com.example.cellex.repositories.user.UserRepository;
import com.example.cellex.services.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShopStaffService {
    private final ShopRepository shopRepository;
    private final UserRepository userRepository;
    private final ShopRoleRepository shopRoleRepository;
    private final ShopStaffInvitationRepository invitationRepository;
    private final ShopStaffMemberRepository staffMemberRepository;
    private final NotificationService notificationService;

    public List<String> getPermissionKeys() {
        return VendorPermission.allKeys();
    }

    public List<ShopRole> getRoles(String vendorId) {
        UUID shopUuid = resolveManagedShopUuid(vendorId);
        return shopRoleRepository.findByShopUuidAndIsActiveTrue(shopUuid);
    }

    public ShopRole getRole(String operatorId, String roleId) {
        return shopRoleRepository.findByUuidAndShopUuidAndIsActiveTrue(UUID.fromString(roleId), resolveManagedShopUuid(operatorId))
                .orElseThrow(() -> new AppException(ErrorCode.SHOP_ROLE_NOT_FOUND));
    }

    @Transactional
    public ShopRole createRole(String vendorId, String name, String description, List<String> permissions) {
        UUID shopUuid = resolveManagedShopUuid(vendorId);
        if (shopRoleRepository.existsByShopUuidAndNameIgnoreCaseAndIsActiveTrue(shopUuid, name)) {
            throw new AppException(ErrorCode.SHOP_ROLE_ALREADY_EXISTS);
        }
        return shopRoleRepository.save(ShopRole.builder()
                .shopUuid(shopUuid).name(name.trim()).description(description)
                .permissions(normalizePermissions(permissions)).isActive(true).build());
    }

    @Transactional
    public ShopRole updateRole(String vendorId, String roleId, String name, String description, List<String> permissions) {
        UUID shopUuid = resolveManagedShopUuid(vendorId);
        ShopRole role = shopRoleRepository.findByUuidAndShopUuidAndIsActiveTrue(UUID.fromString(roleId), shopUuid)
                .orElseThrow(() -> new AppException(ErrorCode.SHOP_ROLE_NOT_FOUND));
        if (name != null && !name.isBlank() && !role.getName().equalsIgnoreCase(name.trim())
                && shopRoleRepository.existsByShopUuidAndNameIgnoreCaseAndIsActiveTrue(shopUuid, name.trim())) {
            throw new AppException(ErrorCode.SHOP_ROLE_ALREADY_EXISTS);
        }
        if (name != null && !name.isBlank()) role.setName(name.trim());
        role.setDescription(description);
        role.setPermissions(normalizePermissions(permissions));
        return shopRoleRepository.save(role);
    }

    @Transactional
    public void deleteRole(String vendorId, String roleId) {
        UUID shopUuid = resolveManagedShopUuid(vendorId);
        ShopRole role = shopRoleRepository.findByUuidAndShopUuidAndIsActiveTrue(UUID.fromString(roleId), shopUuid)
                .orElseThrow(() -> new AppException(ErrorCode.SHOP_ROLE_NOT_FOUND));
        if (staffMemberRepository.countByShopRoleUuidAndIsActiveTrue(role.getUuid()) > 0) {
            throw new AppException(ErrorCode.SHOP_ROLE_IN_USE);
        }
        role.setActive(false);
        shopRoleRepository.save(role);
    }

    @Transactional
    public ShopStaffInvitation invite(String vendorId, String userId, String roleId) {
        Shop shop = shopRepository.findById(resolveManagedShopUuid(vendorId)).orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));
        User user = userRepository.findById(userId).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        if (user.getRole() == Role.VENDOR || user.getRole() == Role.ADMIN) throw new AppException(ErrorCode.CANNOT_INVITE_VENDOR_OR_ADMIN);
        UUID shopUuid = UUID.fromString(shop.getId());
        UUID invitedUser = UUID.fromString(userId);
        if (staffMemberRepository.findByShopUuidAndUserUuidAndIsActiveTrue(shopUuid, invitedUser).isPresent()) throw new AppException(ErrorCode.USER_ALREADY_STAFF_OF_SHOP);
        if (invitationRepository.existsByShopUuidAndInvitedUserUuidAndStatus(shopUuid, invitedUser, StaffInvitationStatus.PENDING)) throw new AppException(ErrorCode.USER_ALREADY_INVITED);
        ShopRole role = shopRoleRepository.findByUuidAndShopUuidAndIsActiveTrue(UUID.fromString(roleId), shopUuid).orElseThrow(() -> new AppException(ErrorCode.SHOP_ROLE_NOT_FOUND));

        ShopStaffInvitation invitation = invitationRepository.save(ShopStaffInvitation.builder()
                .shopUuid(shopUuid).shopRoleUuid(UUID.fromString(role.getId())).invitedUserUuid(invitedUser)
                .invitedByUuid(UUID.fromString(vendorId)).status(StaffInvitationStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusDays(7)).build());

        notificationService.sendNotificationToUser(user, "Lời mời làm nhân viên",
                "Bạn được mời làm nhân viên cho shop " + shop.getShopName() + " với role " + role.getName(),
                NotificationType.SYSTEM, null, "/invitations", null);
        return invitation;
    }

    public List<Map<String, Object>> searchUsers(String operatorId, String keyword) {
        resolveManagedShopUuid(operatorId);
        if (keyword == null || keyword.isBlank()) return List.of();
        return userRepository.searchByFullNameOrEmail(keyword.trim(), PageRequest.of(0, 10)).stream()
                .map(u -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", u.getId());
                    m.put("fullName", u.getFullName());
                    m.put("email", u.getEmail());
                    m.put("avatarUrl", u.getAvatarUrl());
                    return m;
                }).collect(Collectors.toList());
    }

    public List<ShopStaffInvitation> getShopInvitations(String operatorId) {
        return invitationRepository.findByShopUuidOrderByCreatedAtDesc(resolveManagedShopUuid(operatorId));
    }

    public List<ShopStaffInvitationResponse> getShopInvitationsView(String operatorId) {
        UUID shopUuid = resolveManagedShopUuid(operatorId);
        Shop shop = shopRepository.findById(shopUuid.toString()).orElse(null);
        return invitationRepository.findByShopUuidOrderByCreatedAtDesc(shopUuid).stream()
                .map(invitation -> toInvitationResponse(invitation, shop))
                .collect(Collectors.toList());
    }

    @Transactional
    public void revokeInvitation(String operatorId, String invitationId) {
        UUID shopUuid = resolveManagedShopUuid(operatorId);
        ShopStaffInvitation invitation = invitationRepository.findById(UUID.fromString(invitationId)).orElseThrow(() -> new AppException(ErrorCode.INVITATION_NOT_FOUND));
        if (!shopUuid.equals(invitation.getShopUuid())) throw new AppException(ErrorCode.UNAUTHORIZED);
        if (invitation.getStatus() != StaffInvitationStatus.PENDING) throw new AppException(ErrorCode.INVITATION_ALREADY_PROCESSED);
        invitation.setStatus(StaffInvitationStatus.REVOKED);
        invitation.setRespondedAt(LocalDateTime.now());
        invitationRepository.save(invitation);
    }

    @Transactional
    public void acceptInvitation(String userId, String invitationId) {
        ShopStaffInvitation invitation = invitationRepository.findById(UUID.fromString(invitationId)).orElseThrow(() -> new AppException(ErrorCode.INVITATION_NOT_FOUND));
        if (!invitation.getInvitedUserId().equals(userId)) throw new AppException(ErrorCode.UNAUTHORIZED);
        if (invitation.getStatus() != StaffInvitationStatus.PENDING) throw new AppException(ErrorCode.INVITATION_ALREADY_PROCESSED);
        if (invitation.getExpiresAt().isBefore(LocalDateTime.now())) throw new AppException(ErrorCode.INVITATION_EXPIRED);

        staffMemberRepository.save(ShopStaffMember.builder()
                .shopUuid(invitation.getShopUuid()).userUuid(UUID.fromString(userId)).shopRoleUuid(invitation.getShopRoleUuid())
                .joinedAt(LocalDateTime.now()).isActive(true).build());
        invitation.setStatus(StaffInvitationStatus.ACCEPTED);
        invitation.setRespondedAt(LocalDateTime.now());
        invitationRepository.save(invitation);

        User user = userRepository.findById(userId).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        if (user.getRole() == Role.USER) {
            user.setRole(Role.STAFF);
            userRepository.save(user);
        }
    }

    @Transactional
    public void declineInvitation(String userId, String invitationId) {
        ShopStaffInvitation invitation = invitationRepository.findById(UUID.fromString(invitationId)).orElseThrow(() -> new AppException(ErrorCode.INVITATION_NOT_FOUND));
        if (!invitation.getInvitedUserId().equals(userId)) throw new AppException(ErrorCode.UNAUTHORIZED);
        if (invitation.getStatus() != StaffInvitationStatus.PENDING) throw new AppException(ErrorCode.INVITATION_ALREADY_PROCESSED);
        invitation.setStatus(StaffInvitationStatus.DECLINED);
        invitation.setRespondedAt(LocalDateTime.now());
        invitationRepository.save(invitation);
    }

    public List<ShopStaffInvitation> getMyPendingInvitations(String userId) {
        return invitationRepository.findByInvitedUserUuidAndStatusOrderByCreatedAtDesc(UUID.fromString(userId), StaffInvitationStatus.PENDING);
    }

    public List<ShopStaffInvitationResponse> getMyPendingInvitationsView(String userId) {
        return invitationRepository.findByInvitedUserUuidAndStatusOrderByCreatedAtDesc(UUID.fromString(userId), StaffInvitationStatus.PENDING)
                .stream()
                .map(invitation -> toInvitationResponse(invitation, null))
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getStaffMembers(String operatorId) {
        UUID shopUuid = resolveManagedShopUuid(operatorId);
        List<ShopStaffMember> members = staffMemberRepository.findByShopUuidAndIsActiveTrueOrderByCreatedAtDesc(shopUuid);
        List<Map<String, Object>> result = new ArrayList<>();
        for (ShopStaffMember m : members) {
            User user = userRepository.findById(m.getUserUuid()).orElse(null);
            ShopRole role = shopRoleRepository.findById(m.getShopRoleUuid()).orElse(null);
            Map<String, Object> item = new HashMap<>();
            item.put("userId", m.getUserId());
            item.put("fullName", user != null ? user.getFullName() : null);
            item.put("email", user != null ? user.getEmail() : null);
            item.put("avatarUrl", user != null ? user.getAvatarUrl() : null);
            item.put("role", role != null ? role.getName() : null);
            item.put("joinedAt", m.getJoinedAt());
            item.put("isActive", m.isActive());
            result.add(item);
        }
        return result;
    }

    @Transactional
    public void removeStaffMember(String operatorId, String userId) {
        UUID shopUuid = resolveManagedShopUuid(operatorId);
        ShopStaffMember member = staffMemberRepository.findByShopUuidAndUserUuidAndIsActiveTrue(shopUuid, UUID.fromString(userId))
                .orElseThrow(() -> new AppException(ErrorCode.STAFF_MEMBER_NOT_FOUND));
        member.setActive(false);
        member.setLeftAt(LocalDateTime.now());
        staffMemberRepository.save(member);
        normalizeUserRoleAfterStaffChanged(userId);
    }

    @Transactional
    public void changeStaffRole(String operatorId, String userId, String roleId) {
        UUID shopUuid = resolveManagedShopUuid(operatorId);
        ShopStaffMember member = staffMemberRepository.findByShopUuidAndUserUuidAndIsActiveTrue(shopUuid, UUID.fromString(userId))
                .orElseThrow(() -> new AppException(ErrorCode.STAFF_MEMBER_NOT_FOUND));
        ShopRole role = shopRoleRepository.findByUuidAndShopUuidAndIsActiveTrue(UUID.fromString(roleId), shopUuid)
                .orElseThrow(() -> new AppException(ErrorCode.SHOP_ROLE_NOT_FOUND));
        member.setShopRoleUuid(role.getUuid());
        staffMemberRepository.save(member);
    }

    @Transactional
    public void leaveShop(String userId, String shopId) {
        ShopStaffMember member = staffMemberRepository.findByShopUuidAndUserUuidAndIsActiveTrue(UUID.fromString(shopId), UUID.fromString(userId))
                .orElseThrow(() -> new AppException(ErrorCode.STAFF_MEMBER_NOT_FOUND));
        member.setActive(false);
        member.setLeftAt(LocalDateTime.now());
        staffMemberRepository.save(member);
        normalizeUserRoleAfterStaffChanged(userId);
    }

    public Map<String, Object> getMyShopContext(String userId) {
        ShopStaffMember member = staffMemberRepository.findByUserUuidAndIsActiveTrue(UUID.fromString(userId))
                .orElseThrow(() -> new AppException(ErrorCode.STAFF_MEMBER_NOT_FOUND));
        Shop shop = shopRepository.findById(member.getShopUuid()).orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));
        ShopRole role = shopRoleRepository.findById(member.getShopRoleUuid()).orElseThrow(() -> new AppException(ErrorCode.SHOP_ROLE_NOT_FOUND));
        Map<String, Object> res = new HashMap<>();
        res.put("shopId", shop.getId());
        res.put("shopName", shop.getShopName());
        res.put("shopLogoUrl", shop.getLogoUrl());
        res.put("roleId", role.getId());
        res.put("roleName", role.getName());
        res.put("permissions", role.getPermissions() == null ? List.of() : role.getPermissions());
        return res;
    }

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void expireInvitations() {
        List<ShopStaffInvitation> expired = invitationRepository.findByStatusAndExpiresAtBefore(StaffInvitationStatus.PENDING, LocalDateTime.now());
        expired.forEach(i -> i.setStatus(StaffInvitationStatus.EXPIRED));
        invitationRepository.saveAll(expired);
    }

    private UUID resolveManagedShopUuid(String operatorId) {
        return shopRepository.findByVendorId(operatorId)
                .map(s -> UUID.fromString(s.getId()))
                .orElseGet(() -> staffMemberRepository.findByUserUuidAndIsActiveTrue(UUID.fromString(operatorId))
                        .map(ShopStaffMember::getShopUuid)
                        .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND)));
    }

        private ShopStaffInvitationResponse toInvitationResponse(ShopStaffInvitation invitation, Shop cachedShop) {
        Shop shop = cachedShop != null ? cachedShop : shopRepository.findById(invitation.getShopId()).orElse(null);
        User invitedUser = invitation.getInvitedUserId() != null
            ? userRepository.findById(invitation.getInvitedUserId()).orElse(null)
            : null;
        ShopRole role = invitation.getShopRoleId() != null
            ? shopRoleRepository.findById(UUID.fromString(invitation.getShopRoleId())).orElse(null)
            : null;

        return ShopStaffInvitationResponse.builder()
            .id(invitation.getId())
            .shopId(invitation.getShopId())
            .shopName(shop != null ? shop.getShopName() : null)
            .shopRoleId(invitation.getShopRoleId())
            .shopRoleName(role != null ? role.getName() : null)
            .invitedUserId(invitation.getInvitedUserId())
            .invitedUserName(invitedUser != null ? invitedUser.getFullName() : null)
            .invitedUserEmail(invitedUser != null ? invitedUser.getEmail() : null)
            .status(invitation.getStatus())
            .expiresAt(invitation.getExpiresAt())
            .createdAt(invitation.getCreatedAt())
            .build();
        }

    private List<String> normalizePermissions(List<String> permissions) {
        if (permissions == null) return List.of();
        List<String> allowed = VendorPermission.allKeys();
        return permissions.stream().filter(allowed::contains).distinct().collect(Collectors.toList());
    }

    private void normalizeUserRoleAfterStaffChanged(String userId) {
        if (staffMemberRepository.countByUserUuidAndIsActiveTrue(UUID.fromString(userId)) > 0) return;
        User user = userRepository.findById(userId).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        if (user.getRole() == Role.STAFF) {
            user.setRole(Role.USER);
            userRepository.save(user);
        }
    }
}
