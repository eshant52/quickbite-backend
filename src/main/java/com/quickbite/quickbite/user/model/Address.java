package com.quickbite.quickbite.user.model;

import com.quickbite.quickbite.common.model.Base;
import jakarta.persistence.*;
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

    @Column(length = 50, nullable = false)
    private String label;

    @Column(length = 20)
    private String houseNumber;

    @Column(length = 100)
    private String buildingName;

    @Column(length = 150, nullable = false)
    private String street;

    @Column(length = 100)
    private String landmark;

    @Column(length = 50, nullable = false)
    private String city;

    @Column(length = 50, nullable = false)
    private String state;

    @Column(length = 50, nullable = false)
    private String country;

    @Column(length = 10)
    private String postalCode;

    @Column(columnDefinition = "GEOMETRY(POINT, 4326)")
    @JdbcTypeCode(SqlTypes.GEOMETRY)
    private Point location;

    private Boolean isDefault;
}
