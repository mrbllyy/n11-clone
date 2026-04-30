package com.n11bootcamp.user_service.entity;

/**
 * DTO — Shopping Cart Service'ten gelen JSON'u deserialize etmek için kullanılır.
 * Bu class JPA entity DEĞİLDİR, user-db'de tablo oluşturmaz.
 */
public class ShoppingCart {
    private long id;
    private String shoppingCartName;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getShoppingCartName() {
        return shoppingCartName;
    }

    public void setShoppingCartName(String shoppingCartName) {
        this.shoppingCartName = shoppingCartName;
    }

}