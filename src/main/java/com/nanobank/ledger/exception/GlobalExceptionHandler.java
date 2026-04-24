package com.nanobank.ledger.exception;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler({
            EntityNotFoundException.class,
            WalletNotFoundException.class,
            TransactionNotFoundException.class
    })
    public ResponseEntity<ErrorResponse> handleNotFound(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(
                        "NO_ENCONTRADO",
                        ex.getMessage(),
                        LocalDateTime.now(),
                        null
                ));
    }

    @ExceptionHandler(UnauthorizedResourceException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(UnauthorizedResourceException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(
                        "PROHIBIDO",
                        ex.getMessage(),
                        LocalDateTime.now(),
                        null
                ));
    }

    @ExceptionHandler(WalletDeletionNotAllowedException.class)
    public ResponseEntity<ErrorResponse> handleWalletDeletionNotAllowed(WalletDeletionNotAllowedException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(
                        "BILLETERA_CON_TRANSACCIONES",
                        ex.getMessage(),
                        LocalDateTime.now(),
                        null
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        "ERROR_VALIDACION",
                        "La validación falló",
                        LocalDateTime.now(),
                        fieldErrors
                ));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableMessage(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        "CUERPO_SOLICITUD_INVALIDO",
                        "El cuerpo de la solicitud está mal formado o no se puede leer",
                        LocalDateTime.now(),
                        null
                ));
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientBalance(InsufficientBalanceException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ErrorResponse(
                        "SALDO_INSUFICIENTE",
                        ex.getMessage(),
                        LocalDateTime.now(),
                        null
                ));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(new ErrorResponse(
                        "METODO_NO_PERMITIDO",
                        ex.getMessage(),
                        LocalDateTime.now(),
                        null
                ));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException ex) {
        String reason = ex.getReason() != null ? ex.getReason() : "Error de solicitud";
        return ResponseEntity.status(ex.getStatusCode())
                .body(new ErrorResponse(
                        "ERROR_HTTP",
                        reason,
                        LocalDateTime.now(),
                        null
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Excepción no controlada capturada por GlobalExceptionHandler", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(
                        "ERROR_INTERNO_SERVIDOR",
                        "Ocurrió un error inesperado",
                        LocalDateTime.now(),
                        null
                ));
    }
}
