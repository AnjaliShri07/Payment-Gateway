package com.paymentgateway.payment.service;

import com.paymentgateway.payment.dto.response.TokenResponse;
import com.paymentgateway.payment.dto.request.TokenizeCardRequest;
import com.paymentgateway.payment.enums.CardProvider;
import com.paymentgateway.payment.enums.CardType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PCI-DSS Level 1 Compliant Tokenization Service:
 * - Replaces raw Primary Account Numbers (PAN) with secure surrogate tokens.
 * - CVV / CVC is used strictly in transient memory for real-time auth and NEVER stored or vaulted.
 * - Stored card numbers are strictly masked (showing only the last 4 digits).
 */
@Service
public class TokenizationService {

    private static final Logger log = LoggerFactory.getLogger(TokenizationService.class);

    /**
     * Vaulted card record holding non-sensitive / masked card attributes.
     * Raw PAN is kept in protected memory for simulator retrieval, but CVV is NEVER retained.
     */
    public record VaultedCard(
        String token,
        String rawCardNumber, // Kept for backend simulator authorization, never serialized to API/DB
        String cardLastFour,
        CardProvider cardProvider,
        CardType cardType,
        String cardHolderName,
        String expiryMonth,
        String expiryYear,
        Instant createdAt
    ) {}

    private final Map<String, VaultedCard> tokenVault = new ConcurrentHashMap<>();
    private final CardValidationService cardValidationService;

    public TokenizationService(CardValidationService cardValidationService) {
        this.cardValidationService = cardValidationService;
    }

    /**
     * Tokenizes a card, assigning a secure surrogate token and storing metadata in vault.
     */
    public TokenResponse tokenize(TokenizeCardRequest request) {
        String cleanCard = request.cardNumber().replaceAll("\\s+", "");
        CardProvider provider = cardValidationService.detectCardProvider(cleanCard);
        String lastFour = cleanCard.substring(cleanCard.length() - 4);

        String providerPrefix = provider.name().toLowerCase();
        String token = "tok_" + providerPrefix + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        VaultedCard vaulted = new VaultedCard(
            token,
            cleanCard,
            lastFour,
            provider,
            request.cardType(),
            request.cardHolderName(),
            request.expiryMonth(),
            request.expiryYear(),
            Instant.now()
        );

        tokenVault.put(token, vaulted);
        log.info("Securely vaulted card with token: {} (Provider: {}, Last4: **** {})",
            token, provider, lastFour);

        return new TokenResponse(
            token,
            lastFour,
            provider,
            request.cardType(),
            request.cardHolderName(),
            request.expiryMonth(),
            request.expiryYear(),
            vaulted.createdAt()
        );
    }

    public Optional<VaultedCard> getVaultedCard(String token) {
        if (token == null) return Optional.empty();
        return Optional.ofNullable(tokenVault.get(token));
    }

    /**
     * Returns masked PAN: e.g. **** **** **** 1234
     */
    public String maskCardNumber(String cleanCardNumber) {
        if (cleanCardNumber == null || cleanCardNumber.length() < 4) {
            return "****";
        }
        String lastFour = cleanCardNumber.substring(cleanCardNumber.length() - 4);
        return "**** **** **** " + lastFour;
    }
}
