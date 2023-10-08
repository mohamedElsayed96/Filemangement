package com.gizasystems.filemanagement.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gizasystems.filemanagement.exceptions.FileNotFoundException;
import com.gizasystems.filemanagement.exceptions.UploadFileExceededMaxAllowedSizeException;
import com.gizasystems.filemanagement.infrastructure.FileSystemConfig;
import com.gizasystems.filemanagement.models.ResourceCreated;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import org.springframework.http.codec.multipart.FilePart;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

@Profile({"filesystem"})
@Service
@RequiredArgsConstructor
@Log4j2
public class FileSystemStorageFileServiceImpl implements IStorageFileService {

    private final FileSystemConfig fileSystemConfig;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<ResourceCreated> uploadFile(UUID fileId, Map<String, String> fileMetaData, Flux<DataBuffer> partEventFlux) {
        var uploadState = new UploadState(fileId.toString());
        return partEventFlux
                .reduce((dataBuffer, dataBuffer2) -> {
                    uploadState.sizeBuffered += dataBuffer.readableByteCount();
                    if (uploadState.sizeBuffered > fileSystemConfig.getMaxFileSize()) {
                        DataBufferUtils.release(dataBuffer);
                        throw new UploadFileExceededMaxAllowedSizeException(fileMetaData);
                    }
                    return dataBuffer.write(dataBuffer2);
                })
                .flatMap(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];

                    if (bytes.length > fileSystemConfig.getMaxFileSize()) {
                        DataBufferUtils.release(dataBuffer);
                        return Mono.error(new UploadFileExceededMaxAllowedSizeException(fileMetaData));
                    }
                    fileMetaData.put("file_size", uploadState.sizeBuffered + "");
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    try {
                        Path filePath = Paths.get(fileSystemConfig.getStoragePath(), fileId.toString());
                        Path metafilePath = Paths.get(fileSystemConfig.getStoragePath(), fileId + "_meta.txt");
                        objectMapper.writeValue(metafilePath.toFile(), metafilePath);
                        Files.write(filePath, bytes);
                    } catch (IOException e) {
                        log.error(e.getMessage(), e);
                        return Mono.error(e);
                    }
                    return Mono.just(new ResourceCreated(fileId.toString()));
                });

    }

    static class UploadState {
        final String fileKey;
        long sizeBuffered = 0;

        UploadState(String fileKey) {
            this.fileKey = fileKey;
        }
    }

    @Override
    @SneakyThrows
    public Mono<ResponseEntity<Flux<ByteBuffer>>> downloadFile(UUID fileId) {
        Path metaFilePath = Paths.get(fileSystemConfig.getStoragePath(), fileId + "_meta.txt");
        Path filePath = Paths.get(fileSystemConfig.getStoragePath(), fileId.toString());
        var metaData = objectMapper.readValue(metaFilePath.toFile(), new TypeReference<Map<String, String>>() {
        });
        Resource resource = new FileSystemResource(filePath.toFile());
        var fileSize = metaData.get("file_size");
        if (resource.exists() || resource.isReadable()) {
             var responseBody = DataBufferUtils.read(resource, new DefaultDataBufferFactory(), 1024).map(dataBuffer -> {

                ByteBuffer partData = ByteBuffer.allocate(1024);
                dataBuffer.toByteBuffer(partData);
                DataBufferUtils.release(dataBuffer);
                return partData;
            });
            return Mono.just(ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, metaData.get("mime_type"))
                    .header(HttpHeaders.CONTENT_LENGTH, fileSize)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + metaData.get("filename") + "\"")
                    .body(responseBody));
        }
        throw new FileNotFoundException();
    }
}
