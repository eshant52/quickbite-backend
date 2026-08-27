package com.quickbite.quickbite.menu.repository;

import com.quickbite.quickbite.menu.model.Cuisine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CuisineRepository extends JpaRepository<Cuisine, UUID> {
    boolean existsByNameIgnoreCase(String name);
    Optional<Cuisine> findByNameIgnoreCase(String name);
}
