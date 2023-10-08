package com.gizasystems.filemanagement.service;


import com.gizasystems.filemanagement.exceptions.DownloadFailedException;
import com.gizasystems.filemanagement.infrastructure.MinioClientConfig;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.*;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
@Service
@Log4j2
public class DownloadFileService {

    private final IStorageFileService storageFileService;


    @SneakyThrows
    public Mono<ResponseEntity<Flux<ByteBuffer>>> downloadFile(UUID fileId) {
        return storageFileService.downloadFile(fileId);
    }

        /**
         * Lookup a metadata key in a case-insensitive way.
         *
         * @param sdkResponse
         * @param key
         * @param defaultValue
         * @return
         */




    }
