package com.quickbite.quickbite.vehicle.exception;

import com.quickbite.quickbite.common.exception.ResourceNotFoundException;

public class VehicleOwnershipNotFoundException extends ResourceNotFoundException {
    public VehicleOwnershipNotFoundException(String message) {
        super(message);
    }
}
