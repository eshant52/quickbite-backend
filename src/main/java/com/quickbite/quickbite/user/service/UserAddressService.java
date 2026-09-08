package com.quickbite.quickbite.user.service;

import com.quickbite.quickbite.user.dto.AddressResponse;
import com.quickbite.quickbite.user.dto.CreateAddressRequest;

import java.util.List;
import java.util.UUID;

/**
 * Service dedicated to customer address book management.
 */
public interface UserAddressService {

    AddressResponse addAddress(UUID userId, CreateAddressRequest req);

    List<AddressResponse> getAddresses(UUID userId);

    AddressResponse updateAddress(UUID userId, UUID addressId, CreateAddressRequest req);

    void deleteAddress(UUID userId, UUID addressId);

    AddressResponse setDefaultAddress(UUID userId, UUID addressId);
}
