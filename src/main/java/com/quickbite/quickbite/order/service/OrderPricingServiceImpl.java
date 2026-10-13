package com.quickbite.quickbite.order.service;

import com.quickbite.quickbite.cart.model.Cart;
import com.quickbite.quickbite.common.config.property.DeliveryFeeProperties;
import com.quickbite.quickbite.common.config.property.PricingProperties;
import com.quickbite.quickbite.common.dto.PriceBreakdown;
import com.quickbite.quickbite.common.routing.GeoPoint;
import com.quickbite.quickbite.common.routing.RouteResult;
import com.quickbite.quickbite.common.routing.RoutingGateway;
import com.quickbite.quickbite.order.service.fee.DeliveryFeeCalculator;
import com.quickbite.quickbite.order.service.fee.FeeContext;
import com.quickbite.quickbite.user.model.Address;
import org.locationtech.jts.geom.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.List;

@Service
public class OrderPricingServiceImpl implements OrderPricingService {

    private static final Logger log = LoggerFactory.getLogger(OrderPricingServiceImpl.class);
    private static final Duration ROUTE_CACHE_TTL = Duration.ofMinutes(5);
    private static final String ROUTE_CACHE_PREFIX = "quickbite:route:";

    private final RoutingGateway routingGateway;
    private final List<DeliveryFeeCalculator> feeCalculators;
    private final DeliveryFeeProperties feeProperties;
    private final PricingProperties pricingProperties;
    private final StringRedisTemplate stringRedisTemplate;

    @Autowired
    public OrderPricingServiceImpl(
            RoutingGateway routingGateway,
            List<DeliveryFeeCalculator> feeCalculators,
            DeliveryFeeProperties feeProperties,
            PricingProperties pricingProperties,
            @Autowired(required = false) StringRedisTemplate stringRedisTemplate) {
        this.routingGateway = routingGateway;
        this.feeCalculators = feeCalculators;
        this.feeProperties = feeProperties;
        this.pricingProperties = pricingProperties;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public PriceBreakdown calculatePrice(Cart cart, Address customerAddress, BigDecimal tipAmount) {
        Address restaurantAddress = cart.getRestaurant() != null ? cart.getRestaurant().getAddress() : null;

        // 1. Compute route (cached in Redis when available)
        RouteResult route = getOrComputeRoute(restaurantAddress, customerAddress);

        // 2. Fee calculation via Chain of Responsibility
        GeoPoint restaurantPoint = toGeoPoint(restaurantAddress != null ? restaurantAddress.getLocation() : null);
        GeoPoint customerPoint = toGeoPoint(customerAddress != null ? customerAddress.getLocation() : null);
        FeeContext feeContext = new FeeContext(restaurantPoint, customerPoint, route);

        BigDecimal computedFee = BigDecimal.ZERO;
        for (DeliveryFeeCalculator calculator : feeCalculators) {
            computedFee = calculator.calculate(feeContext, computedFee);
        }

        // 3. Apply min/max fee caps
        BigDecimal deliveryFee = computedFee.max(feeProperties.minFee())
                .min(feeProperties.maxFee())
                .setScale(2, RoundingMode.HALF_UP);

        // 4. Totals and taxes
        BigDecimal subTotal = (cart.getTotalPrice() != null ? cart.getTotalPrice() : BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal discountAmount = calculateDiscount(cart).setScale(2, RoundingMode.HALF_UP);
        BigDecimal platformFee = pricingProperties.platformFee().setScale(2, RoundingMode.HALF_UP);
        BigDecimal taxAmount = subTotal.multiply(pricingProperties.gstRate()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal tip = tipAmount != null
                ? tipAmount.setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        BigDecimal total = subTotal
                .subtract(discountAmount)
                .add(deliveryFee)
                .add(platformFee)
                .add(taxAmount)
                .add(tip)
                .setScale(2, RoundingMode.HALF_UP);

        return new PriceBreakdown(
                subTotal,
                discountAmount,
                deliveryFee,
                platformFee,
                taxAmount,
                tip,
                total,
                route.distanceMeters(),
                route.durationSeconds()
        );
    }

    /**
     * Placeholder hook for future promo/coupon discounts.
     */
    private BigDecimal calculateDiscount(Cart cart) {
        return BigDecimal.ZERO;
    }

    private RouteResult getOrComputeRoute(Address restaurantAddress, Address customerAddress) {
        if (restaurantAddress == null || restaurantAddress.getLocation() == null
                || customerAddress == null || customerAddress.getLocation() == null) {
            return new RouteResult(0.0, 0L);
        }

        String cacheKey = null;
        if (restaurantAddress.getId() != null && customerAddress.getId() != null) {
            cacheKey = ROUTE_CACHE_PREFIX + restaurantAddress.getId() + ":" + customerAddress.getId();
            if (stringRedisTemplate != null) {
                try {
                    String cached = stringRedisTemplate.opsForValue().get(cacheKey);
                    if (cached != null) {
                        String[] parts = cached.split(":");
                        if (parts.length == 2) {
                            return new RouteResult(Double.parseDouble(parts[0]), Long.parseLong(parts[1]));
                        }
                    }
                } catch (Exception ex) {
                    log.warn("Redis route cache read error, falling back to direct route calculation: {}", ex.getMessage());
                }
            }
        }

        GeoPoint from = toGeoPoint(restaurantAddress.getLocation());
        GeoPoint to = toGeoPoint(customerAddress.getLocation());
        RouteResult computed = routingGateway.route(from, to);

        if (cacheKey != null && stringRedisTemplate != null) {
            try {
                String value = computed.distanceMeters() + ":" + computed.durationSeconds();
                stringRedisTemplate.opsForValue().set(cacheKey, value, ROUTE_CACHE_TTL);
            } catch (Exception ex) {
                log.warn("Redis route cache write error: {}", ex.getMessage());
            }
        }

        return computed;
    }

    private GeoPoint toGeoPoint(Point point) {
        if (point == null) {
            return GeoPoint.of(0.0, 0.0);
        }
        return GeoPoint.of(point.getY(), point.getX()); // JTS: X=lng, Y=lat
    }
}
