//package com.gizasystems.filemanagement.service;
//
//import com.phsystems.mardealer.lib.brokerconnectors.models.DeleteFileEventData;
//import com.phsystems.mardealer.lib.common.annotation.LogFunctionInputOutput;
//import com.phsystems.mardealer.lib.common.enums.UserTypes;
//import com.phsystems.mardealer.lib.common.models.ResourceDeleted;
//import com.phsystems.mardealer.lib.security.models.CustomPrinciple;
//import com.phsystems.mardealer.lib.security.services.SecurityUtil;
//import com.phsystems.mardealer.microservices.filemanagement.exceptions.DeleteFailedException;
//import com.phsystems.mardealer.microservices.filemanagement.exceptions.FileNotFoundException;
//import com.phsystems.mardealer.microservices.filemanagement.exceptions.ForbiddenAccessToFileException;
//import com.phsystems.mardealer.microservices.filemanagement.infrastructure.MinioClientConfig;
//import com.phsystems.mardealer.microservices.filemanagement.repositories.IFileOwnerRepository;
//import com.phsystems.mardealer.microservices.filemanagement.repositories.IFileRepository;
//import io.jsonwebtoken.Claims;
//import lombok.RequiredArgsConstructor;
//import lombok.SneakyThrows;
//import lombok.extern.log4j.Log4j2;
//import org.springframework.stereotype.Service;
//import reactor.core.publisher.Mono;
//import software.amazon.awssdk.http.SdkHttpResponse;
//import software.amazon.awssdk.services.s3.S3AsyncClient;
//import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
//import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
//
//@RequiredArgsConstructor
//@Service
//@Log4j2
//public class DeleteFileService {
//
//    private final S3AsyncClient s3client;
//    private final MinioClientConfig minioClientConfig;
//
//
//    @LogFunctionInputOutput
//    public Mono<ResourceDeleted> requestFileDeletion(DeleteFileEventData deleteFileEventData, CustomPrinciple principle) {
//        var userId = SecurityUtil.getClaimFromPrinciple(Claims.SUBJECT, String.class, null, principle);
//        var fileId = deleteFileEventData.getFileId();
//        return fileRepository.findById(fileId)
//                .switchIfEmpty(Mono.error(new FileNotFoundException()))
//                .flatMap(fileEntity -> {
//                    if (!fileEntity.getUploadedBy().equals(userId)) {
//                        return Mono.error(new ForbiddenAccessToFileException());
//                    }
//                    return checkOwnerShip(userId, fileId).flatMap(result ->
//                    {
//                        if (Boolean.FALSE.equals(result)) {
//                            return Mono.error(new ForbiddenAccessToFileException());
//                        }
//                        return fileRepository.markFileAsDeleted(true, fileId)
//                                .map(integer -> new ResourceDeleted(true));
//
//                    });
//                });
//
//    }
//
//
//    @SneakyThrows
//    public Mono<ResourceDeleted> deleteFile(String fileId, String userId, UserTypes userTypes) {
//
//        DeleteObjectRequest request = DeleteObjectRequest.builder()
//                .bucket(minioClientConfig.getBucketName())
//                .key(fileId)
//                .build();
//
//        return fileRepository.findById(fileId)
//                .switchIfEmpty(Mono.error(new FileNotFoundException()))
//                .flatMap(fileEntity ->
//                        Mono.fromFuture(s3client.deleteObject(request))
//                                .map(response -> {
//                                    checkResult(response, fileId);
//                                    return new ResourceDeleted();
//
//                                }));
//    }
//
//
//    // Helper used to check return codes from an API call
//    private static void checkResult(DeleteObjectResponse response, String fileId) {
//        SdkHttpResponse sdkResponse = response.sdkHttpResponse();
//        if (sdkResponse != null && sdkResponse.isSuccessful()) {
//            return;
//        }
//        if (sdkResponse != null) {
//            log.error("file_id---> {} Failed To be Deleted due to {} with status {}", fileId, sdkResponse.statusText().orElse("Unknown Error"), sdkResponse.statusCode());
//        } else {
//            log.error("file_id---> {} Failed To be Deleted due to Unknown Error", fileId);
//
//        }
//        throw new DeleteFailedException();
//    }
//
//    private Mono<Boolean> checkOwnerShip(String userId, String fileId) {
//
//        return  fileOwnerRepository.existsByFileOwnerEntityCompositeKey_UserIdAndAndFileOwnerEntityCompositeKey_FileId(userId, fileId);
//
//    }
//
//
//}
