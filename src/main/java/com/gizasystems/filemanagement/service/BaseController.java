package com.gizasystems.filemanagement.service;


import com.gizasystems.filemanagement.models.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
/**
 * Author: Mohamed Eid
 * Date: October 1, 2023,
 * Description: Base Controller to format reactive response.
 */
public abstract class BaseController {


    protected <T extends Flux<U>, U> Mono<ResponseEntity<Response<ResponseListWrapper<U>>>> formatResponse(T result) {
        var res = result == null ? Mono.just(new ResponseListWrapper<U>()) : result.reduce(new ResponseListWrapper<U>(new ArrayList<>()), ((uResponseListWrapper, u) -> {
            uResponseListWrapper.getValues().add(u);
            return uResponseListWrapper;
        }));
        return formatResponse(res, (Pagination) null);
    }

    protected <T extends Flux<U>, U> Mono<ResponseEntity<Response<ResponseListWrapper<U>>>> formatResponse(T result, Pagination paginationData) {
        var res = result == null ? Mono.just(new ResponseListWrapper<U>()) : result.reduce(new ResponseListWrapper<U>(new ArrayList<>()), ((uResponseListWrapper, u) -> {
            uResponseListWrapper.getValues().add(u);
            return uResponseListWrapper;
        }));
        return formatResponse(res, paginationData);
    }

    protected <T> Mono<ResponseEntity<Response<T>>> formatResponse(Mono<T> result) {
        return formatResponse(result, (Pagination) null);
    }

    protected <T> Mono<ResponseEntity<Response<T>>> formatResponse(Mono<T> result, Pagination paginationData) {
        return formatResponse(result, null, paginationData, HttpStatus.OK);
    }

    protected <T> Mono<ResponseEntity<Response<T>>> formatResponse(ErrorResponse errorResponse, HttpStatus httpStatus) {
        return formatResponse(null, errorResponse, null, httpStatus);
    }

    protected <T> Mono<ResponseEntity<Response<T>>> formatResponse(Mono<T> result, ErrorResponse errorResponse, Pagination paginationData, HttpStatus httpStatus) {
        var resultResponse = Response.of(result, errorResponse, paginationData);
        return new GenericResponse<>(resultResponse, httpStatus).getResponseMono();
    }


}
