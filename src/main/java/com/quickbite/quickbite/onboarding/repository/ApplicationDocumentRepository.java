package com.quickbite.quickbite.onboarding.repository;

import com.quickbite.quickbite.onboarding.model.restaurant.RestaurantApplicationDocument;
import com.quickbite.quickbite.onboarding.model.restaurant.RestaurantApplication;
import com.quickbite.quickbite.restaurant.model.RestaurantDocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApplicationDocumentRepository extends JpaRepository<RestaurantApplicationDocument, UUID> {
    List<RestaurantApplicationDocument> findByApplication(RestaurantApplication application);
    Optional<RestaurantApplicationDocument> findByApplicationAndType(RestaurantApplication application, RestaurantDocumentType type);
    Optional<RestaurantApplicationDocument> findByIdAndApplication(UUID id, RestaurantApplication application);
    boolean existsByApplicationAndType(RestaurantApplication application, RestaurantDocumentType type);

    void deleteByApplicationAndType(RestaurantApplication application, RestaurantDocumentType type);
}
