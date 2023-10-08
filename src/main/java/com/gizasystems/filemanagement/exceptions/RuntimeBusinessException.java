package com.gizasystems.filemanagement.exceptions;


import com.gizasystems.filemanagement.enums.ErrorCodes;
import com.gizasystems.filemanagement.models.ErrorResponse;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

@Getter
@Setter
public class RuntimeBusinessException extends RuntimeException {

    private HttpStatus httpStatusCode;
    private String errorCode;
    private String errorMessage;
    private Object errorData;

    public RuntimeBusinessException(HttpStatus httpStatusCode, ErrorCodes errorCode) {
        super(errorCode.getErrorMessage());
        this.httpStatusCode = httpStatusCode;
        this.errorCode = errorCode.getErrorCode();
        this.errorMessage = errorCode.getErrorMessage();
    }
    public RuntimeBusinessException(HttpStatus httpStatusCode, ErrorCodes errorCode, Object errorData) {
        super(errorCode.getErrorMessage());
        this.httpStatusCode = httpStatusCode;
        this.errorCode = errorCode.getErrorCode();
        this.errorMessage = errorCode.getErrorMessage();
        this.errorData = errorData;
    }

    public RuntimeBusinessException(HttpStatus httpStatusCode, String errorCode, String errorMessage) {
        super(errorMessage);
        this.httpStatusCode = httpStatusCode;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }
    public RuntimeBusinessException(ErrorResponse errorResponse, HttpStatus httpStatusCode) {
        super(errorResponse.errorMessage());
        this.httpStatusCode = httpStatusCode;
        this.errorCode = errorResponse.errorCode();
        this.errorMessage = errorResponse.errorMessage();
        this.errorData = errorResponse.errorData();
    }
}
