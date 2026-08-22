package com.quickbite.quickbite.user.service;

import com.quickbite.quickbite.user.dto.AddressResponse;
import com.quickbite.quickbite.user.dto.CreateAddressRequest;
import com.quickbite.quickbite.user.dto.UpdateProfileRequest;
import com.quickbite.quickbite.user.dto.UserProfileResponse;
import com.quickbite.quickbite.user.model.User;

import java.util.List;
import java.util.UUID;

public interface UserService {
    UserProfileResponse getProfile(UUID userId);
    UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest req);
    AddressResponse addAddress(UUID userId, CreateAddressRequest req);
    List<AddressResponse> getAddresses(UUID userId);
    AddressResponse updateAddress(UUID userId, UUID addressId, CreateAddressRequest req);
    void deleteAddress(UUID userId, UUID addressId);
    AddressResponse setDefaultAddress(UUID userId, UUID addressId);
}
