package com.quickbite.quickbite.dtos.onboard;

import java.util.List;

public record OnboardRestaurantRequest(
        String restaurantName,
        String description,
        String street,
        String city,
        String state,
        String country,
        String postalCode,

        List<String> documentUrls,
        List<MenuItemDto> sampleMenu
) {
    public record MenuItemDto(String name, String description, String price) {}
}
