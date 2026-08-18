package org.paymentgateway.auth.constants;

public final class SecurityConstants {

    private SecurityConstants() {
        // Prevent instantiation
    }

    public static final String AUTH_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String TOKEN_TYPE = "Bearer";

    // Claims Keys
    public static final String CLAIM_USER_ID = "userId";
    public static final String CLAIM_EMAIL = "email";
    public static final String CLAIM_ROLES = "roles";

    // Common Messages
    public static final String MSG_UNAUTHORIZED = "Unauthorized: Full authentication is required to access this resource";
    public static final String MSG_ACCESS_DENIED = "Forbidden: You do not have permission to access this resource";
    public static final String MSG_USER_NOT_FOUND = "User not found";
    public static final String MSG_TOKEN_EXPIRED = "Token has expired. Please sign in again.";
    public static final String MSG_TOKEN_INVALID = "Invalid token provided.";
}
