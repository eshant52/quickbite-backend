package com.quickbite.quickbite.user.controller;

import com.quickbite.quickbite.auth.util.AuthenticatedSessionResolver;
import com.quickbite.quickbite.user.dto.AddressResponse;
import com.quickbite.quickbite.user.dto.CreateAddressRequest;
import com.quickbite.quickbite.user.service.UserAddressService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerAddressControllerTest {

    @Mock
    private UserAddressService userAddressService;

    @Mock
    private AuthenticatedSessionResolver authenticatedSessionResolver;

    @Mock
    private Jwt jwt;

    @InjectMocks
    private CustomerAddressController controller;

    private UUID userId;
    private UUID addressId;
    private AddressResponse mockAddress;
    private CreateAddressRequest request;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        addressId = UUID.randomUUID();
        mockAddress = new AddressResponse(
                addressId,
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
                true
        );
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
                true
        );
    }

    @Test
    @DisplayName("getAddresses - returns 200 OK with list of addresses")
    void getAddresses_success() {
        when(authenticatedSessionResolver.userIdFromJwt(jwt)).thenReturn(userId);
        when(userAddressService.getAddresses(userId)).thenReturn(List.of(mockAddress));

        ResponseEntity<List<AddressResponse>> response = controller.getAddresses(jwt);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().getFirst()).isEqualTo(mockAddress);
    }

    @Test
    @DisplayName("addAddress - returns 201 CREATED with created address")
    void addAddress_success() {
        when(authenticatedSessionResolver.userIdFromJwt(jwt)).thenReturn(userId);
        when(userAddressService.addAddress(userId, request)).thenReturn(mockAddress);

        ResponseEntity<AddressResponse> response = controller.addAddress(jwt, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(mockAddress);
    }

    @Test
    @DisplayName("updateAddress - returns 200 OK with updated address")
    void updateAddress_success() {
        when(authenticatedSessionResolver.userIdFromJwt(jwt)).thenReturn(userId);
        when(userAddressService.updateAddress(userId, addressId, request)).thenReturn(mockAddress);

        ResponseEntity<AddressResponse> response = controller.updateAddress(jwt, addressId, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(mockAddress);
    }

    @Test
    @DisplayName("deleteAddress - returns 204 NO CONTENT")
    void deleteAddress_success() {
        when(authenticatedSessionResolver.userIdFromJwt(jwt)).thenReturn(userId);

        ResponseEntity<Void> response = controller.deleteAddress(jwt, addressId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(userAddressService).deleteAddress(userId, addressId);
    }

    @Test
    @DisplayName("setDefaultAddress - returns 200 OK with new default address")
    void setDefaultAddress_success() {
        when(authenticatedSessionResolver.userIdFromJwt(jwt)).thenReturn(userId);
        when(userAddressService.setDefaultAddress(userId, addressId)).thenReturn(mockAddress);

        ResponseEntity<AddressResponse> response = controller.setDefaultAddress(jwt, addressId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(mockAddress);
    }
}
