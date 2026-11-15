package com.nimbleways.springboilerplate.services.implementations;

import com.nimbleways.springboilerplate.contollers.MyController;
import com.nimbleways.springboilerplate.entities.Order;
import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.repositories.OrderRepository;
import com.nimbleways.springboilerplate.repositories.ProductRepository;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import java.time.Clock;
import java.time.ZoneOffset;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/** Characterizes existing stock and notification rules without changing production code. */
@ExtendWith(MockitoExtension.class)
class ProductCharacterizationTests {
    private static final LocalDate TODAY = LocalDate.of(2026, 6, 15);
    @Mock private ProductRepository products;
    @Mock private OrderRepository orders;
    @Mock private NotificationService notifications;

    private ProductService service() {
        ProductService service = new ProductService(products, notifications, Clock.fixed(TODAY.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC));
        return service;
    }

    @ParameterizedTest
    @CsvSource({"0", "-1", "15"})
    void delaySavesSuppliedLeadTimeAndNotifies(int leadTime) {
        Product product = product("NORMAL", 3, 8, -1, 1, 1);
        service().notifyDelay(leadTime, product);
        assertEquals(leadTime, product.getLeadTime());
        assertEquals(3, product.getAvailable());
        verify(products).save(product);
        verify(notifications).sendDelayNotification(leadTime, "item");
        verifyNoMoreInteractions(products, notifications);
    }

    @ParameterizedTest
    @CsvSource({
        "3,-1,0,EXPIRATION", "3,0,0,EXPIRATION", "3,1,2,NONE",
        "0,1,0,EXPIRATION", "0,0,0,EXPIRATION", "0,-1,0,EXPIRATION"
    })
    void expirableServiceUsesStrictExpiryBoundary(int stock, int expiry, int expected, String notification) {
        Product product = product("EXPIRABLE", stock, 2, -1, 1, expiry);
        {
            service().handleExpiredProduct(product);
        }
        assertOutcome(product, expected, notification);
    }

    @ParameterizedTest
    @CsvSource({
        "-1,1,2,0,OUT", "1,10,2,3,OUT", "0,2,2,3,DELAY",
        "-1,2,2,3,DELAY", "-1,0,0,3,DELAY", "-3,-1,0,0,OUT"
    })
    void seasonalServiceCharacterizesDeliveryAndSeasonBoundaries(
            int start, int end, int lead, int expected, String notification) {
        Product product = product("SEASONAL", 3, lead, start, end, 1);
        {
            service().handleSeasonalProduct(product);
        }
        assertOutcome(product, expected, notification);
    }

    @ParameterizedTest
    @CsvSource({
        "NORMAL,3,2,-1,10,1,2,NONE,true",
        "NORMAL,0,2,-1,10,1,0,DELAY,true",
        "NORMAL,0,0,-1,10,1,0,NONE,false",
        "NORMAL,0,-1,-1,10,1,0,NONE,false",
        "SEASONAL,3,2,-1,10,1,2,NONE,true",
        "SEASONAL,3,2,0,10,1,3,DELAY,true",
        "SEASONAL,3,0,-10,0,1,3,DELAY,true",
        "SEASONAL,3,2,-10,0,1,0,OUT,true",
        "SEASONAL,3,2,1,10,1,3,OUT,true",
        "SEASONAL,0,2,-1,10,1,0,DELAY,true",
        "SEASONAL,0,2,-1,2,1,0,DELAY,true",
        "SEASONAL,0,3,-1,2,1,0,OUT,true",
        "EXPIRABLE,3,2,-1,10,1,2,NONE,true",
        "EXPIRABLE,3,2,-1,10,0,0,EXPIRATION,true",
        "EXPIRABLE,3,2,-1,10,-1,0,EXPIRATION,true",
        "EXPIRABLE,0,2,-1,10,1,0,EXPIRATION,true"

    })
    void orderProcessingPreservesCurrentRules(String type, int stock, int lead, int start,
            int end, int expiry, int expected, String notification, boolean saved) {
        Product product = product(type, stock, lead, start, end, expiry);
        Order order = new Order(7L, Collections.singleton(product));
        when(orders.findById(7L)).thenReturn(Optional.of(order));
        MyController controller = new MyController(new OrderService(orders, service()));
        {
            assertEquals(7L, controller.processOrder(7L).id());
        }
        assertEquals(lead, product.getLeadTime());
        if (saved) {
            assertOutcome(product, expected, notification);
        } else {
            assertEquals(expected, product.getAvailable());
            verifyNoInteractions(products, notifications);
        }
        verify(orders).findById(7L);
        verifyNoMoreInteractions(orders);
    }

    private Product product(String type, int stock, int lead, int start, int end, int expiry) {
        return new Product(1L, lead, stock, type, "item", TODAY.plusDays(expiry),
                TODAY.plusDays(start), TODAY.plusDays(end));
    }

    private void assertOutcome(Product product, int stock, String notification) {
        assertEquals(stock, product.getAvailable());
        verify(products).save(product);
        switch (notification) {
            case "DELAY":
                verify(notifications).sendDelayNotification(product.getLeadTime(), "item");
                break;
            case "OUT":
                verify(notifications).sendOutOfStockNotification("item");
                break;
            case "EXPIRATION":
                verify(notifications).sendExpirationNotification("item", product.getExpiryDate());
                break;
            default:
                verifyNoInteractions(notifications);
        }
        verifyNoMoreInteractions(products, notifications);
    }
}
