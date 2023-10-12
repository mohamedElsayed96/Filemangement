package com.gizasystems.filemanagement.exceptions;


import com.gizasystems.filemanagement.enums.ErrorCodes;
import org.springframework.http.HttpStatus;

public class FileAlreadyExistException extends RuntimeBusinessException {

    public FileAlreadyExistException() {
        super(HttpStatus.CONFLICT, ErrorCodes.ERROR_FM_009);
    }


}
