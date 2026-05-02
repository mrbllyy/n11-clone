package com.n11bootcamp.order_service.dto;
import java.util.List;

public class CreateOrderRequest {

    private String username;
    private List<OrderItemDto> items;
    private AddressInfo addressInfo;
    private PaymentCard paymentCard;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public List<OrderItemDto> getItems() { return items; }
    public void setItems(List<OrderItemDto> items) { this.items = items; }

    public AddressInfo getAddressInfo() { return addressInfo; }
    public void setAddressInfo(AddressInfo addressInfo) { this.addressInfo = addressInfo; }

    public PaymentCard getPaymentCard() { return paymentCard; }
    public void setPaymentCard(PaymentCard paymentCard) { this.paymentCard = paymentCard; }

    public static class OrderItemDto {
        private Long productId;
        private Integer quantity;
        // Opsiyonel: Backend tarafından doldurulacak alanlar
        private String productName;
        private Double price;

        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }

        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }

        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }

        public Double getPrice() { return price; }
        public void setPrice(Double price) { this.price = price; }
    }

    public static class AddressInfo {
        private String fullName;
        private String phone;
        private String city;
        private String district;
        private String fullAddress;

        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }

        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }

        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }

        public String getDistrict() { return district; }
        public void setDistrict(String district) { this.district = district; }

        public String getFullAddress() { return fullAddress; }
        public void setFullAddress(String fullAddress) { this.fullAddress = fullAddress; }
    }

    public static class PaymentCard {
        private String cardHolderName;
        private String cardNumber;
        private String expireDate; // "MM/YY" formatında gelebilir
        private String cvv;

        public String getCardHolderName() { return cardHolderName; }
        public void setCardHolderName(String cardHolderName) { this.cardHolderName = cardHolderName; }

        public String getCardNumber() { return cardNumber; }
        public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }

        public String getExpireDate() { return expireDate; }
        public void setExpireDate(String expireDate) { this.expireDate = expireDate; }

        public String getCvv() { return cvv; }
        public void setCvv(String cvv) { this.cvv = cvv; }
    }
}