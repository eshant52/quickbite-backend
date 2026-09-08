package com.quickbite.quickbite.user.service;

import com.quickbite.quickbite.common.exception.ResourceNotFoundException;
import com.quickbite.quickbite.user.dto.AddressResponse;
import com.quickbite.quickbite.user.dto.CreateAddressRequest;
import com.quickbite.quickbite.user.model.Address;
import com.quickbite.quickbite.user.model.User;
import com.quickbite.quickbite.user.repository.AddressRepository;
import com.quickbite.quickbite.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserAddressServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AddressRepository addressRepository;

    @InjectMocks
    private UserAddressServiceImpl userAddressService;

    private UUID userId;
    private UUID addressId;
    private User user;
    private Address address;
    private CreateAddressRequest request;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        addressId = UUID.randomUUID();

        user = new User();
        user.setId(userId);
        user.setName("Alice");

        address = new Address();
        address.setId(addressId);
        address.setUser(user);
        address.setLabel("Home");
        address.setStreet("1st Main Rd");
        address.setCity("Bengaluru");
        address.setState("Karnataka");
        address.setCountry("India");
        address.setPostalCode("560001");
        address.setIsDefault(true);

        request = new CreateAddressRequest(
                "Home",
                "12",
                "Sunshine Apt",
                "1st Main Rd",
                "Near Park",
                "Bengaluru",
                "Karnataka",
                "India",
                "560001",
                12.9716,
                77.5946,
                false
        );
    }

    @Test
    @DisplayName("addAddress - first address automatically marked as default")
    void addAddress_firstAddress_becomesDefault() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(addressRepository.countByUser(user)).thenReturn(0);
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> {
            Address a = invocation.getArgument(0);
            a.setId(addressId);
            return a;
        });

        AddressResponse response = userAddressService.addAddress(userId, request);

        assertThat(response.isDefault()).isTrue();
        verify(addressRepository).save(any(Address.class));
    }

    @Test
    @DisplayName("addAddress - subsequent address with isDefault clears previous defaults")
    void addAddress_subsequentDefault_clearsPreviousDefaults() {
        CreateAddressRequest defaultReq = new CreateAddressRequest(
                "Work", "10", "Tech Park", "Outer Ring Rd", null,
                "Bengaluru", "Karnataka", "India", "560103", 12.9352, 77.6946, true
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(addressRepository.countByUser(user)).thenReturn(1);
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> {
            Address a = invocation.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });

        AddressResponse response = userAddressService.addAddress(userId, defaultReq);

        assertThat(response.isDefault()).isTrue();
        verify(addressRepository).clearDefaultForUser(userId);
    }

    @Test
    @DisplayName("addAddress - throws when max limit of 20 addresses reached")
    void addAddress_maxLimitReached() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(addressRepository.countByUser(user)).thenReturn(20);

        assertThatThrownBy(() -> userAddressService.addAddress(userId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Maximum number of addresses reached");
    }

    @Test
    @DisplayName("getAddresses - returns list of user addresses")
    void getAddresses_success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(addressRepository.findByUser(user)).thenReturn(List.of(address));

        List<AddressResponse> result = userAddressService.getAddresses(userId);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().label()).isEqualTo("Home");
    }

    @Test
    @DisplayName("updateAddress - updates address fields and coordinates")
    void updateAddress_success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(addressRepository.findByIdAndUser(addressId, user)).thenReturn(Optional.of(address));
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AddressResponse response = userAddressService.updateAddress(userId, addressId, request);

        assertThat(response.label()).isEqualTo("Home");
        verify(addressRepository).save(address);
    }

    @Test
    @DisplayName("deleteAddress - deletes user address")
    void deleteAddress_success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(addressRepository.findByIdAndUser(addressId, user)).thenReturn(Optional.of(address));

        userAddressService.deleteAddress(userId, addressId);

        verify(addressRepository).delete(address);
    }

    @Test
    @DisplayName("setDefaultAddress - clears previous default and sets selected address as default")
    void setDefaultAddress_success() {
        address.setIsDefault(false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(addressRepository.findByIdAndUser(addressId, user)).thenReturn(Optional.of(address));
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AddressResponse response = userAddressService.setDefaultAddress(userId, addressId);

        assertThat(response.isDefault()).isTrue();
        verify(addressRepository).clearDefaultForUser(userId);
        verify(addressRepository).save(address);
    }
}
