package com.quickbite.quickbite.menu.service;

import com.quickbite.quickbite.menu.dto.CuisineResponse;
import com.quickbite.quickbite.menu.repository.CuisineRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CuisineCatalogServiceImpl implements CuisineCatalogService {

    private final CuisineRepository cuisineRepository;

    public CuisineCatalogServiceImpl(CuisineRepository cuisineRepository) {
        this.cuisineRepository = cuisineRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CuisineResponse> listApproved() {
        return cuisineRepository.findAll().stream()
                .map(CuisineResponse::from)
                .toList();
    }
}
