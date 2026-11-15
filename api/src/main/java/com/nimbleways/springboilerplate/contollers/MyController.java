package com.nimbleways.springboilerplate.contollers;

import com.nimbleways.springboilerplate.dto.product.ProcessOrderResponse;
import com.nimbleways.springboilerplate.services.implementations.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Exposes the order processing HTTP endpoint. */
@RestController
@RequestMapping("/orders")
public class MyController {
    private final OrderService orderService;

    /**
     * Creates the controller.
     * @param orderService order processing orchestrator
     */
    public MyController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * Processes an order and returns its identifier with HTTP 200.
     * @param orderId identifier from the request path
     * @return unchanged response contract containing the order identifier
     * @throws com.nimbleways.springboilerplate.exceptions.OrderNotFoundException if the order does not exist
     */
    @PostMapping("{orderId}/processOrder")
    @ResponseStatus(HttpStatus.OK)
    public ProcessOrderResponse processOrder(@PathVariable Long orderId) {
        return new ProcessOrderResponse(orderService.processOrder(orderId));
    }
}
