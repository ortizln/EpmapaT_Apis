package com.erp.sri_files.exceptions;

import com.erp.sri_files.dto.response.ApiErrorResponse;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();

        return build(HttpStatus.BAD_REQUEST, "DOC_VALIDATION_ERROR", "La solicitud contiene errores de validacion", details);
    }

    @ExceptionHandler(DocumentoRecepcionException.class)
    public ResponseEntity<ApiErrorResponse> handleDocumentoRecepcion(DocumentoRecepcionException ex) {
        return build(HttpStatus.BAD_REQUEST, "DOC_RECEPCION_ERROR", ex.getMessage(), List.of());
    }

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ApiErrorResponse> handleAuth(AuthException ex) {
        return build(HttpStatus.UNAUTHORIZED, "AUTH_ERROR", ex.getMessage(), List.of());
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleEntityNotFound(EntityNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", ex.getMessage(), List.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String detail = "Parametro invalido: " + ex.getName();
        return build(HttpStatus.BAD_REQUEST, "REQUEST_TYPE_MISMATCH", "Uno o mas parametros tienen un formato invalido", List.of(detail));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return build(HttpStatus.BAD_REQUEST, "REQUEST_ARGUMENT_ERROR", ex.getMessage(), List.of());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalState(IllegalStateException ex) {
        return build(HttpStatus.CONFLICT, "PROCESSING_ERROR", ex.getMessage(), List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Ocurrio un error interno no controlado", List.of(ex.getClass().getSimpleName()));
    }

    private ResponseEntity<ApiErrorResponse> build(HttpStatus status, String code, String message, List<String> details) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(
                LocalDateTime.now().toString(),
                status.value(),
                code,
                message,
                details
        ));
    }
}
