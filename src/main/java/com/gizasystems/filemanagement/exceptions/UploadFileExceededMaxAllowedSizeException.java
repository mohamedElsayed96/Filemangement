package com.gizasystems.filemanagement.exceptions;


import com.gizasystems.filemanagement.enums.ErrorCodes;
import org.springframework.http.HttpStatus;

public class UploadFileExceededMaxAllowedSizeException extends RuntimeBusinessException {

    public UploadFileExceededMaxAllowedSizeException(Object data) {
        super(HttpStatus.BAD_REQUEST, ErrorCodes.ERROR_FM_004, data);
    }


}
