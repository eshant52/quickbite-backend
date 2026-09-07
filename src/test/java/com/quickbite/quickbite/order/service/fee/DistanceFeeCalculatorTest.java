package com.quickbite.quickbite.order.service.fee;

import com.quickbite.quickbite.common.config.property.DeliveryFeeProperties;
import com.quickbite.quickbite.common.routing.GeoPoint;
import com.quickbite.quickbite.common.routing.RouteResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.assertj.core.api.Assertions.assertThat;

class DistanceFeeCalculatorTest {

    private DistanceFeeCalculator calculator;

    @BeforeEach
    void setUp() {
        DeliveryFeeProperties properties = new DeliveryFeeProperties(
                BigDecimal.valueOf(15.00),
                BigDecimal.valueOf(8.00),
                BigDecimal.valueOf(20.00),
                BigDecimal.valueOf(80.00)
        );
        calculator = new DistanceFeeCalculator(properties);
    }

    @Test
    @DisplayName("Adds per-km charge based on route distance")
    void calculate_addsDistanceFee() {
        // 4200 meters = 4.2 km
        // 4.2 km * 8.00 = 33.60
        FeeContext context = new FeeContext(
                GeoPoint.of(12.9716, 77.5946),
                GeoPoint.of(12.9352, 77.6245),
                new RouteResult(4200.0, 700)
        );

        BigDecimal currentFee = BigDecimal.valueOf(15.00); // base fee
        BigDecimal updatedFee = calculator.calculate(context, currentFee);

        assertThat(updatedFee).isEqualTo(BigDecimal.valueOf(48.60).setScale(2, RoundingMode.HALF_UP));
    }
}
