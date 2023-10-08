package com.gizasystems.filemanagement.models;

public record ErrorResponse (String errorCode, String errorMessage, Object errorData) {
}
