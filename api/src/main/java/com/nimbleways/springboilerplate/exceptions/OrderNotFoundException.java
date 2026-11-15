package com.nimbleways.springboilerplate.exceptions;

/** Indicates that an order requested for processing does not exist. */
public class OrderNotFoundException extends RuntimeException {
    /**
     * Creates an error identifying the missing order.
     * @param orderId requested identifier
     */
    public OrderNotFoundException(Long orderId) {
        super("Order not found: " + orderId);
    }
}
