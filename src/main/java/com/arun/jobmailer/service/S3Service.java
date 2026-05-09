package com.arun.jobmailer.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.File;

@Service
public class S3Service {

    @Autowired(required = false)
    private S3Client s3Client;

    @Value("${aws.s3.bucket-name:}")
    private String bucketName;

    public void uploadFile(String key, File file) {
        if (s3Client == null) {
            throw new IllegalStateException("S3 is not enabled or S3 client bean is not available");
        }

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
            .bucket(bucketName)
            .key(key)
            .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromFile(file));
    }

    public boolean downloadFile(String key, File dest) {
        if (s3Client == null) return false;
        try {
            software.amazon.awssdk.services.s3.model.GetObjectRequest getObjectRequest =
                    software.amazon.awssdk.services.s3.model.GetObjectRequest.builder()
                            .bucket(bucketName)
                            .key(key)
                            .build();

            // stream to file
            try (java.io.InputStream is = s3Client.getObject(getObjectRequest)) {
                java.nio.file.Files.copy(is, dest.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
