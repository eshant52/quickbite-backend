package com.quickbite.quickbite.menu.service;

import com.quickbite.quickbite.menu.dto.CuisineResponse;

import java.util.List;

/**
 * Public catalog service for browsing approved cuisines.
 */
public interface CuisineCatalogService {

    /**
     * Lists all approved master catalog cuisines.
     */
    List<CuisineResponse> listApproved();
}
