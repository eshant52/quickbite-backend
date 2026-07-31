package com.quickbite.quickbite.onboarding.repository;

import com.quickbite.quickbite.onboarding.model.ApplicationStatus;
import com.quickbite.quickbite.onboarding.model.RestaurantApplication;
import com.quickbite.quickbite.user.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RestaurantApplicationRepository extends JpaRepository<RestaurantApplication, UUID> {

    Optional<RestaurantApplication> findByOwnerAndStatusIn(User owner, List<ApplicationStatus> statuses);

    Optional<RestaurantApplication> findByIdAndOwner(UUID id, User owner);

    Page<RestaurantApplication> findByStatus(ApplicationStatus status, Pageable pageable);

    boolean existsByOwnerAndStatusIn(User owner, List<ApplicationStatus> statuses);
}
