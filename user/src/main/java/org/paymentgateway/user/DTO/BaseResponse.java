package org.paymentgateway.user.DTO;

/**
 * Generic response wrapper used by REST endpoints to standardize success and error payloads.
 *
 * @param <T> the type of payload carried in the response data field
 */
public class BaseResponse<T> {
    private String status;   // SUCCESS, ERROR
    private String message;  // human-readable message
    private T data;          // actual payload

    public BaseResponse() {}

    public BaseResponse(String status, String message, T data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }

    // Static factory methods for convenience
    public static <T> BaseResponse<T> success(String message, T data) {
        return new BaseResponse<>("SUCCESS", message, data);
    }

    public static <T> BaseResponse<T> error(String message, T data) {
        return new BaseResponse<>("ERROR", message, data);
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
