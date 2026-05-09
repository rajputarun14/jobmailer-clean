package com.arun.jobmailer.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class S3Config {

        // provide empty defaults so application starts when AWS properties are not present
        @Value("${aws.accessKeyId:}")
        private String accessKey;

        @Value("${aws.secretAccessKey:}")
        private String secretKey;

        @Value("${aws.region:us-east-1}")
        private String region;

        // only create S3 client when explicitly enabled via property aws.s3.enabled=true
        @Bean
        @ConditionalOnProperty(prefix = "aws.s3", name = "enabled", havingValue = "true")
        public S3Client s3Client() {
                AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);

                return S3Client.builder()
                                .region(Region.of(region))
                                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                                .build();
        }
}
