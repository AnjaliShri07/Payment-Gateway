package org.paymentgateway.auth.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class TokenRefreshException extends RuntimeException {

    private final String token;

    public TokenRefreshException(String token, String message) {
        super(String.format("Failed for token [%s]: %s", token, message));
        this.token = token;
    }

    public String getToken() {
        return token;
    }
}
