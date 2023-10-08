package com.gizasystems.filemanagement.exceptions;

import com.gizasystems.filemanagement.enums.ErrorCodes;
import org.springframework.http.HttpStatus;

public class UploadFailedException extends RuntimeBusinessException {

    public UploadFailedException() {
        super(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCodes.ERROR_FM_002);
    }


}
