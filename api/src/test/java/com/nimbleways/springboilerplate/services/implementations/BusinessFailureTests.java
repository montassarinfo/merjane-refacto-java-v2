package com.nimbleways.springboilerplate.services.implementations;

import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.exceptions.OrderNotFoundException;
import com.nimbleways.springboilerplate.exceptions.UnknownProductTypeException;
import com.nimbleways.springboilerplate.repositories.OrderRepository;
import com.nimbleways.springboilerplate.repositories.ProductRepository;
import java.time.Clock;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Verifies explicit business failures without side effects. */
@ExtendWith(MockitoExtension.class)
class BusinessFailureTests {
    @Mock private ProductRepository products;
    @Mock private OrderRepository orders;
    @Mock private NotificationService notifications;

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"UNKNOWN", "normal"})
    void rejectsUnknownTypeBeforeSavingOrNotifying(String type) {
        Product product = new Product(1L, 2, 3, type, "item", null, null, null);
        ProductService service = new ProductService(products, notifications, Clock.systemUTC());
        UnknownProductTypeException error = assertThrows(UnknownProductTypeException.class,
                () -> service.processProduct(product));
        assertEquals("Unknown product type: " + type, error.getMessage());
        assertEquals(3, product.getAvailable());
        verifyNoInteractions(products, notifications);
    }

    @Test
    void missingOrderRaisesExplicitExceptionWithoutProcessingProducts() {
        ProductService productsService = mock(ProductService.class);
        when(orders.findById(7L)).thenReturn(Optional.empty());
        OrderService service = new OrderService(orders, productsService);
        assertThrows(OrderNotFoundException.class, () -> service.processOrder(7L));
        verify(orders).findById(7L);
        verifyNoInteractions(productsService);
    }
}
