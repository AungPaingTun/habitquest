package com.habitquest.common;

import org.springframework.http.HttpStatus;

/** Thrown by services for expected errors; GlobalExceptionHandler turns it into an HTTP response. */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
