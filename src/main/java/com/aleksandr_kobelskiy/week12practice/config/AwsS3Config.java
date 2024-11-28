package com.aleksandr_kobelskiy.week12practice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;

import java.net.URI;

@Configuration
public class AwsS3Config {

    @Value("${aws.s3.accessKey}")
    private String accessKey;

    @Value("${aws.s3.secretKey}")
    private String secretKey;

    @Value("${aws.s3.regionName}")
    private String region;

    @Value("${aws.s3.endpointOverride}")
    private String endpointOverride;

    @Bean
    public S3AsyncClient s3AsyncClient() {

        return S3AsyncClient.builder()
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)
                ))
                .region(Region.of(region))
                .endpointOverride(URI.create(endpointOverride))
                .build();
    }
}
