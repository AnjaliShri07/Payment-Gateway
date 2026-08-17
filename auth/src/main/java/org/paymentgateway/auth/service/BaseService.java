package org.paymentgateway.auth.service;

import org.springframework.beans.factory.annotation.Value;

/**
 * Base service class for common utilities.
 *
 * OpenAI docs: https://platform.openai.com/docs
 * Java docs: https://docs.oracle.com/en/java/
 */
public abstract class BaseService {
    protected static final String TOKEN_ISSUER = "auth-service";

    @Value("${access-token-validity-seconds}")
    protected long accessTokenValiditySeconds;
    // common helpers can be added here
}