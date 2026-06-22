package com.quickbite.quickbite.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;
import org.locationtech.jts.geom.Point;

import java.util.List;

@Getter
@Setter
@Entity
public class DeliveryAgent extends Base {
    @OneToOne
    private User user;

    @ManyToOne
    private Vehicle currentVehicle;

    private boolean isAvailable;
    private Point lastLocation;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    private DeliveryAgentVerificationStatus currentStatus;

    @OneToMany(mappedBy = "owner")
    private List<VehicleOwnership> vehicles;

    @OneToMany(mappedBy = "deliveryAgent")
    private List<DeliveryAgentDocument> documents;

    @OneToMany(mappedBy = "deliveryAgent")
    private List<DeliveryAgentVerificationHistory> verificationHistory;
}
