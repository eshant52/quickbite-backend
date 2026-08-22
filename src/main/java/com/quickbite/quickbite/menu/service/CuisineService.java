package com.quickbite.quickbite.menu.service;

import com.quickbite.quickbite.common.dto.CursorPage;
import com.quickbite.quickbite.menu.dto.CuisineRequest;
import com.quickbite.quickbite.menu.dto.CuisineResponse;
import com.quickbite.quickbite.menu.model.CuisineStatus;

import java.util.List;
import java.util.UUID;

public interface CuisineService {
    // Owner
    CuisineResponse request(CuisineRequest req, UUID requesterId);

    // Admin
    CursorPage<CuisineResponse> listByStatus(CuisineStatus status, UUID cursor, int size);
    CuisineResponse approve(UUID cuisineId, UUID adminId);
    CuisineResponse reject(UUID cuisineId, UUID adminId, String remarks);

    // Public
    List<CuisineResponse> listApproved();
}
