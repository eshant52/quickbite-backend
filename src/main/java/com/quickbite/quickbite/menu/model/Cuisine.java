package com.quickbite.quickbite.menu.model;

import com.quickbite.quickbite.common.model.Base;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Pure master catalog entity representing a global cuisine type (e.g., "Italian", "North Indian").
 * <p>
 * Does not contain transient workflow metadata (such as review remarks or status).
 * User requests to add new cuisines are handled via {@link CuisineRequest}.
 */
@Getter
@Setter
@Entity
@Table(name = "cuisines")
public class Cuisine extends Base {

    @Column(length = 100, nullable = false, unique = true)
    private String name;
}
