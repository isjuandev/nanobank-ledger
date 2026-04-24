package com.nanobank.ledger.exception;

public class WalletDeletionNotAllowedException extends RuntimeException {
    public WalletDeletionNotAllowedException(String message) {
        super(message);
    }
}
