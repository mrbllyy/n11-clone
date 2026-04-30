package com.n11bootcamp.order_service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.n11bootcamp.order_service.controller.OrderController;
import com.n11bootcamp.order_service.dto.CreateOrderRequest;
import com.n11bootcamp.order_service.dto.OrderResponse;
import com.n11bootcamp.order_service.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@ActiveProfiles("test")
public class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderServiceImpl orderServiceImpl;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testCreateOrder() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setUsername("testuser");

        OrderResponse response = new OrderResponse();
        response.setOrderId(1L);
        response.setUsername("testuser");
        response.setStatus("CREATED");

        Mockito.when(orderServiceImpl.createOrder(any(CreateOrderRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(1L))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.status").value("CREATED"));
    }

    @Test
    public void testGetAllOrders() throws Exception {
        OrderResponse response = new OrderResponse();
        response.setOrderId(1L);
        response.setUsername("testuser");

        Mockito.when(orderServiceImpl.findAllOrders())
                .thenReturn(Collections.singletonList(response));

        mockMvc.perform(get("/api/orders/all")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].orderId").value(1L));
    }
}
