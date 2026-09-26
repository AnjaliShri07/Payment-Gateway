package org.paymentgateway.auth.dto.response;

/**

 * Response payload returned by authentication service operations.

 */
public record ClientRegisterResponse(
    String clientId,
    String clientSecret
) {}
