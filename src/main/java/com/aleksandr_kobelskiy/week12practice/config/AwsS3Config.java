package com.aleksandr_kobelskiy.week12practice.config;

import com.fasterxml.jackson.core.ErrorReportConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;

import java.net.URI;
import java.time.Duration;

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

    @Bean
    @Profile("test")
    public S3AsyncClient localStackS3AsyncClient() {

        return S3AsyncClient.builder()
                .httpClientBuilder(NettyNioAsyncHttpClient.builder()
//                        .maxConcurrency(100) // Увеличьте лимит одновременных соединений
//                        .connectionAcquisitionTimeout(Duration.ofSeconds(30)) // Таймаут подключения
//                        .connectionTimeout(Duration.ofSeconds(30)) // Таймаут соединения
//                        .readTimeout(Duration.ofSeconds(60)) // Таймаут чтения данных

                        .maxConcurrency(100) // Увеличьте лимит одновременных соединений
                        .connectionAcquisitionTimeout(Duration.ofSeconds(120)) // Таймаут подключения
                        .connectionTimeout(Duration.ofSeconds(120)) // Таймаут соединения
                        .readTimeout(Duration.ofSeconds(120)) // Таймаут чтения данных
                )
                .endpointOverride(URI.create("http://localhost:4566")) // Используем LocalStack
                .region(Region.US_EAST_1) // Регион, указанный в application-test.properties
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("accesskey", "secretkey")
                ))
                .build();
    }
}
