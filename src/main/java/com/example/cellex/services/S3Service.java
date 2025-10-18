package com.example.cellex.services;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class S3Service {

    private final Cloudinary cloudinary;

    @Value("${cloudinary.folder-prefix:}")
    private String folderPrefix;

    public String uploadFile(MultipartFile file, String folder) throws IOException {
        if (file == null || file.isEmpty()) {
            return null; // No file uploaded
        }

        String targetFolder = (folderPrefix != null && !folderPrefix.isEmpty()) ? folderPrefix + "/" + folder : folder;

        Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                "folder", targetFolder,
                "resource_type", "auto"
        ));

        Object secureUrl = uploadResult.get("secure_url");
        return secureUrl != null ? secureUrl.toString() : null;
    }

    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.trim().isEmpty()) return;

        try {
            // Extract public_id from Cloudinary URL
            // Example URL: https://res.cloudinary.com/<cloud_name>/image/upload/v1620000000/folder/subfolder/filename.jpg
            String publicId = extractPublicIdFromUrl(fileUrl);
            if (publicId == null || publicId.isEmpty()) return;

            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (Exception e) {
            System.err.println("Failed to delete file from Cloudinary: " + e.getMessage());
        }
    }

    private String extractPublicIdFromUrl(String url) {
        try {
            // Remove query params
            int q = url.indexOf('?');
            String cleaned = (q >= 0) ? url.substring(0, q) : url;

            // Find "/upload/" then strip optional version segment v12345/
            int uploadIndex = cleaned.indexOf("/upload/");
            if (uploadIndex < 0) return null;

            String afterUpload = cleaned.substring(uploadIndex + "/upload/".length());

            // Remove version if present
            Pattern versionPattern = Pattern.compile("^v\\d+/(.+)$");
            Matcher m = versionPattern.matcher(afterUpload);
            if (m.find()) {
                afterUpload = m.group(1);
            }

            // Remove file extension
            int lastDot = afterUpload.lastIndexOf('.');
            String withoutExt = (lastDot > 0) ? afterUpload.substring(0, lastDot) : afterUpload;

            return withoutExt;
        } catch (Exception e) {
            return null;
        }
    }
}
