package com.quickbite.quickbite.user.repository;

import com.quickbite.quickbite.user.model.User;
import com.quickbite.quickbite.user.model.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findUserByEmail(String email);

    /**
     * Used by the notification listener to fan-out events to all users of a given role (e.g. all admins).
     *
     * @param role the role to filter users by
     * @return a list of users with the specified role
     */
    List<User> findByRole(UserRole role);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByPhoneNumber(String phoneNumber);
}
