package com.quickbite.quickbite.order.dto;

import com.quickbite.quickbite.payment.model.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record PlaceOrderRequest(
        @NotNull UUID addressId,
        @NotNull PaymentMethod paymentMethod,
        @DecimalMin("0.00") BigDecimal tipAmount
) {
}
