package com.gizasystems.filemanagement.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import reactor.core.publisher.Mono;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Response<T> {
    private ErrorResponse errorResponse;
    private T result;
    private Pagination pagination;

    public  static <T> Mono<Response<T>> of(Mono<T> result, ErrorResponse errorResponse, Pagination pagination ){
        return result == null? Mono.just(new Response<>(errorResponse, null, pagination)) : result.map(res-> new Response<>(errorResponse, res, pagination));
    }


}
