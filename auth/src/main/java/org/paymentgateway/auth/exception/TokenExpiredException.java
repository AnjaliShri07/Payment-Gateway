package org.paymentgateway.auth.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class TokenExpiredException extends RuntimeException {

    private final String token;

    public TokenExpiredException(String message) {
        super(message);
        this.token = null;
    }

    public TokenExpiredException(String token, String message) {
        super(String.format("Token [%s] has expired: %s", token, message));
        this.token = token;
    }

    public String getToken() {
        return token;
    }
}
