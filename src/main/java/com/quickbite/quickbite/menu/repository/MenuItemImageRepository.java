package com.quickbite.quickbite.menu.repository;

import com.quickbite.quickbite.menu.model.MenuItem;
import com.quickbite.quickbite.menu.model.MenuItemImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MenuItemImageRepository extends JpaRepository<MenuItemImage, Long> {
    Optional<MenuItemImage> findByIdAndMenuItem(UUID id, MenuItem menuItem);
    void deleteAllByMenuItem(MenuItem menuItem);
}
