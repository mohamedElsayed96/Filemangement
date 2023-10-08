package com.gizasystems.filemanagement.controller;

import com.gizasystems.filemanagement.enums.FileType;
import com.gizasystems.filemanagement.models.ResourceCreated;
import com.gizasystems.filemanagement.models.Response;
import com.gizasystems.filemanagement.service.DownloadFileService;
import com.gizasystems.filemanagement.service.UploadFileService;
import com.gizasystems.filemanagement.service.BaseController;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.PartEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.ByteBuffer;
import java.util.UUID;

@Configuration
@RestController
@RequiredArgsConstructor
@RequestMapping("")
@Log4j2
public class FileController extends BaseController {


    private final UploadFileService uploadFileService;
    private final DownloadFileService downloadFileService;


    @PostMapping()
    public Mono<ResponseEntity<Response<ResourceCreated>>> uploadFile(@RequestParam("fileId") UUID fileId,
                                                                      @RequestParam("fileType") FileType fileType,
                                                                      @RequestBody Flux<PartEvent> filePartFlux) {
        return formatResponse(uploadFileService.uploadFile(fileId, fileType, filePartFlux));

    }
    @GetMapping(path = "/{fileId}")
    public Mono<ResponseEntity<Flux<ByteBuffer>>> downloadFile(@PathVariable("fileId") UUID fileId) {

        return downloadFileService.downloadFile(fileId);
    }
}
