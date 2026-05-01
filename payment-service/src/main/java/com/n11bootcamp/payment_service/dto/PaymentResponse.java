package com.n11bootcamp.payment_service.dto;

public class PaymentResponse {

    private boolean success;
    private String transactionId;
    private String message;

    public PaymentResponse() {
    }

    public PaymentResponse(boolean success, String transactionId, String message) {
        this.success = success;
        this.transactionId = transactionId;
        this.message = message;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public static PaymentResponse ok(String transactionId) {
        return new PaymentResponse(true, transactionId, "Payment approved");
    }

    public static PaymentResponse fail(String message) {
        return new PaymentResponse(false, null, message);
    }
}
