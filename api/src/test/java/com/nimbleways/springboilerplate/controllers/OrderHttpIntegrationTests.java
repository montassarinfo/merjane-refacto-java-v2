package com.nimbleways.springboilerplate.controllers;

import com.nimbleways.springboilerplate.contollers.MyController;
import com.nimbleways.springboilerplate.exceptions.OrderNotFoundException;
import com.nimbleways.springboilerplate.exceptions.UnknownProductTypeException;
import com.nimbleways.springboilerplate.services.implementations.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Checks success and business error HTTP contracts independently of business rules. */
@WebMvcTest(MyController.class)
class OrderHttpIntegrationTests {
    @Autowired private MockMvc mvc;
    @MockBean private OrderService orders;

    @Test
    void returnsUnchangedSuccessBody() throws Exception {
        when(orders.processOrder(7L)).thenReturn(7L);
        mvc.perform(post("/orders/7/processOrder"))
                .andExpect(status().isOk()).andExpect(content().json("{\"id\":7}"));
        verify(orders).processOrder(7L);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void translatesBusinessExceptionsTo422(boolean missingOrder) throws Exception {
        RuntimeException failure = missingOrder ? new OrderNotFoundException(7L)
                : new UnknownProductTypeException("UNKNOWN");
        when(orders.processOrder(7L)).thenThrow(failure);
        mvc.perform(post("/orders/7/processOrder"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value(failure.getMessage()));
        verify(orders).processOrder(7L);
    }
}
