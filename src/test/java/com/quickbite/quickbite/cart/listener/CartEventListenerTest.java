package com.quickbite.quickbite.cart.listener;

import com.quickbite.quickbite.cart.service.CartService;
import com.quickbite.quickbite.common.event.order.OrderPlacedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CartEventListenerTest {

    @Mock
    private CartService cartService;

    @InjectMocks
    private CartEventListener cartEventListener;

    @Test
    @DisplayName("onOrderPlaced clears the customer cart")
    void onOrderPlaced_clearsCart() {
        UUID customerId = UUID.randomUUID();
        OrderPlacedEvent event = new OrderPlacedEvent(
                UUID.randomUUID(),
                customerId,
                "Customer Name",
                "customer@example.com",
                UUID.randomUUID(),
                "Restaurant Name",
                BigDecimal.valueOf(250.00),
                Instant.now()
        );

        cartEventListener.onOrderPlaced(event);

        verify(cartService).clearCart(customerId);
    }
}
