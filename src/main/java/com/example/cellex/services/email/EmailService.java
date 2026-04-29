package com.example.cellex.services.email;

import com.example.cellex.exceptions.AppException;
import com.example.cellex.exceptions.ErrorCode;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine; // Inject thêm Template Engine

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${NOTIFICATION_EMAIL_ENABLED:true}")
    private boolean notificationEmailEnabled;

    /**
     * Gửi mã OTP (Giữ nguyên logic cũ của bạn)
     */
    public void sendOtpEmail(String to, String otp) {
        try {
            validateEmailConfig();

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject("Mã xác thực Cellex của bạn");
            message.setText("Mã OTP của bạn là: " + otp + "\n\nMã này sẽ hết hạn sau 5 phút.\n\nVui lòng không chia sẻ mã này với bất kỳ ai.");

            mailSender.send(message);
            log.info("OTP email sent successfully to: {}", to);
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to send OTP email to: {}. Error: {}", to, e.getMessage(), e);
            throw new AppException(ErrorCode.EMAIL_SEND_FAILED);
        }
    }

    /**
     * Gửi thông báo HTML (Mới thêm)
     * Hàm này sẽ được gọi từ NotificationEventListener
     */
    @Async // Chạy bất đồng bộ để không block luồng chính
    public void sendNotificationEmail(String to, String subject, String content, String actionUrl, String userName) {
        if (!notificationEmailEnabled) {
            log.info("Notification email is disabled by config. Skip sending to: {}", to);
            return;
        }

        try {
            validateEmailConfig();

            log.info("Sending notification email to: {}", to);
            MimeMessage message = mailSender.createMimeMessage();
            // Sử dụng MULTIPART_MODE_MIXED_RELATED để hỗ trợ HTML và ảnh (nếu có)
            MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name());

            // Tạo Context để truyền dữ liệu vào Template
            Context context = new Context();
            context.setVariable("title", subject);
            context.setVariable("message", content);
            context.setVariable("actionUrl", actionUrl);
            context.setVariable("userName", userName != null ? userName : "Bạn");

            // Render file HTML: src/main/resources/templates/email-notification.html
            String htmlContent = templateEngine.process("email-notification", context);

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true); // true = isHtml

            mailSender.send(message);
            log.info("Notification email sent successfully to: {}", to);

        } catch (MessagingException e) {
            log.error("Failed to build email message for: {}", to, e);
            // Với notification async, ta thường log lỗi chứ không throw exception để tránh crash luồng listener
        } catch (Exception e) {
            log.error("Failed to send notification email to: {}", to, e);
        }
    }

    private void validateEmailConfig() {
        if (fromEmail == null || fromEmail.equals("your-email@gmail.com")) {
            log.error("Email configuration not properly set. fromEmail: {}", fromEmail);
            throw new AppException(ErrorCode.EMAIL_SEND_FAILED);
        }
    }
}