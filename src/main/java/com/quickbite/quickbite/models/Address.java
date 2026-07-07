package com.quickbite.quickbite.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Point;

@Getter
@Setter
@Entity
@Table(name = "addresses")
public class Address extends Base {
    @JoinColumn(nullable = false)
    @ManyToOne
    private User user;

    @NotBlank(message = "Label is required")
    @Size(max = 50, message = "Label must be at most 50 characters")
    @Column(length = 50, nullable = false)
    private String label;

    @Size(max = 20, message = "House number must be at most 20 characters")
    @Column(length = 20)
    private String houseNumber;

    @Size(max = 100, message = "Building name must be at most 100 characters")
    @Column(length = 100)
    private String buildingName;

    @NotBlank(message = "Street is required")
    @Size(max = 150, message = "Street must be at most 150 characters")
    @Column(length = 150, nullable = false)
    private String street;

    @Size(max = 100, message = "Landmark must be at most 100 characters")
    @Column(length = 100)
    private String landmark;

    @NotBlank(message = "City is required")
    @Size(max = 50, message = "City must be at most 50 characters")
    @Column(length = 50, nullable = false)
    private String city;

    @NotBlank(message = "State is required")
    @Size(max = 50, message = "State must be at most 50 characters")
    @Column(length = 50, nullable = false)
    private String state;

    @NotBlank(message = "Country is required")
    @Size(max = 50, message = "Country must be at most 50 characters")
    @Column(length = 50, nullable = false)
    private String country;

    @Size(max = 10, message = "Postal code must be at most 10 characters")
    @Column(length = 10)
    private String postalCode;

    @Column(columnDefinition = "GEOMETRY(POINT, 4326)")
    @JdbcTypeCode(SqlTypes.GEOMETRY)
    private Point location;

    private Boolean isDefault;
}
