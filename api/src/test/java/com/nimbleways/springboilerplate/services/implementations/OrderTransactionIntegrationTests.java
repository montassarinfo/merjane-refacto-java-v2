package com.nimbleways.springboilerplate.services.implementations;

import com.nimbleways.springboilerplate.entities.Order;
import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.exceptions.UnknownProductTypeException;
import com.nimbleways.springboilerplate.repositories.OrderRepository;
import com.nimbleways.springboilerplate.repositories.ProductRepository;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Optional;
import javax.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Verifies rollback through the Spring transaction proxy and real product persistence. */
@SpringBootTest
class OrderTransactionIntegrationTests {
    @Autowired private OrderService orderService;
    @Autowired private ProductRepository products;
    @Autowired private EntityManager entityManager;
    @SpyBean private OrderRepository orders;
    @SpyBean private ProductService productService;

    @Test
    void rollsBackFlushedStockWhenALaterProductHasUnknownType() {
        Product normal = products.saveAndFlush(new Product(null, 2, 3, "NORMAL", "rollback-normal", null, null, null));
        Product unknown = products.saveAndFlush(new Product(null, 2, 3, "UNKNOWN", "rollback-unknown", null, null, null));
        // A JPA Set has no ordering guarantee: fix only the read order so the failure occurs second.
        Order order = new Order(90001L, new LinkedHashSet<>(Arrays.asList(normal, unknown)));
        doReturn(Optional.of(order)).when(orders).findById(90001L);
        doAnswer(invocation -> {
            invocation.callRealMethod();
            entityManager.flush();
            assertEquals(2, products.findById(normal.getId()).orElseThrow().getAvailable());
            return null;
        }).when(productService).processProduct(normal);

        // The test itself has no transaction: only OrderService can roll back this flushed write.
        assertThrows(UnknownProductTypeException.class, () -> orderService.processOrder(90001L));
        assertEquals(3, products.findById(normal.getId()).orElseThrow().getAvailable());
        assertEquals(3, products.findById(unknown.getId()).orElseThrow().getAvailable());
        verify(productService).processProduct(normal);
        verify(productService).processProduct(unknown);
    }
}
