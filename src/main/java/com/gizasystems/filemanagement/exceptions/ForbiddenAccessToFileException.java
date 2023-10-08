package com.gizasystems.filemanagement.exceptions;


import com.gizasystems.filemanagement.enums.ErrorCodes;
import org.springframework.http.HttpStatus;

public class ForbiddenAccessToFileException extends RuntimeBusinessException {

    public ForbiddenAccessToFileException() {
        super(HttpStatus.FORBIDDEN, ErrorCodes.ERROR_FM_007);
    }


}
