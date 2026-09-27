package com.quickbite.quickbite.order.controller;

import com.quickbite.quickbite.order.dto.AdminRefundOrderRequest;
import com.quickbite.quickbite.order.service.AdminOrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/orders")
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    public AdminOrderController(AdminOrderService adminOrderService) {
        this.adminOrderService = adminOrderService;
    }

    @PostMapping("/{orderId}/refund")
    public ResponseEntity<Void> refundDeliveredOrder(
            @PathVariable UUID orderId,
            @Valid @RequestBody AdminRefundOrderRequest request) {
        adminOrderService.refundDeliveredOrder(orderId, request.reason());
        return ResponseEntity.noContent().build();
    }
}
