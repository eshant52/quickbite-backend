package com.quickbite.quickbite.onboarding.repository;

import com.quickbite.quickbite.onboarding.model.ApplicationDocument;
import com.quickbite.quickbite.onboarding.model.RestaurantApplication;
import com.quickbite.quickbite.restaurant.model.RestaurantDocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApplicationDocumentRepository extends JpaRepository<ApplicationDocument, UUID> {
    List<ApplicationDocument> findByApplication(RestaurantApplication application);
    Optional<ApplicationDocument> findByApplicationAndType(RestaurantApplication application, RestaurantDocumentType type);
    Optional<ApplicationDocument> findByIdAndApplication(UUID id, RestaurantApplication application);
    boolean existsByApplicationAndType(RestaurantApplication application, RestaurantDocumentType type);
}
