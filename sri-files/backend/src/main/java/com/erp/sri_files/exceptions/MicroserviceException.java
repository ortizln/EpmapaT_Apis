package com.erp.sri_files.exceptions;

public class MicroserviceException extends RuntimeException {
    public MicroserviceException(String message, Throwable cause) {
        super(message, cause);
    }
}