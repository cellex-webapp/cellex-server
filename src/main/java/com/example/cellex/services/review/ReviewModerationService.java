package com.example.cellex.services.review;

import com.example.cellex.enums.NotificationType;
import com.example.cellex.enums.ReviewStatus;
import com.example.cellex.models.review.ModerationResult;
import com.example.cellex.models.review.Review;
import com.example.cellex.models.user.User;
import com.example.cellex.repositories.review.ReviewRepository;
import com.example.cellex.repositories.user.UserRepository;
import com.example.cellex.services.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewModerationService {

    private final OpenAIModerationService openAIModerationService;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    /**
     * Asynchronously moderate a review using OpenAI Moderation API
     * Updates the review status based on moderation result
     */
    @Async
    public void moderateReviewAsync(String reviewId) {
        log.info("Starting async moderation for review: {}", reviewId);

        Optional<Review> optionalReview = reviewRepository.findById(reviewId);
        if (optionalReview.isEmpty()) {
            log.error("Review not found for moderation: {}", reviewId);
            return;
        }

        Review review = optionalReview.get();

        // Only moderate reviews that are pending moderation
        if (review.getStatus() != ReviewStatus.PENDING_MODERATION) {
            log.info("Review {} is not in PENDING_MODERATION status, skipping", reviewId);
            return;
        }

        try {
            ModerationResult result = openAIModerationService.moderateTextSync(review.getComment());
            if (result != null) {
                applyModerationResult(review, result);
            } else {
                log.warn("Moderation API failed for review {}, keeping in PENDING_MODERATION for manual review", reviewId);
                // Keep review in PENDING_MODERATION status for admin to manually review
            }
        } catch (Exception e) {
            log.error("Error during review moderation for review {}: {}", reviewId, e.getMessage(), e);
            // On error, keep review in pending status for manual review
        }
    }

    /**
     * Synchronously moderate a review and return the result
     */
    public ModerationResult moderateReviewSync(Review review) {
        log.info("Starting sync moderation for review: {}", review.getId());

        ModerationResult result = openAIModerationService.moderateTextSync(review.getComment());
        applyModerationResult(review, result);

        return result;
    }

    /**
     * Apply moderation result to review and update status
     */
    private void applyModerationResult(Review review, ModerationResult result) {
        review.setModerationResult(result);

        boolean wasRejected = false;
        String notificationTitle;
        String notificationMessage;

        if (result.getIsFlagged()) {
            review.setStatus(ReviewStatus.REJECTED_AUTO);
            wasRejected = true;
            log.info("Review {} REJECTED_AUTO - Flagged categories: {}", 
                    review.getId(), result.getFlaggedCategories());
            
            notificationTitle = "Đánh giá của bạn đã bị từ chối";
            notificationMessage = generateRejectionReason(result.getFlaggedCategories());
        } else {
            review.setStatus(ReviewStatus.APPROVED);
            log.info("Review {} APPROVED by moderation", review.getId());
            
            notificationTitle = "Đánh giá của bạn đã được duyệt";
            notificationMessage = "Đánh giá của bạn đã được kiểm duyệt và xuất bản thành công. Cảm ơn bạn đã chia sẻ đánh giá!";
        }

        review.setUpdatedAt(LocalDateTime.now());
        reviewRepository.save(review);

        // Send notification to user
        sendModerationNotification(review, notificationTitle, notificationMessage, wasRejected);
    }

    /**
     * Send notification to user about moderation result
     */
    private void sendModerationNotification(Review review, String title, String message, boolean wasRejected) {
        try {
            Optional<User> userOptional = userRepository.findById(review.getUserId());
            if (userOptional.isEmpty()) {
                log.warn("User not found for notification: {}", review.getUserId());
                return;
            }

            User user = userOptional.get();
            String actionUrl = "/reviews/" + review.getId();
            
            notificationService.sendNotificationToUser(
                    user,
                    title,
                    message,
                    NotificationType.REVIEW,
                    "{\"reviewId\":\"" + review.getId() + "\",\"productId\":\"" + review.getProductId() + "\"}",
                    actionUrl,
                    null
            );

            log.info("Moderation notification sent to user {} for review {}", review.getUserId(), review.getId());
        } catch (Exception e) {
            log.error("Failed to send moderation notification for review {}: {}", review.getId(), e.getMessage());
        }
    }

    /**
     * Check if a review is visible to public users
     */
    public boolean isReviewVisibleToPublic(Review review) {
        return review.getStatus() == ReviewStatus.APPROVED || 
               review.getStatus() == ReviewStatus.APPROVED_BY_ADMIN;
    }

    /**
     * Get human-readable rejection reason from moderation result
     */
    public String getReadableRejectionReason(ModerationResult result) {
        if (result == null || result.getFlaggedCategories() == null || result.getFlaggedCategories().isEmpty()) {
            return "Kiểm duyệt nội dung thất bại";
        }

        StringBuilder reason = new StringBuilder("Đánh giá bị từ chối do: ");
        for (int i = 0; i < result.getFlaggedCategories().size(); i++) {
            if (i > 0) {
                reason.append(", ");
            }
            reason.append(formatCategory(result.getFlaggedCategories().get(i)));
        }
        return reason.toString();
    }

    private String formatCategory(String category) {
        return switch (category) {
            case "hate" -> "Ngôn từ thù địch";
            case "hate/threatening" -> "Ngôn từ thù địch có tính đe dọa";
            case "harassment" -> "Quấy rối";
            case "harassment/threatening" -> "Quấy rối có tính đe dọa";
            case "self-harm" -> "Nội dung tự gây hại";
            case "self-harm/intent" -> "Ý định tự gây hại";
            case "self-harm/instructions" -> "Hướng dẫn tự gây hại";
            case "sexual" -> "Nội dung khiêu dâm";
            case "sexual/minors" -> "Nội dung khiêu dâm liên quan đến trẻ em";
            case "violence" -> "Nội dung bạo lực";
            case "violence/graphic" -> "Nội dung bạo lực đồ họa";
            default -> category.replace("/", " - ").replace("-", " ");
        };
    }

    /**
     * Generate user-friendly rejection reason in Vietnamese
     */
    public String generateRejectionReason(List<String> flaggedCategories) {
        if (flaggedCategories == null || flaggedCategories.isEmpty()) {
            return null;
        }

        if (flaggedCategories.size() == 1) {
            return getReasonForCategory(flaggedCategories.get(0));
        }

        // Multiple categories
        StringBuilder reason = new StringBuilder("Đánh giá của bạn chứa nội dung vi phạm: ");
        for (int i = 0; i < flaggedCategories.size(); i++) {
            if (i > 0) {
                reason.append(", ");
            }
            reason.append(getCategoryShortName(flaggedCategories.get(i)));
        }
        return reason.toString();
    }

    private String getReasonForCategory(String category) {
        return switch (category) {
            case "hate", "hate/threatening" -> 
                "Đánh giá của bạn chứa ngôn từ thù địch hoặc phân biệt đối xử";
            case "harassment", "harassment/threatening" -> 
                "Đánh giá của bạn chứa ngôn từ quấy rối hoặc xúc phạm người khác";
            case "self-harm", "self-harm/intent", "self-harm/instructions" -> 
                "Đánh giá của bạn chứa nội dung liên quan đến tự gây hại";
            case "sexual", "sexual/minors" -> 
                "Đánh giá của bạn chứa nội dung khiêu dâm không phù hợp";
            case "violence", "violence/graphic" -> 
                "Đánh giá của bạn chứa nội dung bạo lực hoặc đe dọa";
            default -> "Đánh giá của bạn chứa nội dung không phù hợp";
        };
    }

    private String getCategoryShortName(String category) {
        return switch (category) {
            case "hate", "hate/threatening" -> "ngôn từ thù địch";
            case "harassment", "harassment/threatening" -> "quấy rối/xúc phạm";
            case "self-harm", "self-harm/intent", "self-harm/instructions" -> "nội dung tự gây hại";
            case "sexual", "sexual/minors" -> "nội dung khiêu dâm";
            case "violence", "violence/graphic" -> "bạo lực";
            default -> "nội dung không phù hợp";
        };
    }

    /**
     * Map flagged categories to Vietnamese
     */
    public List<String> mapCategoriesToVietnamese(List<String> categories) {
        if (categories == null || categories.isEmpty()) {
            return null;
        }
        return categories.stream()
                .map(this::formatCategory)
                .toList();
    }
}
