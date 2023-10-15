package com.gizasystems.filemanagement.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gizasystems.filemanagement.exceptions.*;
import com.gizasystems.filemanagement.infrastructure.FileSystemConfig;
import com.gizasystems.filemanagement.models.ResourceCreated;
import com.gizasystems.filemanagement.models.ResourceDeleted;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

/**
 * Author: Mohamed Eid
 * Date: October 1, 2023,
 * Description: Implementation for File System.
 */
@Profile({"filesystem"})
@Service
@RequiredArgsConstructor
@Log4j2
public class FileSystemStorageFileServiceImpl implements IStorageFileService {

    private final FileSystemConfig fileSystemConfig;
    private final ObjectMapper objectMapper;

    @Override
    @SneakyThrows
    public Mono<ResourceCreated> uploadFile(UUID fileId, Map<String, String> fileMetaData, Flux<DataBuffer> partEventFlux) {
        var uploadState = new UploadState(fileId.toString());

        var filename = fileId.toString();
        var metaFilename = fileId + "_meta.txt";


        Path filePath = Paths.get(fileSystemConfig.getStoragePath(), filename);
        Path metafilePath = Paths.get(fileSystemConfig.getMetaStoragePath(), metaFilename);

        var file = new File(filePath.toString());
        var metaFile = new File(metafilePath.toString());


        if (file.exists() || metaFile.exists()) {
            log.error("Upload Failed: Failed due to existing file with the same id {}", fileId);
            throw new FileAlreadyExistException();
        }

        var outputStream = new FileOutputStream(filePath.toString());
        var metaOutputStream = new FileOutputStream(metafilePath.toString());

        return DataBufferUtils.write(partEventFlux, outputStream).flatMap(dataBuffer -> {
                    uploadState.sizeBuffered += dataBuffer.readableByteCount();
                    if (uploadState.sizeBuffered > fileSystemConfig.getMaxFileSize() * Math.pow(1024, 2)) {
                        DataBufferUtils.release(dataBuffer);
                        return Mono.error(new UploadFileExceededMaxAllowedSizeException(fileMetaData));
                    }
                    return Mono.just(new ResourceCreated(fileId.toString()));
                }).reduce((o, o2) -> new ResourceCreated(fileId.toString()))
                .zipWith(Mono.fromCallable(() -> {
                    try {
                        objectMapper.writeValue(metaOutputStream, fileMetaData);
                    } catch (IOException e) {
                        log.error(e.getMessage(), e);
                        throw new UploadFailedException();
                    }
                    return true;
                }).subscribeOn(Schedulers.boundedElastic())).map(Tuple2::getT1)
                .onErrorResume(ex ->
                        Mono.fromCallable(() -> {
                            if (Files.exists(filePath)) {
                                try {
                                    outputStream.close();
                                    Files.delete(filePath);
                                } catch (IOException e) {
                                    log.error(e.getMessage(), e);
                                }
                            }
                            return true;

                        }).subscribeOn(Schedulers.boundedElastic()).zipWith(Mono.fromCallable(() -> {
                            if (Files.exists(metafilePath)) {
                                try {
                                    metaOutputStream.close();
                                    Files.delete(metafilePath);
                                } catch (IOException e) {
                                    log.error(e.getMessage(), e);
                                }
                            }
                            return true;
                        }).subscribeOn(Schedulers.boundedElastic())).flatMap(objects -> Mono.error(ex))

                )
                .doFinally(signalType -> {
                    try {
                        outputStream.close();
                        metaOutputStream.close();
                    } catch (IOException e) {
                        log.error(e.getMessage(), e);
                        throw new UploadFailedException();
                    }
                });

    }


    @Override
    @SneakyThrows
    public Mono<ResponseEntity<Flux<ByteBuffer>>> downloadFile(UUID fileId) {
        Path metaFilePath = Paths.get(fileSystemConfig.getMetaStoragePath(), fileId + "_meta.txt");
        Path filePath = Paths.get(fileSystemConfig.getStoragePath(), fileId.toString());

        Resource resource = new FileSystemResource(filePath.toFile());
        if (resource.exists() || resource.isReadable()) {
            var metaData = objectMapper.readValue(metaFilePath.toFile(), new TypeReference<Map<String, String>>() {
            });
            var fileSize = metaData.get("file_size");
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

    @Override
    public Mono<ResourceDeleted> deleteFile(UUID fileId) {
        var filename = fileId.toString();
        var metaFilename = fileId + "_meta.txt";


        Path filePath = Paths.get(fileSystemConfig.getStoragePath(), filename);
        Path metafilePath = Paths.get(fileSystemConfig.getMetaStoragePath(), metaFilename);

        return Mono.fromCallable(() -> {
                    Files.delete(filePath);
                    return true;
                })
                .subscribeOn(Schedulers.boundedElastic())
                .zipWith(Mono.fromCallable(() -> {
                    Files.delete(metafilePath);
                    return true;
                }).subscribeOn(Schedulers.boundedElastic()))
                .onErrorResume(throwable -> {
                    log.error(throwable.getMessage(), throwable);
                    if (throwable instanceof NoSuchFileException) throw new FileNotFoundException();
                    throw new DeleteFailedException();
                })
                .map(result -> new ResourceDeleted(true));


    }

    static class UploadState {
        final String fileKey;
        long sizeBuffered = 0;

        UploadState(String fileKey) {
            this.fileKey = fileKey;
        }
    }
}
