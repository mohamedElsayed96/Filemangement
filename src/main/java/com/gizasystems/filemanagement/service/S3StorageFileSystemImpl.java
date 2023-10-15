package com.gizasystems.filemanagement.service;

import com.gizasystems.filemanagement.exceptions.*;
import com.gizasystems.filemanagement.infrastructure.MinioClientConfig;
import com.gizasystems.filemanagement.models.ResourceCreated;
import com.gizasystems.filemanagement.models.ResourceDeleted;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.*;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Author: Mohamed Eid
 * Date: October 1, 2023,
 * Description: Implementation for S3 server.
 */
@Log4j2
@Service
@RequiredArgsConstructor
@Profile({"minio"})
public class S3StorageFileSystemImpl implements IStorageFileService {
    private final S3AsyncClient s3Client;
    private final MinioClientConfig minioClientConfig;

    private static ByteBuffer concatBuffers(List<DataBuffer> buffers) {
        log.info("[I198] creating BytBuffer from {} chunks", buffers.size());

        int partSize = 0;
        for (org.springframework.core.io.buffer.DataBuffer b : buffers) {
            partSize += b.readableByteCount();
        }

        ByteBuffer partData = ByteBuffer.allocate(partSize);
        buffers.forEach(buffer ->
                partData.put(buffer.toByteBuffer()));

        // Reset read pointer to first byte
        partData.rewind();

        log.info("[I208] partData: size={}", partData.capacity());
        return partData;

    }

    // Helper used to check return codes from an API call
    private static void checkResult(GetObjectResponse response, String fileId) {
        SdkHttpResponse sdkResponse = response.sdkHttpResponse();
        if (sdkResponse != null && sdkResponse.isSuccessful()) {
            return;
        }
        if (sdkResponse != null) {
            log.error("file_id---> {} Failed To be Download due to {} with status {}", fileId, sdkResponse.statusText().orElse("Unknown Error"), sdkResponse.statusCode());
        } else {
            log.error("file_id---> {} Failed To be Download due to Unknown Error", fileId);

        }
        throw new DownloadFailedException();
    }

    public Mono<ResourceCreated> uploadFile(UUID fileId, Map<String, String> fileMetaData, Flux<DataBuffer> partEventFlux) {
        var fileKey = fileId.toString();
        log.info("inside filePart");
        var uploadState = new UploadState(minioClientConfig.getBucketName(), fileKey);
        var mimeType = fileMetaData.get("mime_type");
        HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                .bucket(minioClientConfig.getBucketName())
                .key(fileKey)
                .build();


        CompletableFuture<CreateMultipartUploadResponse> uploadRequest = s3Client
                .createMultipartUpload(CreateMultipartUploadRequest.builder()
                        .contentType(mimeType)
                        .key(fileKey)
                        .metadata(fileMetaData)
                        .bucket(minioClientConfig.getBucketName())
                        .build());
        return Mono
                .fromFuture(s3Client.headObject(headObjectRequest))
                .flatMap(headObjectResponse -> {
                   return Mono.error(new FileAlreadyExistException());
                })
                .onErrorResume(throwable -> {
                     if(throwable instanceof FileAlreadyExistException ex) throw ex;
                    return Mono.empty();
                })
                .then(Mono.fromFuture(uploadRequest)
                        .flatMapMany(response -> {
                            checkResult(response, uploadState.fileKey);
                            uploadState.uploadId = response.uploadId();
                            return partEventFlux;
                        })
                        .bufferUntil(buffer -> {
                            uploadState.partBuffered += buffer.readableByteCount();
                            uploadState.sizeBuffered += buffer.readableByteCount();
                            if (uploadState.partBuffered >= minioClientConfig.getMultipartMinPartSize()) {
                                log.info("[I173] bufferUntil: returning true, sizeBuffered= {}, bufferedBytes={}, partCounter={}, uploadId={}", uploadState.sizeBuffered, uploadState.partBuffered, uploadState.partCounter, uploadState.uploadId);
                                uploadState.partBuffered = 0;
                                return true;
                            } else {
                                return false;
                            }
                        })
                        .flatMap(dataBuffers -> {
                            if (uploadState.sizeBuffered > minioClientConfig.getMaxFileSize()) {
                                return Mono.when(abortMultipartUpload(uploadState)).then(Mono.error(new UploadFileExceededMaxAllowedSizeException(fileMetaData)));
                            }
                            return Mono.just(dataBuffers);
                        })
                        .map(S3StorageFileSystemImpl::concatBuffers)
                        .flatMap(buffer -> uploadPart(uploadState, buffer))
                        .reduce(uploadState, (state, completedPart) -> {
                            state.completedParts.put(completedPart.partNumber(), completedPart);
                            return state;
                        })
                        .flatMap(this::completeUpload)
                        .map(response -> {
                            checkResult(response, uploadState.fileKey);
                            return uploadState.fileKey;
                        }).flatMap(s -> Mono.just(new ResourceCreated(s))));


    }

    /**
     * Upload a single file part to the requested bucket
     *
     * @param uploadState
     * @param buffer
     * @return
     */
    private Mono<CompletedPart> uploadPart(UploadState uploadState, ByteBuffer buffer) {
        final int partNumber = ++uploadState.partCounter;
        log.info("[I218] uploadPart: partNumber={}, contentLength={}", partNumber, buffer.capacity());

        CompletableFuture<UploadPartResponse> request = s3Client.uploadPart(UploadPartRequest.builder()
                        .bucket(uploadState.bucket)
                        .key(uploadState.fileKey)
                        .partNumber(partNumber)
                        .uploadId(uploadState.uploadId)
                        .contentLength((long) buffer.capacity())
                        .build(),
                AsyncRequestBody.fromPublisher(Mono.just(buffer)));

        return Mono
                .fromFuture(request)
                .map(uploadPartResult -> {
                    checkResult(uploadPartResult, uploadState.fileKey);
                    log.info("[I230] uploadPart complete: part={}, etag={}", partNumber, uploadPartResult.eTag());
                    return CompletedPart.builder()
                            .eTag(uploadPartResult.eTag())
                            .partNumber(partNumber)
                            .build();
                });
    }

    private Mono<CompleteMultipartUploadResponse> completeUpload(UploadState state) {
        log.info("[I202] completeUpload: bucket={}, fileKey={}, completedParts.size={}", state.bucket, state.fileKey, state.completedParts.size());

        CompletedMultipartUpload multipartUpload = CompletedMultipartUpload.builder()
                .parts(state.completedParts.values())
                .build();

        return Mono.fromFuture(s3Client.completeMultipartUpload(CompleteMultipartUploadRequest.builder()
                .bucket(state.bucket)
                .uploadId(state.uploadId)
                .multipartUpload(multipartUpload)
                .key(state.fileKey)
                .build()));
    }

    private Mono<AbortMultipartUploadResponse> abortMultipartUpload(UploadState state) {
        log.info("[I202] abortMultipartUpload: bucket={}, fileKey={}, completedParts.size={}", state.bucket, state.fileKey, state.completedParts.size());

        return Mono.fromFuture(s3Client.abortMultipartUpload(AbortMultipartUploadRequest.builder()
                .bucket(state.bucket)
                .uploadId(state.uploadId)
                .key(state.fileKey)
                .build()));
    }

    /**
     * check result from an API call.
     *
     * @param result Result from an API call
     */
    private void checkResult(SdkResponse result, String fileId) {
        SdkHttpResponse sdkResponse = result.sdkHttpResponse();

        if (sdkResponse != null && sdkResponse.isSuccessful()) {
            return;
        }

        if (sdkResponse != null) {
            log.error("file_id---> {} Failed To be Upload due to {} with status {}", fileId, sdkResponse.statusText().orElse("Unknown Error"), sdkResponse.statusCode());
        } else {
            log.error("file_id---> {} Failed To be Upload due to Unknown Error", fileId);

        }
        throw new UploadFailedException();


    }

    private static void checkResult(DeleteObjectResponse response, String fileId) {
        SdkHttpResponse sdkResponse = response.sdkHttpResponse();
        if (sdkResponse != null && sdkResponse.isSuccessful()) {
            return;
        }
        if (sdkResponse != null) {
            log.error("file_id---> {} Failed To be Deleted due to {} with status {}", fileId, sdkResponse.statusText().orElse("Unknown Error"), sdkResponse.statusCode());
        } else {
            log.error("file_id---> {} Failed To be Deleted due to Unknown Error", fileId);

        }
        throw new DeleteFailedException();
    }

    @Override
    public Mono<ResponseEntity<Flux<ByteBuffer>>> downloadFile(UUID fileId) {

        var fileKey = fileId.toString();
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(minioClientConfig.getBucketName())
                .key(fileKey)
                .build();


        return Mono.fromFuture(s3Client.getObject(request, AsyncResponseTransformer.toPublisher()))
                .map(response -> {
                    checkResult(response.response(), fileKey);
                    String filename = getMetadataItem(response.response(), "filename", fileKey);

                    log.info("[I65] filename={}, length={}", filename, response.response()
                            .contentLength());

                    return ResponseEntity.ok()
                            .header(HttpHeaders.CONTENT_TYPE, response.response()
                                    .contentType())
                            .header(HttpHeaders.CONTENT_LENGTH, Long.toString(response.response()
                                    .contentLength()))
                            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                            .body(Flux.from(response));

                }).doOnError(throwable -> {
                    log.error(throwable.getMessage(), throwable);
                    if (throwable instanceof NoSuchKeyException) {
                        throw new FileNotFoundException();
                    }
                    throw new DownloadFailedException();
                });
    }

    @SneakyThrows
    public Mono<ResourceDeleted> deleteFile(UUID fileId) {

        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(minioClientConfig.getBucketName())
                .key(fileId.toString())
                .build();

        return
                Mono.fromFuture(s3Client.deleteObject(request))
                        .map(response -> {
                            checkResult(response, fileId.toString());
                            return new ResourceDeleted(true);
                        });
    }


    // Helper used to check return codes from an API call


    private String getMetadataItem(GetObjectResponse sdkResponse, String key, String defaultValue) {
        for (Map.Entry<String, String> entry : sdkResponse.metadata()
                .entrySet()) {
            if (entry.getKey()
                    .equalsIgnoreCase(key)) {
                return entry.getValue();
            }
        }
        return defaultValue;
    }

    static class UploadState {
        final String bucket;
        final String fileKey;

        String uploadId;
        int partCounter;
        Map<Integer, CompletedPart> completedParts = new HashMap<>();
        int partBuffered = 0;
        long sizeBuffered = 0;

        UploadState(String bucket, String fileKey) {
            this.bucket = bucket;
            this.fileKey = fileKey;
        }
    }
}
