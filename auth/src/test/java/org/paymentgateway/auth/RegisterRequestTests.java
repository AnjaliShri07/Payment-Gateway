package org.paymentgateway.auth;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.paymentgateway.auth.dto.request.RegisterRequest;

import static org.junit.jupiter.api.Assertions.assertFalse;

class RegisterRequestTests {

    @Test
    void missingAdminAccessFlagDefaultsToFalse() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper()
            .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES);

        RegisterRequest request = objectMapper.readValue(
            "{\"username\":\"alice\",\"email\":\"alice@example.com\",\"password\":\"Password1!\"}",
            RegisterRequest.class
        );

        assertFalse(request.requestAdminAccess());
    }
}
