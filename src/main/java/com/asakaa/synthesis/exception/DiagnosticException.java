package com.asakaa.synthesis.exception;

public class DiagnosticException extends RuntimeException {

    public DiagnosticException(String message) {
        super(message);
    }

    public DiagnosticException(String message, Throwable cause) {
        super(message, cause);
    }
}
