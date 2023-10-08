package com.gizasystems.filemanagement.models;

import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Generic response class
 */

@Log4j2
public class GenericResponse<T> {
    /**
     * response field
     */

    private Mono<ResponseEntity<Response<T>>> responseMono;
    private Flux<ResponseEntity<Response<T>>> responseFlux;

    /**
     * construcroe
     */

    public GenericResponse(final Mono<Response<T>> genericResponse, final HttpStatus statusCode) {
        this.responseMono = genericResponse.map(res-> new ResponseEntity<>(res, statusCode));
    }
    public GenericResponse(final Flux<Response<T>> genericResponse, final HttpStatus statusCode) {
        this.responseFlux = genericResponse.map(res-> new ResponseEntity<>(res, statusCode));
    }

    public Mono<ResponseEntity<Response<T>>> getResponseMono() {
        return responseMono;
    }
    public Flux<ResponseEntity<Response<T>>> getResponseFlux() {
        return responseFlux;
    }




}
