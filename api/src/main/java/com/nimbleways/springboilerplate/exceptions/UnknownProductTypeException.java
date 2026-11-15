package com.nimbleways.springboilerplate.exceptions;

/** Indicates an unsupported product type, including a missing type. */
public class UnknownProductTypeException extends RuntimeException {
    /**
     * Creates an error identifying the unsupported type.
     * @param type unsupported or null product type
     */
    public UnknownProductTypeException(String type) {
        super("Unknown product type: " + type);
    }
}
