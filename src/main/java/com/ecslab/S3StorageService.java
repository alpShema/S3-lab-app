package com.ecslab;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

/** Production implementation — uploads photos to S3, serves via CloudFront. */
@Service
@Profile("prod")
public class S3StorageService implements StorageService {

    private final S3Client s3;
    private final String bucket;
    private final String cloudFrontUrl;

    public S3StorageService(
            @Value("${app.s3.bucket}") String bucket,
            @Value("${app.cloudfront.url}") String cloudFrontUrl) {
        // SDK auto-detects region and IAM role credentials from the ECS task metadata
        this.s3 = S3Client.create();
        this.bucket = bucket;
        this.cloudFrontUrl = cloudFrontUrl;
    }

    @Override
    public String upload(MultipartFile file) throws IOException {
        String key = "photos/" + UUID.randomUUID() + extension(file.getOriginalFilename());
        s3.putObject(
            PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(file.getContentType())
                .build(),
            RequestBody.fromBytes(file.getBytes())
        );
        return key;
    }

    @Override
    public String getUrl(String key) {
        return cloudFrontUrl + "/" + key;
    }

    private String extension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.'));
    }
}
