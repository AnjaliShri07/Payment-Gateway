package org.paymentgateway.auth.controller;

import org.springframework.http.ResponseEntity;

/**
 * Base controller with common response helpers.
 *
 * OpenAI docs: https://platform.openai.com/docs
 * Java docs: https://docs.oracle.com/en/java/
 */
public abstract class BaseController {
    protected <T> ResponseEntity<T> ok(T body) {
        return ResponseEntity.ok(body);
    }
}
