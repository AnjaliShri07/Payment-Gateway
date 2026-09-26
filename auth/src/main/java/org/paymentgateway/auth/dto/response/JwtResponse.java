package org.paymentgateway.auth.dto.response;

/**

 * Response payload returned by authentication service operations.

 */
public class JwtResponse {
    private final String token;

    public JwtResponse(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }
}
