package com.example.cellex.services;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Properties;

@Service
public class EmailService {

    private static final String APPLICATION_NAME = "Cellex";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String USER = "me"; // 'me' đại diện cho tài khoản đã xác thực

    @Value("${google.client.id}")
    private String clientId;
    @Value("${google.client.secret}")
    private String clientSecret;
    @Value("${google.refresh.token}")
    private String refreshToken;

    // Phương thức tạo kết nối đã được xác thực tới Gmail API
    private Gmail getGmailService() throws GeneralSecurityException, IOException {
        final NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();

        // Xây dựng credential từ các thông tin đã lấy
        Credential credential = new GoogleCredential.Builder()
                .setTransport(httpTransport)
                .setJsonFactory(JSON_FACTORY)
                .setClientSecrets(clientId, clientSecret)
                .build();

        credential.setRefreshToken(refreshToken);

        // Tạo đối tượng Gmail service
        return new Gmail.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    // Phương thức gửi email OTP
    public void sendOtpEmail(String to, String otp) {
        try {
            Gmail service = getGmailService();

            // Tạo nội dung email (MimeMessage)
            Properties props = new Properties();
            Session session = Session.getDefaultInstance(props, null);
            MimeMessage email = new MimeMessage(session);
            email.setFrom(new InternetAddress("me"));
            email.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(to));
            email.setSubject("Your Cellex Verification Code");
            email.setText("Your OTP code is: " + otp + "\nThis code will expire in 5 minutes.");

            // Mã hóa email sang định dạng Base64 URL-safe
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            email.writeTo(buffer);
            byte[] rawMessageBytes = buffer.toByteArray();
            String encodedEmail = Base64.encodeBase64URLSafeString(rawMessageBytes);

            // Tạo đối tượng Message của Gmail
            Message message = new Message();
            message.setRaw(encodedEmail);

            // Gửi email qua API
            service.users().messages().send(USER, message).execute();
            System.out.println("OAuth 2.0 Email sent successfully to " + to);

        } catch (GeneralSecurityException | IOException | MessagingException e) {
            // Xử lý lỗi ở đây là rất quan trọng
            // In ra lỗi để debug, trong ứng dụng thực tế nên ném ra một AppException
            e.printStackTrace();
            throw new RuntimeException("Failed to send email using OAuth 2.0", e);
        }
    }
}