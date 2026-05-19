package com.example.cellex.controllers;

import com.example.cellex.dtos.response.ApiResponse;
import com.example.cellex.dtos.response.staff.ShopStaffInvitationResponse;
import com.example.cellex.models.user.User;
import com.example.cellex.services.staff.ShopStaffService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/staff")
@RequiredArgsConstructor
public class StaffInvitationController {
    private final ShopStaffService shopStaffService;

    @GetMapping("/invitations")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<ShopStaffInvitationResponse>> getInvitations(Authentication authentication) {
        String userId = ((User) authentication.getPrincipal()).getId();
        return ApiResponse.<List<ShopStaffInvitationResponse>>builder().code(1000).result(shopStaffService.getMyPendingInvitationsView(userId)).build();
    }

    @PostMapping("/invitations/{id}/accept")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> accept(Authentication authentication, @PathVariable String id) {
        String userId = ((User) authentication.getPrincipal()).getId();
        shopStaffService.acceptInvitation(userId, id);
        return ApiResponse.<Void>builder().code(1000).message("Da chap nhan loi moi").build();
    }

    @PostMapping("/invitations/{id}/decline")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> decline(Authentication authentication, @PathVariable String id) {
        String userId = ((User) authentication.getPrincipal()).getId();
        shopStaffService.declineInvitation(userId, id);
        return ApiResponse.<Void>builder().code(1000).message("Da tu choi loi moi").build();
    }

    @PostMapping("/leave")
    @PreAuthorize("hasRole('STAFF')")
    public ApiResponse<Void> leaveShop(Authentication authentication, @RequestBody Map<String, String> body) {
        String userId = ((User) authentication.getPrincipal()).getId();
        shopStaffService.leaveShop(userId, body.get("shopId"));
        return ApiResponse.<Void>builder().code(1000).message("Da roi khoi shop").build();
    }

    @GetMapping("/my-shop")
    @PreAuthorize("hasRole('STAFF')")
    public ApiResponse<Map<String, Object>> myShop(Authentication authentication) {
        String userId = ((User) authentication.getPrincipal()).getId();
        return ApiResponse.<Map<String, Object>>builder().code(1000).result(shopStaffService.getMyShopContext(userId)).build();
    }
}

