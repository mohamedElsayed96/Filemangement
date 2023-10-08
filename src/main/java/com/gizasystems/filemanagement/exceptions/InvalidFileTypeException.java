package com.gizasystems.filemanagement.exceptions;


import com.gizasystems.filemanagement.enums.ErrorCodes;
import org.springframework.http.HttpStatus;

public class InvalidFileTypeException extends RuntimeBusinessException {

    public InvalidFileTypeException(Object errorData) {
        super(HttpStatus.BAD_REQUEST, ErrorCodes.ERROR_FM_005);
    }


}
