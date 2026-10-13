package com.quickbite.quickbite.order.service;

import com.quickbite.quickbite.cart.service.CartCheckoutServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guard rail: these classes perform external I/O (routing provider HTTP calls, Redis) while
 * pricing a cart. They must never run inside a DB transaction, otherwise a connection is held
 * from the pool for the duration of the remote call.
 */
class NoTransactionAroundExternalCallsTest {

    private static final List<Class<?>> EXTERNAL_IO_CLASSES = List.of(
            OrderPricingServiceImpl.class,
            OrderCreationServiceImpl.class,
            CartCheckoutServiceImpl.class,
            OrderServiceImpl.class // placeOrder orchestrates TX1 -> payment gateway -> TX2
    );

    @Test
    @DisplayName("Classes doing external I/O are not class-level @Transactional")
    void noClassLevelTransactional() {
        for (Class<?> type : EXTERNAL_IO_CLASSES) {
            assertThat(type.isAnnotationPresent(Transactional.class))
                    .as("%s must not be class-level @Transactional", type.getSimpleName())
                    .isFalse();
        }
    }

    @Test
    @DisplayName("Pricing, order creation, checkout preview and placeOrder methods are not @Transactional")
    void noTransactionalOnExternalCallMethods() throws NoSuchMethodException {
        assertNotTransactional(OrderPricingServiceImpl.class.getMethod("calculatePrice",
                com.quickbite.quickbite.cart.model.Cart.class,
                com.quickbite.quickbite.user.model.Address.class,
                java.math.BigDecimal.class));
        assertNotTransactional(OrderCreationServiceImpl.class.getMethod("createOrderWithItems",
                java.util.UUID.class,
                com.quickbite.quickbite.order.dto.PlaceOrderRequest.class));
        assertNotTransactional(CartCheckoutServiceImpl.class.getMethod("getCheckoutPreview",
                java.util.UUID.class, java.util.UUID.class, java.math.BigDecimal.class));
        assertNotTransactional(OrderServiceImpl.class.getMethod("placeOrder",
                java.util.UUID.class,
                com.quickbite.quickbite.order.dto.PlaceOrderRequest.class));
    }

    private static void assertNotTransactional(Method method) {
        assertThat(method.isAnnotationPresent(Transactional.class))
                .as("%s.%s must not be @Transactional",
                        method.getDeclaringClass().getSimpleName(), method.getName())
                .isFalse();
    }
}
