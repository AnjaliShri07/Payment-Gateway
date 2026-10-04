package com.paymentgateway.payment.repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import org.bson.conversions.Bson;
import com.paymentgateway.payment.document.PaymentAuditLog;
import com.paymentgateway.payment.enums.PaymentStatus;
import com.paymentgateway.payment.model.PaymentDetails;
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
public class PaymentAuditLogRepository {

    private final MongoCollection<Document> collection;

    public PaymentAuditLogRepository(MongoDatabase database) {
        this.collection = database.getCollection("payment_audit_logs");
    }

    public PaymentAuditLog save(PaymentAuditLog auditLog) {
        var payload = auditLog.getPayload();
        var payment = auditLog.getPayment();
        Document payloadDocument = new Document()
            .append("user_id", payload.userId())
            .append("username", payload.username())
            .append("email", payload.email())
            .append("roles", payload.roles())
            .append("enabled", payload.enabled())
            .append("created_at", toDate(payload.createdAt()))
            .append("updated_at", toDate(payload.updatedAt()));
        Document paymentDocument = new Document()
            .append("transaction_id", payment.getTransactionId())
            .append("idempotency_key", payment.getIdempotencyKey())
            .append("card_holder_name", payment.getCardHolderName())
            .append("status", payment.getStatus() != null ? payment.getStatus().name() : null)
            .append("amount", toDecimal128(payment.getAmount()))
            .append("currency", payment.getCurrency())
            .append("card_last_four", payment.getCardLastFour())
            .append("card_token", payment.getCardToken())
            .append("card_type", payment.getCardType() != null ? payment.getCardType().name() : null)
            .append("card_provider", payment.getCardProvider() != null ? payment.getCardProvider().name() : null)
            .append("auth_code", payment.getAuthCode())
            .append("acquirer_reference_number", payment.getAcquirerReferenceNumber())
            .append("risk_score", payment.getRiskScore())
            .append("description", payment.getDescription())
            .append("failure_reason", payment.getFailureReason())
            .append("refund_amount", toDecimal128(payment.getRefundAmount()))
            .append("refund_reason", payment.getRefundReason());
        Document document = new Document()
            .append("payload", payloadDocument)
            .append("payment", paymentDocument)
            .append("event_time", toDate(auditLog.getEventTime()))
            .append("created_at", toDate(auditLog.getCreatedAt()));
        if (auditLog.getId() != null && ObjectId.isValid(auditLog.getId())) {
            document.put("_id", new ObjectId(auditLog.getId()));
        }

        collection.insertOne(document);
        auditLog.setId(document.getObjectId("_id").toHexString());
        return auditLog;
    }

    public List<PaymentAuditLog> findByTransactionIdOrderByCreatedAtDesc(String transactionId) {
        return find(
            Filters.or(Filters.eq("payment.transaction_id", transactionId), Filters.eq("transaction_id", transactionId)),
            Sorts.descending("created_at")
        );
    }

    public List<PaymentAuditLog> findByUserIdOrderByCreatedAtDesc(Long userId) {
        return find(
            Filters.or(Filters.eq("payload.user_id", userId), Filters.eq("user_id", userId)),
            Sorts.descending("created_at")
        );
    }

    public List<PaymentAuditLog> findByStatus(PaymentStatus status) {
        String value = status != null ? status.name() : null;
        return find(
            Filters.or(Filters.eq("payment.status", value), Filters.eq("status", value)),
            Sorts.ascending("created_at")
        );
    }

    private List<PaymentAuditLog> find(Bson filter, Bson sort) {
        List<PaymentAuditLog> results = new ArrayList<>();
        for (Document document : collection.find(filter).sort(sort)) {
            results.add(toAuditLog(document));
        }
        return results;
    }

    private PaymentAuditLog toAuditLog(Document document) {
        PaymentAuditLog auditLog = new PaymentAuditLog();
        Object id = document.get("_id");
        auditLog.setId(id != null ? id.toString() : null);
        Document payload = document.get("payload", Document.class);
        Document payment = document.get("payment", Document.class);
        if (payload != null && payment != null) {
            auditLog.setPayload(new com.paymentgateway.payment.model.PaymentPayload(
                toLong(payload.get("user_id")),
                payload.getString("username"),
                payload.getString("email"),
                payload.getList("roles", String.class),
                payload.getBoolean("enabled"),
                toInstant(payload.get("created_at")),
                toInstant(payload.get("updated_at"))
            ));
            PaymentDetails details = new PaymentDetails();
            details.setTransactionId(payment.getString("transaction_id"));
            details.setIdempotencyKey(payment.getString("idempotency_key"));
            details.setCardHolderName(payment.getString("card_holder_name"));
            details.setCardLastFour(payment.getString("card_last_four"));
            details.setCardToken(payment.getString("card_token"));
            String cardType = payment.getString("card_type");
            details.setCardType(cardType != null ? com.paymentgateway.payment.enums.CardType.valueOf(cardType) : null);
            String provider = payment.getString("card_provider");
            details.setCardProvider(provider != null ? com.paymentgateway.payment.enums.CardProvider.valueOf(provider) : null);
            details.setAmount(toBigDecimal(payment.get("amount")));
            details.setCurrency(payment.getString("currency"));
            String status = payment.getString("status");
            details.setStatus(status != null ? PaymentStatus.valueOf(status) : null);
            details.setAuthCode(payment.getString("auth_code"));
            details.setAcquirerReferenceNumber(payment.getString("acquirer_reference_number"));
            details.setRiskScore(payment.getInteger("risk_score"));
            details.setDescription(payment.getString("description"));
            details.setFailureReason(payment.getString("failure_reason"));
            details.setRefundAmount(toBigDecimal(payment.get("refund_amount")));
            details.setRefundReason(payment.getString("refund_reason"));
            auditLog.setPayment(details);
        } else {
            // Preserve compatibility with audit records written before the nested document format.
            auditLog.setTransactionId(document.getString("transaction_id"));
            auditLog.setUserId(toLong(document.get("user_id")));
            auditLog.setIdempotencyKey(document.getString("idempotency_key"));
            String status = document.getString("status");
            auditLog.setStatus(status != null ? PaymentStatus.valueOf(status) : null);
            auditLog.setAmount(toBigDecimal(document.get("amount")));
            auditLog.setCurrency(document.getString("currency"));
            auditLog.setCardLastFour(document.getString("card_last_four"));
            auditLog.setCardToken(document.getString("card_token"));
            auditLog.setCardType(document.getString("card_type"));
            auditLog.setCardProvider(document.getString("card_provider"));
            auditLog.setAuthCode(document.getString("auth_code"));
            auditLog.setRiskScore(document.getInteger("risk_score"));
            auditLog.setDescription(document.getString("description"));
            auditLog.setFailureReason(document.getString("failure_reason"));
        }
        auditLog.setEventTime(toInstant(document.get("event_time")));
        auditLog.setCreatedAt(toInstant(document.get("created_at")));
        return auditLog;
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
