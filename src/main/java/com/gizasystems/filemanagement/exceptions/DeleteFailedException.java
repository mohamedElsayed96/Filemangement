package com.gizasystems.filemanagement.exceptions;


import com.gizasystems.filemanagement.enums.ErrorCodes;
import org.springframework.http.HttpStatus;

public class DeleteFailedException extends RuntimeBusinessException {

    public DeleteFailedException() {
        super(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCodes.ERROR_FM_001);
    }


}
