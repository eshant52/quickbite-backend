package com.quickbite.quickbite.payment.repository;

import com.quickbite.quickbite.payment.model.PaymentStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PaymentStatusHistoryRepository extends JpaRepository<PaymentStatusHistory, UUID> {
}
