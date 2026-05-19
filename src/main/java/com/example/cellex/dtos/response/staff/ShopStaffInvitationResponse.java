package com.example.cellex.dtos.response.staff;

import com.example.cellex.enums.StaffInvitationStatus;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopStaffInvitationResponse {
    private String id;
    private String shopId;
    private String shopName;
    private String shopRoleId;
    private String shopRoleName;
    private String invitedUserId;
    private String invitedUserName;
    private String invitedUserEmail;
    private StaffInvitationStatus status;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
}
