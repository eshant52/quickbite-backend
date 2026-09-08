package com.quickbite.quickbite.user.service;

import com.quickbite.quickbite.common.exception.ResourceNotFoundException;
import com.quickbite.quickbite.user.dto.AddressResponse;
import com.quickbite.quickbite.user.dto.CreateAddressRequest;
import com.quickbite.quickbite.user.model.Address;
import com.quickbite.quickbite.user.model.User;
import com.quickbite.quickbite.user.repository.AddressRepository;
import com.quickbite.quickbite.user.repository.UserRepository;
import org.jspecify.annotations.NonNull;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class UserAddressServiceImpl implements UserAddressService {

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    public UserAddressServiceImpl(UserRepository userRepository, AddressRepository addressRepository) {
        this.userRepository = userRepository;
        this.addressRepository = addressRepository;
    }

    @Override
    public AddressResponse addAddress(UUID userId, CreateAddressRequest req) {
        User user = loadUser(userId);

        int addressCount = addressRepository.countByUser(user);

        if (addressCount >= 20) {
            throw new ResourceNotFoundException("Maximum number of addresses reached");
        }

        Address address = new Address();

        boolean isDefault = addressCount == 0 || req.isDefault();
        if (addressCount > 0 && req.isDefault()) {
            addressRepository.clearDefaultForUser(userId);
        }

        Point location = (req.latitude() != null && req.longitude() != null)
                ? GEOMETRY_FACTORY.createPoint(new Coordinate(req.longitude(), req.latitude()))
                : null;

        address.setUser(user);
        return getAddressResponse(req, location, address, isDefault);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> getAddresses(UUID userId) {
        User user = loadUser(userId);
        List<Address> addresses = addressRepository.findByUser(user);
        return addresses.stream()
                .map(AddressResponse::from)
                .toList();
    }

    @Override
    public AddressResponse updateAddress(UUID userId, UUID addressId, CreateAddressRequest req) {
        Address address = loadAddressOfUser(addressId, userId);

        Point location = (req.latitude() != null && req.longitude() != null)
                ? GEOMETRY_FACTORY.createPoint(new Coordinate(req.longitude(), req.latitude()))
                : null;

        if (req.isDefault()) {
            addressRepository.clearDefaultForUser(userId);
        }

        return getAddressResponse(req, location, address, req.isDefault());
    }

    @Override
    public void deleteAddress(UUID userId, UUID addressId) {
        Address address = loadAddressOfUser(addressId, userId);
        addressRepository.delete(address);
    }

    @Override
    public AddressResponse setDefaultAddress(UUID userId, UUID addressId) {
        Address address = loadAddressOfUser(addressId, userId);
        addressRepository.clearDefaultForUser(userId);
        address.setIsDefault(true);
        return AddressResponse.from(addressRepository.save(address));
    }

    private User loadUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private Address loadAddressOfUser(UUID addressId, UUID userId) {
        User user = loadUser(userId);
        return addressRepository.findByIdAndUser(addressId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
    }

    @NonNull
    private AddressResponse getAddressResponse(CreateAddressRequest req, Point location, Address address, boolean isDefault) {
        address.setLabel(req.label());
        address.setHouseNumber(req.houseNumber());
        address.setBuildingName(req.buildingName());
        address.setStreet(req.street());
        address.setLandmark(req.landmark());
        address.setCity(req.city());
        address.setState(req.state());
        address.setCountry(req.country());
        address.setPostalCode(req.postalCode());
        address.setLocation(location);
        address.setIsDefault(isDefault);

        return AddressResponse.from(addressRepository.save(address));
    }
}
