package com.quickbite.quickbite.user.repository;

import com.quickbite.quickbite.user.model.User;
import com.quickbite.quickbite.user.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findUserByEmail(String email);

    /** Used by the notification listener to fan-out events to all users of a given role (e.g. all admins). */
    List<User> findByRole(UserRole role);
}
