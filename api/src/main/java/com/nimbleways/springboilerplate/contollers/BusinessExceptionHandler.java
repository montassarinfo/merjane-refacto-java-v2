package com.nimbleways.springboilerplate.contollers;

import com.nimbleways.springboilerplate.exceptions.OrderNotFoundException;
import com.nimbleways.springboilerplate.exceptions.UnknownProductTypeException;
import java.util.Collections;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Translates explicit business errors into the HTTP 422 contract. */
@RestControllerAdvice
public class BusinessExceptionHandler {
    /**
     * Exposes the business error message without technical stack details.
     * @param exception missing order or unsupported product type
     * @return JSON object with a message field
     */
    @ExceptionHandler({OrderNotFoundException.class, UnknownProductTypeException.class})
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public Map<String, String> handleBusinessException(RuntimeException exception) {
        return Collections.singletonMap("message", exception.getMessage());
    }
}
