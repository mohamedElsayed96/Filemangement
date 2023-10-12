package com.gizasystems.filemanagement.service;


import com.gizasystems.filemanagement.enums.FileType;
import com.gizasystems.filemanagement.exceptions.InvalidFileTypeException;
import com.gizasystems.filemanagement.exceptions.UploadDataNotFileException;
import com.gizasystems.filemanagement.models.ResourceCreated;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePartEvent;
import org.springframework.http.codec.multipart.PartEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.UUID;

/**
 * Author: Mohamed Eid
 * Date: October 1, 2023,
 * Description: Handle upload file events.
 */
@RequiredArgsConstructor
@Service
@Log4j2
public class UploadFileService {


    private final IStorageFileService storageFileService;

    public Mono<ResourceCreated> uploadFile(UUID fileId,
                                            FileType fileType,
                                            Flux<PartEvent> files) {


        return files.windowUntil(PartEvent::isLast).concatMap(p -> p.switchOnFirst((signal, partEvents) -> {
            if (!signal.hasValue()) {
                return Mono.just(new ResourceCreated(false));
            }
            PartEvent event = signal.get();
            if (!(event instanceof FilePartEvent fileEvent)) {
                 throw new UploadDataNotFileException();
            }
            var metadata = new HashMap<String, String>();
            String filename = fileEvent.filename();
            metadata.put("filename", filename);
            var mt = fileEvent.headers().getContentType();
            if (mt == null) mt = MediaType.APPLICATION_OCTET_STREAM;
            metadata.put("mime_type", mt.toString());
            metadata.put("extension", getFileExtension(filename));

            var validMimeType = fileType.checkMimeType(mt.toString());
            if (!validMimeType) {
                 throw new InvalidFileTypeException(metadata);
            }

            log.info("upload file name:{}", filename);
            Flux<DataBuffer> contents = partEvents.map(PartEvent::content);

            return storageFileService.uploadFile(fileId, metadata, contents);
        })).single();

    }

    private  String getFileExtension(String fileName) {
        Path path = Paths.get(fileName);
        return path.getFileName().toString().contains(".") ?
                path.getFileName().toString().substring(path.getFileName().toString().lastIndexOf(".") + 1) : "";
    }
}
