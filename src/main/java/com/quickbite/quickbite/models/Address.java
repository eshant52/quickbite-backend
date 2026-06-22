package com.quickbite.quickbite.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.locationtech.jts.geom.Point;

@Getter
@Setter
@Entity
public class Address extends Base {
    @JoinColumn(nullable = false)
    @ManyToOne
    private User user;
    private String label;
    private String houseNumber;
    private String buildingName;
    private String street;
    private String landmark;
    private String city;
    private String state;
    private String country;
    private String postalCode;
    private Point location;
    private boolean isDefault;
}
