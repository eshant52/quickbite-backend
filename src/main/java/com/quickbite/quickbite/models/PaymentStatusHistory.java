package com.quickbite.quickbite.models;

import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class PaymentStatusHistory extends Base {
    private Payment payment;
    private PaymentStatus status;
}
