package com.example.cellex.services;

import com.example.cellex.exceptions.AppException;
import com.example.cellex.exceptions.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendOtpEmail(String to, String otp) {
        try {
            // Validate email configuration first
            if (fromEmail == null || fromEmail.equals("your-email@gmail.com")) {
                log.error("Email configuration not properly set. fromEmail: {}", fromEmail);
                throw new AppException(ErrorCode.EMAIL_SEND_FAILED);
            }

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject("Your Cellex Verification Code");
            message.setText("Your OTP code is: " + otp + "\n\nThis code will expire in 5 minutes.\n\nPlease do not share this code with anyone.");

            mailSender.send(message);
            log.info("OTP email sent successfully to: {}", to);

        } catch (AppException e) {
            // Re-throw AppException to maintain proper error code
            throw e;
        } catch (Exception e) {
            log.error("Failed to send email to: {}. Error: {}", to, e.getMessage(), e);
            throw new AppException(ErrorCode.EMAIL_SEND_FAILED);
        }
    }
}