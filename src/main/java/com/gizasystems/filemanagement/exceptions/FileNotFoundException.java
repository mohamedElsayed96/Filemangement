package com.gizasystems.filemanagement.exceptions;


import com.gizasystems.filemanagement.enums.ErrorCodes;
import org.springframework.http.HttpStatus;

public class FileNotFoundException extends RuntimeBusinessException {

    public FileNotFoundException() {
        super(HttpStatus.NOT_FOUND, ErrorCodes.ERROR_FM_006);
    }


}
