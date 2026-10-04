package com.paymentgateway.payment.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.paymentgateway.payment.dto.response.TokenResponse;
import com.paymentgateway.payment.dto.request.TokenizeCardRequest;
import com.paymentgateway.payment.enums.CardProvider;
import com.paymentgateway.payment.enums.CardType;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class TokenizationServiceTest {

    private TokenizationService tokenizationService;
    private CardValidationService cardValidationService;

    @BeforeEach
    void setUp() {
        cardValidationService = new CardValidationService();
        tokenizationService = new TokenizationService(cardValidationService);
    }

    @Test
    @DisplayName("Tokenize card creates valid tok_ token and masks last 4 digits")
    void testTokenizeCard() {
        TokenizeCardRequest request = new TokenizeCardRequest(
            CardType.CREDIT,
            "4532015112830366",
            "Alice Smith",
            "12",
            "2030",
            "123"
        );

        TokenResponse response = tokenizationService.tokenize(request);

        assertNotNull(response);
        assertNotNull(response.cardToken());
        assertTrue(response.cardToken().startsWith("tok_visa_"));
        assertEquals("0366", response.cardLastFour());
        assertEquals(CardProvider.VISA, response.cardProvider());
        assertEquals(CardType.CREDIT, response.cardType());

        // Vault verification
        Optional<TokenizationService.VaultedCard> vaultedOpt = tokenizationService.getVaultedCard(response.cardToken());
        assertTrue(vaultedOpt.isPresent());
        assertEquals("0366", vaultedOpt.get().cardLastFour());
        assertEquals("Alice Smith", vaultedOpt.get().cardHolderName());
    }

    @Test
    @DisplayName("Mask card number formats PAN to **** **** **** 1234")
    void testMaskCardNumber() {
        String masked = tokenizationService.maskCardNumber("4532015112830366");
        assertEquals("**** **** **** 0366", masked);
    }
}
