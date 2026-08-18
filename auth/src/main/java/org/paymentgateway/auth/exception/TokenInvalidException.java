package org.paymentgateway.auth.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class TokenInvalidException extends RuntimeException {

    private final String token;

    public TokenInvalidException(String message) {
        super(message);
        this.token = null;
    }

    public TokenInvalidException(String token, String message) {
        super(String.format("Invalid token [%s]: %s", token, message));
        this.token = token;
    }

    public String getToken() {
        return token;
    }
}
