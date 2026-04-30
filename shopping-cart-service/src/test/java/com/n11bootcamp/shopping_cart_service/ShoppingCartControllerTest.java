package com.n11bootcamp.shopping_cart_service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.n11bootcamp.shopping_cart_service.controller.ShoppingCartController;
import com.n11bootcamp.shopping_cart_service.entity.Product;
import com.n11bootcamp.shopping_cart_service.entity.ShoppingCart;
import com.n11bootcamp.shopping_cart_service.service.ShoppingCartService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ShoppingCartController.class)
@ActiveProfiles("test")
public class ShoppingCartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ShoppingCartService shoppingCartService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testCreateCart() throws Exception {
        ShoppingCart cart = new ShoppingCart();
        cart.setId(1L);
        cart.setShoppingCartName("TestCart");

        Mockito.when(shoppingCartService.createCart("TestCart"))
                .thenReturn(ResponseEntity.ok(cart));

        mockMvc.perform(post("/api/shopping-cart")
                .param("name", "TestCart")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.shoppingCartName").value("TestCart"));
    }

    @Test
    public void testAddProductsToCart() throws Exception {
        Product p1 = new Product();
        p1.setId(1L);
        p1.setTitle("Product 1");

        ShoppingCart cart = new ShoppingCart();
        cart.setId(1L);
        cart.setShoppingCartName("TestCart");
        cart.setProducts(Collections.singleton(p1));

        Mockito.when(shoppingCartService.addProducts(eq(1L), any(List.class)))
                .thenReturn(ResponseEntity.ok(cart));

        mockMvc.perform(post("/api/shopping-cart/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Collections.singletonList(p1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.products[0].title").value("Product 1"));
    }
}
