package com.gizasystems.filemanagement.exceptions;


import com.gizasystems.filemanagement.enums.ErrorCodes;
import org.springframework.http.HttpStatus;

public class UploadDataNotFileException extends RuntimeBusinessException {

    public UploadDataNotFileException() {
        super(HttpStatus.BAD_REQUEST, ErrorCodes.ERROR_FM_002);
    }


}
