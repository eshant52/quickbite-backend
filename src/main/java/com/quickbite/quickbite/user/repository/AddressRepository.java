package com.quickbite.quickbite.user.repository;

import com.quickbite.quickbite.user.model.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import com.quickbite.quickbite.user.model.User;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AddressRepository extends JpaRepository<Address, UUID> {
    List<Address> findByUser(User user);
    Optional<Address> findByIdAndUser(UUID id, User user);

    @Modifying
    @Query("UPDATE Address a SET a.isDefault = false WHERE a.user.id = :userId")
    void clearDefaultForUser(@Param("userId") UUID userId);

    int countByUser(User user);
}