package com.nimbleways.springboilerplate.services.implementations;

import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.repositories.ProductRepository;
import com.nimbleways.springboilerplate.exceptions.UnknownProductTypeException;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.stereotype.Service;

/** Applies stock and notification rules for each supported product type. */
@Service
public class ProductService {
    private final ProductRepository productRepository;
    private final NotificationService notificationService;
    private final Clock clock;

    /**
     * Creates the service with its persistence, notification and time dependencies.
     * @param productRepository product persistence
     * @param notificationService notification delivery
     * @param clock clock used for calendar decisions
     */
    public ProductService(ProductRepository productRepository, NotificationService notificationService, Clock clock) {
        this.productRepository = productRepository;
        this.notificationService = notificationService;
        this.clock = clock;
    }

    /**
     * Processes one product, rejecting unsupported or missing types.
     * @throws UnknownProductTypeException if the type is unsupported or missing
     * @param product product whose stock may be updated
     * @throws NullPointerException if required product data is null
     */
    public void processProduct(Product product) {
        if (product.getType() == null) {
            throw new UnknownProductTypeException(null);
        }
        switch (product.getType()) {
            case "NORMAL":
                if (product.getAvailable() > 0) {
                    consumeProduct(product);
                } else if (product.getLeadTime() > 0) {
                    notifyDelay(product.getLeadTime(), product);
                }
                break;
            case "SEASONAL":
                LocalDate today = LocalDate.now(clock);
                if (today.isAfter(product.getSeasonStartDate()) && today.isBefore(product.getSeasonEndDate())
                        && product.getAvailable() > 0) {
                    consumeProduct(product);
                } else {
                    handleSeasonalProduct(product, today);
                }
                break;
            case "EXPIRABLE":
                handleExpiredProduct(product);
                break;
            default:
                throw new UnknownProductTypeException(product.getType());
        }
    }

    /**
     * Persists the supplied lead time, then sends a delay notification.
     * @param leadTime delivery delay in days, retained without validation
     * @param product product to update
     * @throws NullPointerException if product is null
     */
    public void notifyDelay(int leadTime, Product product) {
        product.setLeadTime(leadTime);
        productRepository.save(product);
        notificationService.sendDelayNotification(leadTime, product.getName());
    }

    /**
     * Handles seasonal unavailability; a delivery exactly at season end remains allowed.
     * @param product seasonal product to update and notify about
     * @throws NullPointerException if required seasonal data is null
     */
    public void handleSeasonalProduct(Product product) {
        handleSeasonalProduct(product, LocalDate.now(clock));
    }

    private void handleSeasonalProduct(Product product, LocalDate today) {
        if (today.plusDays(product.getLeadTime()).isAfter(product.getSeasonEndDate())) {
            notificationService.sendOutOfStockNotification(product.getName());
            product.setAvailable(0);
            productRepository.save(product);
        } else if (product.getSeasonStartDate().isAfter(today)) {
            notificationService.sendOutOfStockNotification(product.getName());
            productRepository.save(product);
        } else {
            notifyDelay(product.getLeadTime(), product);
        }
    }

    /**
     * Consumes stock only before expiry; otherwise sends expiration and clears stock,
     * including when stock is empty and expiry is in the future.
     * @param product expirable product to update
     * @throws NullPointerException if required expiry data is null
     */
    public void handleExpiredProduct(Product product) {
        if (product.getAvailable() > 0 && product.getExpiryDate().isAfter(LocalDate.now(clock))) {
            consumeProduct(product);
        } else {
            notificationService.sendExpirationNotification(product.getName(), product.getExpiryDate());
            product.setAvailable(0);
            productRepository.save(product);
        }
    }

    private void consumeProduct(Product product) {
        product.setAvailable(product.getAvailable() - 1);
        productRepository.save(product);
    }
}
