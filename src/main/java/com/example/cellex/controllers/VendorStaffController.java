package com.example.cellex.controllers;

import com.example.cellex.dtos.response.ApiResponse;
import com.example.cellex.dtos.response.staff.ShopStaffInvitationResponse;
import com.example.cellex.models.shop.ShopRole;
import com.example.cellex.models.shop.ShopStaffInvitation;
import com.example.cellex.models.user.User;
import com.example.cellex.services.staff.ShopStaffService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/vendor")
@RequiredArgsConstructor
public class VendorStaffController {
    private final ShopStaffService shopStaffService;

    @GetMapping("/shop-roles/permissions")
    @PreAuthorize("hasAnyRole('VENDOR','STAFF')")
    public ApiResponse<List<String>> getPermissions() {
        return ApiResponse.<List<String>>builder().code(1000).result(shopStaffService.getPermissionKeys()).build();
    }

    @GetMapping("/shop-roles")
    @PreAuthorize("hasAnyRole('VENDOR','STAFF')")
    public ApiResponse<List<ShopRole>> getRoles(Authentication authentication) {
        String userId = ((User) authentication.getPrincipal()).getId();
        return ApiResponse.<List<ShopRole>>builder().code(1000).result(shopStaffService.getRoles(userId)).build();
    }

    @GetMapping("/shop-roles/{roleId}")
    @PreAuthorize("hasAnyRole('VENDOR','STAFF')")
    public ApiResponse<ShopRole> getRole(Authentication authentication, @PathVariable String roleId) {
        String userId = ((User) authentication.getPrincipal()).getId();
        return ApiResponse.<ShopRole>builder().code(1000).result(shopStaffService.getRole(userId, roleId)).build();
    }

    @PostMapping("/shop-roles")
    @PreAuthorize("hasRole('VENDOR')")
    public ApiResponse<ShopRole> createRole(Authentication authentication, @RequestBody Map<String, Object> body) {
        String userId = ((User) authentication.getPrincipal()).getId();
        ShopRole role = shopStaffService.createRole(userId, String.valueOf(body.get("name")),
                (String) body.get("description"), (List<String>) body.get("permissions"));
        return ApiResponse.<ShopRole>builder().code(1000).message("Tao role thanh cong").result(role).build();
    }

    @PutMapping("/shop-roles/{roleId}")
    @PreAuthorize("hasRole('VENDOR')")
    public ApiResponse<ShopRole> updateRole(Authentication authentication, @PathVariable String roleId, @RequestBody Map<String, Object> body) {
        String userId = ((User) authentication.getPrincipal()).getId();
        ShopRole role = shopStaffService.updateRole(userId, roleId, (String) body.get("name"),
                (String) body.get("description"), (List<String>) body.get("permissions"));
        return ApiResponse.<ShopRole>builder().code(1000).message("Cap nhat role thanh cong").result(role).build();
    }

    @DeleteMapping("/shop-roles/{roleId}")
    @PreAuthorize("hasRole('VENDOR')")
    public ApiResponse<Void> deleteRole(Authentication authentication, @PathVariable String roleId) {
        String userId = ((User) authentication.getPrincipal()).getId();
        shopStaffService.deleteRole(userId, roleId);
        return ApiResponse.<Void>builder().code(1000).message("Xoa role thanh cong").build();
    }

    @GetMapping("/staff/search")
    @PreAuthorize("hasRole('VENDOR')")
    public ApiResponse<List<Map<String, Object>>> searchUsers(Authentication authentication, @RequestParam String keyword) {
        String userId = ((User) authentication.getPrincipal()).getId();
        return ApiResponse.<List<Map<String, Object>>>builder().code(1000).result(shopStaffService.searchUsers(userId, keyword)).build();
    }

    @PostMapping("/staff/invite")
    @PreAuthorize("hasRole('VENDOR')")
    public ApiResponse<ShopStaffInvitation> invite(Authentication authentication, @RequestBody Map<String, String> body) {
        String userId = ((User) authentication.getPrincipal()).getId();
        ShopStaffInvitation invitation = shopStaffService.invite(userId, body.get("userId"), body.get("shopRoleId"));
        return ApiResponse.<ShopStaffInvitation>builder().code(1000).message("Moi nhan vien thanh cong").result(invitation).build();
    }

    @GetMapping("/staff/invitations")
    @PreAuthorize("hasRole('VENDOR')")
    public ApiResponse<List<ShopStaffInvitationResponse>> getInvitations(Authentication authentication) {
        String userId = ((User) authentication.getPrincipal()).getId();
        return ApiResponse.<List<ShopStaffInvitationResponse>>builder().code(1000).result(shopStaffService.getShopInvitationsView(userId)).build();
    }

    @DeleteMapping("/staff/invitations/{id}")
    @PreAuthorize("hasRole('VENDOR')")
    public ApiResponse<Void> revokeInvitation(Authentication authentication, @PathVariable String id) {
        String userId = ((User) authentication.getPrincipal()).getId();
        shopStaffService.revokeInvitation(userId, id);
        return ApiResponse.<Void>builder().code(1000).message("Huy loi moi thanh cong").build();
    }

    @GetMapping("/staff")
    @PreAuthorize("hasRole('VENDOR')")
    public ApiResponse<List<Map<String, Object>>> getStaffMembers(Authentication authentication) {
        String userId = ((User) authentication.getPrincipal()).getId();
        return ApiResponse.<List<Map<String, Object>>>builder().code(1000).result(shopStaffService.getStaffMembers(userId)).build();
    }

    @DeleteMapping("/staff/{userId}")
    @PreAuthorize("hasRole('VENDOR')")
    public ApiResponse<Void> removeStaff(Authentication authentication, @PathVariable String userId) {
        String operatorId = ((User) authentication.getPrincipal()).getId();
        shopStaffService.removeStaffMember(operatorId, userId);
        return ApiResponse.<Void>builder().code(1000).message("Thu hoi quyen nhan vien thanh cong").build();
    }

    @PutMapping("/staff/{userId}/role")
    @PreAuthorize("hasRole('VENDOR')")
    public ApiResponse<Void> changeStaffRole(Authentication authentication, @PathVariable String userId, @RequestBody Map<String, String> body) {
        String operatorId = ((User) authentication.getPrincipal()).getId();
        shopStaffService.changeStaffRole(operatorId, userId, body.get("shopRoleId"));
        return ApiResponse.<Void>builder().code(1000).message("Cap nhat role nhan vien thanh cong").build();
    }
}

