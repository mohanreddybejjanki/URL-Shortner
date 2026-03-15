package com.project.urlshortener.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a short code or alias does not exist in the database.
 *
 * @ResponseStatus(HttpStatus.NOT_FOUND) → Spring MVC will automatically
 * return HTTP 404 when this exception propagates out of a controller,
 * unless a @ControllerAdvice handles it first (which we do in GlobalExceptionHandler).
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class UrlNotFoundException extends RuntimeException {

    public UrlNotFoundException(String message) {
        super(message);
    }
}