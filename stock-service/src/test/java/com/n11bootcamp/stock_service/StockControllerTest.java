package com.n11bootcamp.stock_service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.n11bootcamp.stock_service.controller.StockController;
import com.n11bootcamp.stock_service.dto.StockUpdateRequest;
import com.n11bootcamp.stock_service.dto.StockUpdateResponse;
import com.n11bootcamp.stock_service.entity.ProductStock;
import com.n11bootcamp.stock_service.repository.ProductStockRepository;
import com.n11bootcamp.stock_service.service.StockDomainService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StockController.class)
@ActiveProfiles("test")
public class StockControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StockDomainService stockDomainService;

    @MockBean
    private ProductStockRepository productStockRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testGetAllStocks() throws Exception {
        ProductStock stock = new ProductStock();
        stock.setProductId(1L);
        stock.setQuantity(100);

        Mockito.when(productStockRepository.findAll())
                .thenReturn(Collections.singletonList(stock));

        mockMvc.perform(get("/api/stocks")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].productId").value(1L))
                .andExpect(jsonPath("$[0].quantity").value(100));
    }

    @Test
    public void testIncreaseStock() throws Exception {
        StockUpdateRequest request = new StockUpdateRequest();
        StockUpdateRequest.StockItem item = new StockUpdateRequest.StockItem(1L, 50);
        request.setItems(Collections.singletonList(item));

        StockUpdateResponse response = new StockUpdateResponse(true, "Stock increased");

        Mockito.when(stockDomainService.increase(any(StockUpdateRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/stocks/increase")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Stock increased"));
    }
}
