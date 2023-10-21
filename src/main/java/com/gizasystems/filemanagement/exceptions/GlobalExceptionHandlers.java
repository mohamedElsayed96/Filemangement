package com.gizasystems.filemanagement.exceptions;


import com.gizasystems.filemanagement.enums.ErrorCodes;
import com.gizasystems.filemanagement.models.ErrorResponse;
import com.gizasystems.filemanagement.models.Response;
import com.gizasystems.filemanagement.service.BaseController;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Mono;

import java.util.*;
/**
 * Author: Mohamed Eid
 * Date: October 1, 2023,
 * Description: Global Exception Handler to handle reactive exception.
 */
@RestControllerAdvice
@Log4j2
public class GlobalExceptionHandlers extends BaseController {


    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<Response<Object>>> serverExceptionHandler(Exception ex) {
        log.error(ex.getMessage(), ex);
        if (ex instanceof RuntimeBusinessException exception) {
            var error = new ErrorResponse(exception.getErrorCode(), exception.getErrorMessage(), exception.getErrorData());
            return formatResponse(error, exception.getHttpStatusCode());
        }

        if (ex instanceof MethodArgumentNotValidException exception) {
            List<Map<String, String>> errorList = new ArrayList<>();
            for (ObjectError error : exception.getBindingResult().getAllErrors()) {
                Map<String, String> errorMap = new HashMap<>();
                errorMap.put(((FieldError) error).getField(), error.getDefaultMessage());
                errorList.add(errorMap);
            }
            var errorResponse = prepareBadRequestResponse(errorList);
            return formatResponse(errorResponse, HttpStatus.BAD_REQUEST);
        }
        if (ex instanceof BindException exception) {
            List<Map<String, String>> errorList = new ArrayList<>();
            for (ObjectError error : exception.getBindingResult().getAllErrors()) {
                Map<String, String> errorMap = new HashMap<>();
                errorMap.put(((FieldError) error).getField(), error.getDefaultMessage());
                errorList.add(errorMap);
            }
            var errorResponse = prepareBadRequestResponse(errorList);
            return formatResponse(errorResponse, HttpStatus.BAD_REQUEST);
        }
        if (ex instanceof WebExchangeBindException exception) {
            List<Map<String, String>> errorList = new ArrayList<>();
            for (ObjectError error : exception.getBindingResult().getAllErrors()) {
                Map<String, String> errorMap = new HashMap<>();
                errorMap.put(((FieldError) error).getField(), error.getDefaultMessage());
                errorList.add(errorMap);
            }
            var errorResponse = prepareBadRequestResponse(errorList);
            return formatResponse(errorResponse, HttpStatus.BAD_REQUEST);
        }

        var error = new ErrorResponse(ErrorCodes.ERROR_000.getErrorCode(), ErrorCodes.ERROR_000.getErrorMessage(), null);
        return formatResponse(error, HttpStatus.INTERNAL_SERVER_ERROR);


    }

    ErrorResponse prepareBadRequestResponse(Object errorData) {
        return new ErrorResponse(ErrorCodes.ERROR_001.getErrorCode(), ErrorCodes.ERROR_001.getErrorMessage(), errorData);
    }

}
