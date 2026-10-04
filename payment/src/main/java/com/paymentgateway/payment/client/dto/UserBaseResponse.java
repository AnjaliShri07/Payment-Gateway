package com.paymentgateway.payment.client.dto;

public class UserBaseResponse<T> {
    private String status;
    private String message;
    private T data;

    public UserBaseResponse() {}

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
}
