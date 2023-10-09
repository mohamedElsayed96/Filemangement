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
import org.springframework.core.io.buffer.DataBufferLimitException;
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
import reactor.util.function.Tuple2;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
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
    @SneakyThrows
    public Mono<ResourceCreated> uploadFile(UUID fileId, Map<String, String> fileMetaData, Flux<DataBuffer> partEventFlux) {
        var uploadState = new UploadState(fileId.toString());
        var filename = fileId.toString();
        var metaFilename = fileId + "_meta.txt";
        var backUpFilename = fileId + ".old";
        var backUpMetaFileName = fileId + ".old";

        Path filePath = Paths.get(fileSystemConfig.getStoragePath(), filename);
        Path metafilePath = Paths.get(fileSystemConfig.getStoragePath(), metaFilename);

        var oldFile = filePath.toFile();
        var oldMetaFile = metafilePath.toFile();

        if (oldFile.exists()) {
            File newFile = new File(oldFile.getParent(), backUpFilename);
            oldFile.renameTo(newFile);
        }
        if (oldMetaFile.exists()) {
            File newFile = new File(oldMetaFile.getParent(), backUpMetaFileName);
            oldMetaFile.renameTo(newFile);
        }

        var outputStream = new FileOutputStream(filePath.toString());
        return DataBufferUtils.write(partEventFlux, outputStream).flatMap(dataBuffer -> {
                    uploadState.sizeBuffered += dataBuffer.readableByteCount();
                    if (uploadState.sizeBuffered > fileSystemConfig.getMaxFileSize()) {
                        DataBufferUtils.release(dataBuffer);
                        return Mono.error(new UploadFileExceededMaxAllowedSizeException(fileMetaData));
                    }
                    return Mono.just(new ResourceCreated(fileId.toString()));
                }).reduce((o, o2) -> new ResourceCreated(fileId.toString()))
                .zipWith(Mono.fromCallable(() -> {
                    try {
                        metafilePath.toFile().createNewFile();
                        objectMapper.writeValue(metafilePath.toFile(), fileMetaData);

                    } catch (IOException e) {
                        return Mono.error(e);
                    }
                    return true;
                })).map(Tuple2::getT1).doOnSuccess(resourceCreated -> {
                    File backupFile = new File(oldFile.getParent(), backUpFilename);
                    File backupMetaFile = new File(oldMetaFile.getParent(), backUpMetaFileName);
                    if (backupFile.exists()) backupFile.delete();
                    if (backupMetaFile.exists()) backupMetaFile.delete();
                })
                .onErrorResume(ex -> {

                    if (ex instanceof DataBufferLimitException) {
                        return Mono.error(new UploadFileExceededMaxAllowedSizeException(fileMetaData));
                    }
                    return Mono.error(ex);

                })
                .publishOn(Schedulers.boundedElastic()).doFinally(signalType -> {
                    try {
                        outputStream.close();
                    } catch (IOException e) {
                        log.error(e.getMessage(), e);
                        throw new RuntimeException(e);
                    }
                });

    }




    @Override
    @SneakyThrows
    public Mono<ResponseEntity<Flux<ByteBuffer>>> downloadFile(UUID fileId) {
        Path metaFilePath = Paths.get(fileSystemConfig.getStoragePath(), fileId + "_meta.txt");
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

    static class UploadState {
        final String fileKey;
        long sizeBuffered = 0;

        UploadState(String fileKey) {
            this.fileKey = fileKey;
        }
    }
}
