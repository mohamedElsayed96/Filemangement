package com.gizasystems.filemanagement.service;

import com.gizasystems.filemanagement.models.ResourceCreated;
import com.gizasystems.filemanagement.models.ResourceDeleted;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.FileNotFoundException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.UUID;

public interface IStorageFileService {

    Mono<ResourceCreated> uploadFile(UUID fileId, Map<String, String> fileMetaData, Flux<DataBuffer> partEventFlux);
    Mono<ResponseEntity<Flux<ByteBuffer>>> downloadFile(UUID fileId);
    Mono<ResourceDeleted> deleteFile(UUID fileId);


}
