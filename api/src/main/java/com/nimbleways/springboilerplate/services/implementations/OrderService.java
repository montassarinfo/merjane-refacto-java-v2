package com.nimbleways.springboilerplate.services.implementations;

import com.nimbleways.springboilerplate.entities.Order;
import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.repositories.OrderRepository;
import com.nimbleways.springboilerplate.exceptions.OrderNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

/** Loads orders and delegates each product to the product service. */
@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final ProductService productService;

    /**
     * Creates the order orchestrator.
     * @param orderRepository order persistence
     * @param productService product business rules
     */
    public OrderService(OrderRepository orderRepository, ProductService productService) {
        this.orderRepository = orderRepository;
        this.productService = productService;
    }

    /**
     * Processes each item using the existing iteration and persistence behavior.
     * @param orderId identifier of the order
     * @return identifier of the processed order
     * @throws OrderNotFoundException if the order does not exist
     */
    @Transactional
    public Long processOrder(Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
        for (Product product : order.getItems()) {
            productService.processProduct(product);
        }
        return order.getId();
    }
}
