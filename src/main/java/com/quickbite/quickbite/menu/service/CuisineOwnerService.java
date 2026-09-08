package com.quickbite.quickbite.menu.service;

import com.quickbite.quickbite.menu.dto.CuisineRequest;
import com.quickbite.quickbite.menu.dto.CuisineRequestResponse;

import java.util.UUID;

/**
 * Service for restaurant owners to request new cuisines.
 */
public interface CuisineOwnerService {

    /**
     * Requests a new cuisine by a restaurant owner.
     */
    CuisineRequestResponse request(CuisineRequest req, UUID requesterId);
}
