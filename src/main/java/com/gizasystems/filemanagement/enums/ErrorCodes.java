package com.gizasystems.filemanagement.enums;

import lombok.Getter;

@Getter
public enum ErrorCodes {
    ERROR_000("ERROR_000", "Internal Server Error"),
    ERROR_001("ERROR_001", "Bad Request"),



    ERROR_FM_001("ERROR_FM_001", "File Download Failed"),
    ERROR_FM_002("ERROR_FM_002", "File Upload Failed"),
    ERROR_FM_003("ERROR_FM_003", "Uploaded Data are not a file"),
    ERROR_FM_004("ERROR_FM_004", "Uploaded file exceeded max allowed size"),
    ERROR_FM_005("ERROR_FM_005", "Uploaded file type are not allowed"),
    ERROR_FM_006("ERROR_FM_006", "File not found"),
    ERROR_FM_007("ERROR_FM_007", "Access Denied to this File"),
    ERROR_FM_008("ERROR_FM_008", "Delete File Failed"),
    ERROR_FM_009("ERROR_FM_009", "File Already Exist"),
    ;



    private final String errorCode;
    private final String errorMessage;

    ErrorCodes(String errorCode, String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }
}
