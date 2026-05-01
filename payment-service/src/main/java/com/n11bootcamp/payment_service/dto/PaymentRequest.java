package com.n11bootcamp.payment_service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class PaymentRequest {

    @NotNull
    private Long orderId;

    @NotBlank
    private String username;

    @NotNull
    @DecimalMin("0.01")
    private Double amount;

    private String paymentMethod;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String city;
    private String streetAddress;
    private String country;
    private String address;

    @Valid
    @NotNull
    private Card card;

    private List<Item> items;

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getStreetAddress() { return streetAddress; }
    public void setStreetAddress(String streetAddress) { this.streetAddress = streetAddress; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public Card getCard() { return card; }
    public void setCard(Card card) { this.card = card; }

    public List<Item> getItems() { return items; }
    public void setItems(List<Item> items) { this.items = items; }

    public static class Card {
        @NotBlank
        private String cardHolderName;

        @NotBlank
        private String cardNumber;

        @NotBlank
        private String expireMonth;

        @NotBlank
        private String expireYear;

        @NotBlank
        private String cvc;

        public String getCardHolderName() { return cardHolderName; }
        public void setCardHolderName(String cardHolderName) { this.cardHolderName = cardHolderName; }

        public String getCardNumber() { return cardNumber; }
        public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }

        public String getExpireMonth() { return expireMonth; }
        public void setExpireMonth(String expireMonth) { this.expireMonth = expireMonth; }

        public String getExpireYear() { return expireYear; }
        public void setExpireYear(String expireYear) { this.expireYear = expireYear; }

        public String getCvc() { return cvc; }
        public void setCvc(String cvc) { this.cvc = cvc; }
    }

    public static class Item {
        private Long productId;
        private String productName;
        private Double price;
        private Integer quantity;
        private String category1;
        private String category2;

        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }

        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }

        public Double getPrice() { return price; }
        public void setPrice(Double price) { this.price = price; }

        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }

        public String getCategory1() { return category1; }
        public void setCategory1(String category1) { this.category1 = category1; }

        public String getCategory2() { return category2; }
        public void setCategory2(String category2) { this.category2 = category2; }
    }
}
