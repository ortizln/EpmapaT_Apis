package com.erp.sri_files.exceptions;

public class FacturaElectronicaException extends RuntimeException {

    public FacturaElectronicaException() {
        super();
    }

    public FacturaElectronicaException(String message) {
        super(message);
    }

    public FacturaElectronicaException(String message, Throwable cause) {
        super(message, cause);
    }

    public FacturaElectronicaException(Throwable cause) {
        super(cause);
    }
}
