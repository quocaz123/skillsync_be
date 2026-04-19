package com.skillsync.skillsync.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
public class CloudflareR2Config {

    @Value("${cloudflare.r2.access-key}")
    private String accessKey;

    @Value("${cloudflare.r2.secret-key}")
    private String secretKey;

    @Value("${cloudflare.r2.region}")
    private String region;

    @Value("${cloudflare.r2.endpoint:}")
    private String endpointOverride;

    @Bean
    public S3Configuration s3Configuration() {
        return S3Configuration.builder()
                .pathStyleAccessEnabled(true)
                .build();
    }

    @Bean
    public S3Presigner s3Presigner(S3Configuration s3Configuration) {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);

        S3Presigner.Builder builder = S3Presigner.builder()
                .region(Region.of(region))
                .serviceConfiguration(s3Configuration)
                .credentialsProvider(StaticCredentialsProvider.create(credentials));

        // Bắt buộc với R2: không set sẽ presign cho host mặc định AWS S3 → upload sai/không chạy
        if (endpointOverride != null && !endpointOverride.isBlank()) {
            builder.endpointOverride(URI.create(endpointOverride));
        }

        return builder.build();
    }

    @Bean
    public S3Client s3Client(S3Configuration s3Configuration) {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);

        var builder = S3Client.builder()
                .region(Region.of(region))
                .serviceConfiguration(s3Configuration)
                .credentialsProvider(StaticCredentialsProvider.create(credentials));

        if (endpointOverride != null && !endpointOverride.isBlank()) {
            builder.endpointOverride(URI.create(endpointOverride));
        }

        return builder.build();
    }
}
