package com.quickbite.quickbite.menu.service;

import com.quickbite.quickbite.common.dto.CursorPage;
import com.quickbite.quickbite.menu.dto.CuisineRequestResponse;
import com.quickbite.quickbite.menu.dto.CuisineResponse;
import com.quickbite.quickbite.menu.model.CuisineStatus;

import java.util.UUID;

public interface AdminCuisineService {
    CursorPage<CuisineRequestResponse> listRequestsByStatus(CuisineStatus status, UUID cursor, int size);
    CuisineResponse approve(UUID requestId, UUID adminId);
    CuisineRequestResponse reject(UUID requestId, UUID adminId, String remarks);
}
