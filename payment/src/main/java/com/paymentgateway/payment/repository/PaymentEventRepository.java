package com.paymentgateway.payment.repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.paymentgateway.payment.document.PaymentEventDocument;
import com.paymentgateway.payment.model.PaymentDetails;
import com.paymentgateway.payment.model.PaymentPayload;
import com.paymentgateway.payment.enums.PaymentStatus;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Repository
public class PaymentEventRepository {

    private final MongoCollection<Document> collection;

    public PaymentEventRepository(MongoDatabase database) {
        this.collection = database.getCollection("payment_events");
    }

    public PaymentEventDocument save(PaymentEventDocument event) {
        PaymentPayload payload = event.getPayload();
        PaymentDetails payment = event.getPayment();
        Document payloadDocument = new Document()
            .append("userId", payload.userId())
            .append("username", payload.username())
            .append("email", payload.email())
            .append("roles", payload.roles())
            .append("enabled", payload.enabled())
            .append("createdAt", toDate(payload.createdAt()))
            .append("updatedAt", toDate(payload.updatedAt()));
        Document paymentDocument = new Document()
            .append("transactionId", payment.getTransactionId())
            .append("idempotencyKey", payment.getIdempotencyKey())
            .append("cardHolderName", payment.getCardHolderName())
            .append("cardLastFour", payment.getCardLastFour())
            .append("cardToken", payment.getCardToken())
            .append("cardType", payment.getCardType() != null ? payment.getCardType().name() : null)
            .append("cardProvider", payment.getCardProvider() != null ? payment.getCardProvider().name() : null)
            .append("amount", toDecimal128(payment.getAmount()))
            .append("currency", payment.getCurrency())
            .append("status", payment.getStatus() != null ? payment.getStatus().name() : null)
            .append("authCode", payment.getAuthCode())
            .append("acquirerReferenceNumber", payment.getAcquirerReferenceNumber())
            .append("riskScore", payment.getRiskScore())
            .append("description", payment.getDescription())
            .append("failureReason", payment.getFailureReason())
            .append("refundAmount", toDecimal128(payment.getRefundAmount()))
            .append("refundReason", payment.getRefundReason());
        Document document = new Document()
            .append("eventType", event.getEventType())
            .append("payload", payloadDocument)
            .append("payment", paymentDocument)
            .append("eventTime", toDate(event.getEventTime()))
            .append("storedAt", toDate(event.getStoredAt()));
        if (event.getId() != null && ObjectId.isValid(event.getId())) {
            document.put("_id", new ObjectId(event.getId()));
        }

        collection.insertOne(document);
        event.setId(document.getObjectId("_id").toHexString());
        return event;
    }

    public List<PaymentEventDocument> findByTransactionIdOrderByEventTimeAsc(String transactionId) {
        List<PaymentEventDocument> results = new ArrayList<>();
        for (Document document : collection.find(Filters.or(
            Filters.eq("payment.transactionId", transactionId),
            Filters.eq("transactionId", transactionId)
        ))
            .sort(Sorts.ascending("eventTime"))) {
            results.add(toPaymentEventDocument(document));
        }
        return results;
    }

    private PaymentEventDocument toPaymentEventDocument(Document document) {
        PaymentEventDocument event = new PaymentEventDocument();
        Object id = document.get("_id");
        event.setId(id != null ? id.toString() : null);
        event.setEventType(document.getString("eventType"));
        Document payload = document.get("payload", Document.class);
        Document payment = document.get("payment", Document.class);
        if (payload != null && payment != null) {
            event.setPayload(new PaymentPayload(
                toLong(payload.get("userId")),
                payload.getString("username"),
                payload.getString("email"),
                payload.getList("roles", String.class),
                payload.getBoolean("enabled"),
                toInstant(payload.get("createdAt")),
                toInstant(payload.get("updatedAt"))
            ));
            PaymentDetails details = new PaymentDetails();
            details.setTransactionId(payment.getString("transactionId"));
            details.setIdempotencyKey(payment.getString("idempotencyKey"));
            details.setCardHolderName(payment.getString("cardHolderName"));
            details.setCardLastFour(payment.getString("cardLastFour"));
            details.setCardToken(payment.getString("cardToken"));
            String cardType = payment.getString("cardType");
            details.setCardType(cardType != null ? com.paymentgateway.payment.enums.CardType.valueOf(cardType) : null);
            String provider = payment.getString("cardProvider");
            details.setCardProvider(provider != null ? com.paymentgateway.payment.enums.CardProvider.valueOf(provider) : null);
            details.setAmount(toBigDecimal(payment.get("amount")));
            details.setCurrency(payment.getString("currency"));
            String status = payment.getString("status");
            details.setStatus(status != null ? PaymentStatus.valueOf(status) : null);
            details.setAuthCode(payment.getString("authCode"));
            details.setAcquirerReferenceNumber(payment.getString("acquirerReferenceNumber"));
            details.setRiskScore(payment.getInteger("riskScore"));
            details.setDescription(payment.getString("description"));
            details.setFailureReason(payment.getString("failureReason"));
            details.setRefundAmount(toBigDecimal(payment.get("refundAmount")));
            details.setRefundReason(payment.getString("refundReason"));
            event.setPayment(details);
        } else {
            PaymentPayload legacyPayload = new PaymentPayload(
                toLong(document.get("userId")),
                null,
                null,
                null,
                null,
                null,
                null
            );
            event.setPayload(legacyPayload);
            PaymentDetails legacyPayment = new PaymentDetails();
            legacyPayment.setTransactionId(document.getString("transactionId"));
            legacyPayment.setAmount(toBigDecimal(document.get("amount")));
            legacyPayment.setCurrency(document.getString("currency"));
            String status = document.getString("status");
            legacyPayment.setStatus(status != null ? PaymentStatus.valueOf(status) : null);
            legacyPayment.setAuthCode(document.getString("authCode"));
            legacyPayment.setAcquirerReferenceNumber(document.getString("acquirerReferenceNumber"));
            legacyPayment.setRiskScore(document.getInteger("riskScore"));
            legacyPayment.setDescription(document.getString("description"));
            legacyPayment.setFailureReason(document.getString("failureReason"));
            legacyPayment.setRefundAmount(toBigDecimal(document.get("refundAmount")));
            legacyPayment.setRefundReason(document.getString("refundReason"));
            event.setPayment(legacyPayment);
        }
        event.setEventTime(toInstant(document.get("eventTime")));
        event.setStoredAt(toInstant(document.get("storedAt")));
        return event;
    }

    private static Decimal128 toDecimal128(BigDecimal value) {
        return value != null ? new Decimal128(value) : null;
    }

    private static Date toDate(Instant value) {
        return value != null ? Date.from(value) : null;
    }

    private static Long toLong(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value instanceof Decimal128 decimal128) {
            return decimal128.bigDecimalValue();
        }
        if (value instanceof Number number) {
            return new BigDecimal(number.toString());
        }
        return null;
    }

    private static Instant toInstant(Object value) {
        return value instanceof Date date ? date.toInstant() : null;
    }
}
