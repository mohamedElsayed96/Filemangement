package com.gizasystems.filemanagement.infrastructure;

import io.minio.MinioClient;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.*;

import java.net.URI;
import java.time.Duration;

@Configuration
@Getter
@ConfigurationProperties("minio")
@PropertySource("${spring.config.location}/modules-minio.config.properties")
@Profile({"minio"})
@Setter
public class MinioClientConfig {


    private URI endpoint;


    private String accessKey;


    private String secretKey;


    private String bucketName;


    private long multipartMinPartSize ;

    @Bean
    public S3AsyncClient s3AsyncClient(AwsCredentialsProvider credentialsProvider) {

        SdkAsyncHttpClient httpClient = NettyNioAsyncHttpClient.builder()
                .writeTimeout(Duration.ZERO)
                .maxConcurrency(64)
                .build();

        S3Configuration serviceConfiguration = S3Configuration.builder()
                .checksumValidationEnabled(false)
                .chunkedEncodingEnabled(true)
                .build();

        S3AsyncClientBuilder b = S3AsyncClient.builder()
                .httpClient(httpClient)
                .region(Region.ME_CENTRAL_1)
                .credentialsProvider(credentialsProvider)
                .forcePathStyle(true)
                .serviceConfiguration(serviceConfiguration);
        b = b.endpointOverride(endpoint);


        return b.build();
    }
    @Bean
    public S3Client s3client(AwsCredentialsProvider credentialsProvider) {



        S3Configuration serviceConfiguration = S3Configuration.builder()
                .checksumValidationEnabled(false)
                .chunkedEncodingEnabled(true)
                .build();

        S3ClientBuilder b = S3Client.builder()
                .region(Region.ME_CENTRAL_1)
                .credentialsProvider(credentialsProvider)
                .forcePathStyle(true)
                .serviceConfiguration(serviceConfiguration);
        b = b.endpointOverride(endpoint);


        return b.build();
    }

    @Bean
    public AwsCredentialsProvider awsCredentialsProvider() {
        return () -> AwsBasicCredentials.create(accessKey, secretKey);
    }

    @Bean
    @SneakyThrows
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint.toURL())
                .credentials(accessKey, secretKey)
                .build();
    }
}