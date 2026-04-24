package com.nanobank.ledger.exception;

public class UnauthorizedResourceException extends RuntimeException {
    public UnauthorizedResourceException(String message) {
        super(message);
    }
}
