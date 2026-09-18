package com.quickbite.quickbite.cart.listener;


import com.quickbite.quickbite.cart.service.CartService;
import com.quickbite.quickbite.common.event.order.OrderPlacedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class CartEventListener {

    private final CartService cartService;

    public CartEventListener(CartService cartService) {
        this.cartService = cartService;
    }

    @EventListener
    public void onOrderPlaced(OrderPlacedEvent event) {
        cartService.clearCart(event.customerId());
    }
}