package com.nimbleways.springboilerplate.services.implementations;

import com.nimbleways.springboilerplate.entities.Order;
import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.exceptions.UnknownProductTypeException;
import com.nimbleways.springboilerplate.repositories.OrderRepository;
import com.nimbleways.springboilerplate.repositories.ProductRepository;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.transaction.support.TransactionSynchronizationManager;
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
    void rollsBackStocksLoadedThroughAPersistedOrderAndItsJpaRelations() {
        assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
        Product first = products.saveAndFlush(new Product(null, 2, 3, "NORMAL", "persisted-first", null, null, null));
        Product second = products.saveAndFlush(new Product(null, 2, 5, "NORMAL", "persisted-second", null, null, null));
        Order order = orders.saveAndFlush(new Order(null, new LinkedHashSet<>(Arrays.asList(first, second))));
        Set<Long> processedIds = new HashSet<>();
        IllegalStateException failure = new IllegalStateException("Failure after both stock writes");
        doAnswer(invocation -> {
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
            Product product = invocation.getArgument(0);
            invocation.callRealMethod();
            entityManager.flush();
            assertEquals(product.getId().equals(first.getId()) ? 2 : 4, product.getAvailable());
            processedIds.add(product.getId());
            // Both items are NORMAL: the test is independent of JPA Set iteration order.
            if (processedIds.size() == 2) {
                throw failure;
            }
            return null;
        }).when(productService).processProduct(any(Product.class));

        // No stub on findById: the service loads the persisted order and its join-table items.
        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> orderService.processOrder(order.getId())));
        assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
        assertEquals(new HashSet<>(Arrays.asList(first.getId(), second.getId())), processedIds);
        assertEquals(3, products.findById(first.getId()).orElseThrow().getAvailable());
        assertEquals(5, products.findById(second.getId()).orElseThrow().getAvailable());
        assertTrue(orders.existsById(order.getId()));
        verify(orders).findById(order.getId());
        verify(productService, times(2)).processProduct(any(Product.class));
    }

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
