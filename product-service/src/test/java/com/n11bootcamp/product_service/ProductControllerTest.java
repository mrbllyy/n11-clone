package com.n11bootcamp.product_service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.n11bootcamp.product_service.controller.ProductController;
import com.n11bootcamp.product_service.entity.Product;
import com.n11bootcamp.product_service.service.ProductService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@ActiveProfiles("test")
public class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testCreateProduct() throws Exception {
        Product mockProduct = new Product();
        mockProduct.setId(1L);
        mockProduct.setTitle("Test Product");
        mockProduct.setPrice(100);
        mockProduct.setBrand("Test Brand");

        Mockito.when(productService.createProduct(any(Product.class)))
                .thenReturn(ResponseEntity.ok(mockProduct));

        mockMvc.perform(post("/api/product")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mockProduct)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Test Product"))
                .andExpect(jsonPath("$.brand").value("Test Brand"));
    }

    @Test
    public void testGetAllProducts() throws Exception {
        Product p1 = new Product();
        p1.setId(1L);
        p1.setTitle("Product 1");

        Product p2 = new Product();
        p2.setId(2L);
        p2.setTitle("Product 2");

        List<Product> products = Arrays.asList(p1, p2);

        Mockito.when(productService.allProducts())
                .thenReturn(ResponseEntity.ok(products));

        mockMvc.perform(get("/api/product")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("Product 1"))
                .andExpect(jsonPath("$[1].title").value("Product 2"));
    }
}
