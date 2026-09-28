package com.quickbite.quickbite.auth.service;

import com.quickbite.quickbite.auth.dto.RegisterRequest;
import com.quickbite.quickbite.user.dto.UserResponseDto;

/**
 * Service dedicated to user registration across different personas (Customer, Delivery Agent, Restaurant).
 */
public interface UserRegistrationService {

    UserResponseDto registerCustomer(RegisterRequest registerRequest);

    UserResponseDto registerDeliveryAgent(RegisterRequest registerRequest);

    UserResponseDto registerRestaurantOwner(RegisterRequest registerRequest);
}
