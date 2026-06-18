package com.ecslab;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.cloudfront.CloudFrontClient;
import software.amazon.awssdk.services.cloudfront.model.CreateInvalidationRequest;
import software.amazon.awssdk.services.cloudfront.model.InvalidationBatch;
import software.amazon.awssdk.services.cloudfront.model.Paths;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

/** Production implementation — uploads/deletes photos in S3, served via CloudFront. */
@Service
@Profile("prod")
public class S3StorageService implements StorageService {

    private final S3Client s3;
    private final CloudFrontClient cloudFront;
    private final String bucket;
    private final String cloudFrontUrl;
    private final String distributionId;

    public S3StorageService(
            @Value("${app.s3.bucket}") String bucket,
            @Value("${app.cloudfront.url}") String cloudFrontUrl,
            @Value("${app.cloudfront.distribution-id}") String distributionId) {
        this.s3 = S3Client.create();
        this.cloudFront = CloudFrontClient.create();
        this.bucket = bucket;
        this.cloudFrontUrl = cloudFrontUrl;
        this.distributionId = distributionId;
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

    @Override
    public void delete(String key) {
        s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());

        cloudFront.createInvalidation(CreateInvalidationRequest.builder()
            .distributionId(distributionId)
            .invalidationBatch(InvalidationBatch.builder()
                .callerReference(UUID.randomUUID().toString())
                .paths(Paths.builder().quantity(1).items("/" + key).build())
                .build())
            .build());
    }

    private String extension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.'));
    }
}
