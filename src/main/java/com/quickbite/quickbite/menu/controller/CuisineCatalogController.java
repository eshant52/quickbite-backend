package com.quickbite.quickbite.menu.controller;

import com.quickbite.quickbite.menu.dto.CuisineResponse;
import com.quickbite.quickbite.menu.service.CuisineCatalogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cuisines")
public class CuisineCatalogController {

    private final CuisineCatalogService cuisineCatalogService;

    public CuisineCatalogController(CuisineCatalogService cuisineCatalogService) {
        this.cuisineCatalogService = cuisineCatalogService;
    }

    @GetMapping
    public ResponseEntity<List<CuisineResponse>> getApprovedCuisines() {
        return ResponseEntity.ok(cuisineCatalogService.listApproved());
    }
}
