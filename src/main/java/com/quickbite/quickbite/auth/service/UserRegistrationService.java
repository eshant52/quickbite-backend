package com.quickbite.quickbite.auth.service;

import com.quickbite.quickbite.auth.dto.RegisterRequest;
import com.quickbite.quickbite.user.dto.UserResponseDto;

/**
 * Service dedicated to user registration across different personas (Customer, Delivery Partner, Restaurant).
 */
public interface UserRegistrationService {

    UserResponseDto registerCustomer(RegisterRequest registerRequest);

    UserResponseDto registerDeliveryPartner(RegisterRequest registerRequest);

    UserResponseDto registerRestaurant(RegisterRequest registerRequest);
}
