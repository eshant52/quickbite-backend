package com.quickbite.quickbite.payment.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.quickbite.quickbite.payment.model.PaymentMethod;
import com.quickbite.quickbite.payment.model.PaymentStatus;

import java.math.BigDecimal;
import java.util.UUID;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = CodPaymentResult.class, name = "COD"),
        @JsonSubTypes.Type(value = StubOnlinePaymentResult.class, name = "STUB_ONLINE")
})
public sealed interface PaymentResult permits CodPaymentResult, StubOnlinePaymentResult {

    UUID paymentId();
    UUID orderId();
    String transactionId();
    PaymentMethod paymentMethod();
    PaymentStatus status();
    BigDecimal amount();
}
