package com.example.cellex.models.chat;

import com.example.cellex.enums.Role;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

/**
 * Entity đại diện cho một phòng chat giữa hai người dùng
 * Mỗi cặp người dùng chỉ có một phòng chat duy nhất
 */
@Document(collection = "chat_rooms")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@CompoundIndexes({
    @CompoundIndex(name = "participants_idx", def = "{'participant_one_id': 1, 'participant_two_id': 1}", unique = true)
})
public class ChatRoom {

    @Id
    private String id;

    /**
     * ID của người tham gia thứ nhất
     */
    @Indexed
    @Field("participant_one_id")
    private String participantOneId;

    /**
     * ID của người tham gia thứ hai
     */
    @Indexed
    @Field("participant_two_id")
    private String participantTwoId;

    /**
     * Tên của người tham gia thứ nhất (để hiển thị)
     */
    @Field("participant_one_name")
    private String participantOneName;

    /**
     * Tên của người tham gia thứ hai (để hiển thị)
     */
    @Field("participant_two_name")
    private String participantTwoName;

    /**
     * Avatar URL của người tham gia thứ nhất
     */
    @Field("participant_one_avatar")
    private String participantOneAvatar;

    /**
     * Avatar URL của người tham gia thứ hai
     */
    @Field("participant_two_avatar")
    private String participantTwoAvatar;

    /**
     * Role của người tham gia thứ nhất (USER, VENDOR, ADMIN)
     */
    @Field("participant_one_role")
    private Role participantOneRole;

    /**
     * Role của người tham gia thứ hai (USER, VENDOR, ADMIN)
     */
    @Field("participant_two_role")
    private Role participantTwoRole;

    /**
     * Nội dung tin nhắn cuối cùng (để hiển thị preview)
     */
    @Field("last_message")
    private String lastMessage;

    /**
     * ID của người gửi tin nhắn cuối cùng
     */
    @Field("last_message_sender_id")
    private String lastMessageSenderId;

    /**
     * Thời gian của tin nhắn cuối cùng
     */
    @Field("last_message_at")
    private LocalDateTime lastMessageAt;

    /**
     * Số tin nhắn chưa đọc của người tham gia thứ nhất
     */
    @Field("unread_count_one")
    @Builder.Default
    private Integer unreadCountOne = 0;

    /**
     * Số tin nhắn chưa đọc của người tham gia thứ hai
     */
    @Field("unread_count_two")
    @Builder.Default
    private Integer unreadCountTwo = 0;

    /**
     * Phòng chat có đang hoạt động không
     */
    @Field("is_active")
    @Builder.Default
    private Boolean isActive = true;

    /**
     * Thời gian tạo phòng chat
     */
    @CreatedDate
    @Field("created_at")
    private LocalDateTime createdAt;

    /**
     * Thời gian cập nhật phòng chat cuối cùng
     */
    @LastModifiedDate
    @Field("updated_at")
    private LocalDateTime updatedAt;
}
