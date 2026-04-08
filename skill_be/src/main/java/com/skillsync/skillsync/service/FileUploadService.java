package com.skillsync.skillsync.service;

import com.skillsync.skillsync.dto.request.upload.PresignedUploadRequest;
import com.skillsync.skillsync.dto.response.upload.PresignedUploadResponse;
import com.skillsync.skillsync.enums.UploadType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URL;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileUploadService {

    private final S3Presigner s3Presigner;
    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.s3.public-base-url}")
    private String publicBaseUrl;

    private static final long EXPIRES_IN_SECONDS = 300L;

    public PresignedUploadResponse generatePresignedUploadUrl(PresignedUploadRequest request) {
        validateRequest(request);

        String sanitizedFileName = sanitizeFileName(request.getFileName());
        String folder = resolveFolder(request.getUploadType());
        String fileKey = folder + "/" + UUID.randomUUID() + "-" + sanitizedFileName;

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(fileKey)
                .contentType(request.getContentType())
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(EXPIRES_IN_SECONDS))
                .putObjectRequest(putObjectRequest)
                .build();

        URL uploadUrl = s3Presigner.presignPutObject(presignRequest).url();
        String fileUrl = buildPublicUrl(fileKey);

        return PresignedUploadResponse.builder()
                .uploadUrl(uploadUrl.toString())
                .fileUrl(fileUrl)
                .fileKey(fileKey)
                .expiresInSeconds(EXPIRES_IN_SECONDS)
                .build();
    }

    public void deleteFileByKey(String fileKey) {
        if (fileKey == null || fileKey.isBlank()) return;

        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(fileKey)
                .build());
    }

    private void validateRequest(PresignedUploadRequest request) {
        if (request == null) throw new IllegalArgumentException("Request không được null");
        if (request.getFileName() == null || request.getFileName().isBlank())
            throw new IllegalArgumentException("fileName không được để trống");
        if (request.getContentType() == null || request.getContentType().isBlank())
            throw new IllegalArgumentException("contentType không được để trống");
        if (request.getUploadType() == null)
            throw new IllegalArgumentException("uploadType không được để trống");

        validateContentType(request.getUploadType(), request.getContentType());
    }

    private void validateContentType(UploadType uploadType, String contentType) {
        Set<String> imageTypes = Set.of("image/jpeg", "image/png", "image/webp");
        Set<String> mixedTypes = Set.of("image/jpeg", "image/png", "image/webp", "application/pdf", "video/mp4");

        switch (uploadType) {
            case AVATAR -> {
                if (!imageTypes.contains(contentType))
                    throw new IllegalArgumentException("Avatar chỉ hỗ trợ JPG, PNG, WEBP");
            }
            case TEACHING_EVIDENCE, SESSION_ATTACHMENT, REPORT_EVIDENCE -> {
                if (!mixedTypes.contains(contentType))
                    throw new IllegalArgumentException("Loại file không được hỗ trợ");
            }
        }
    }

    private String resolveFolder(UploadType uploadType) {
        return switch (uploadType) {
            case AVATAR -> "avatars";
            case TEACHING_EVIDENCE -> "teaching-evidences";
            case SESSION_ATTACHMENT -> "session-attachments";
            case REPORT_EVIDENCE -> "report-evidences";
        };
    }

    private String sanitizeFileName(String fileName) {
        return fileName.trim()
                .replaceAll("\\s+", "-")
                .replaceAll("[^a-zA-Z0-9._-]", "");
    }

    public String buildPublicUrl(String fileKey) {
        return publicBaseUrl.endsWith("/") ? publicBaseUrl + fileKey : publicBaseUrl + "/" + fileKey;
    }
}