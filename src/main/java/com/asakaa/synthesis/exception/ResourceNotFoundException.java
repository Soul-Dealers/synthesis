package com.asakaa.synthesis.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resourceName, Long id) {
        super(String.format("%s with id %d was not found", resourceName, id));
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
