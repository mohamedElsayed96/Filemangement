package com.gizasystems.filemanagement.enums;

import lombok.Getter;

@Getter
public enum FileType {
    IMAGE("image/jpeg,image/png"),
    DOCUMENT("application/pdf,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document");

    private final String allowedMimeTypes;
    FileType( String allowedMimeTypes) {

        this.allowedMimeTypes = allowedMimeTypes;
    }

    public boolean checkMimeType(String mimeType){
        return this.allowedMimeTypes.contains(mimeType);
    }

}
