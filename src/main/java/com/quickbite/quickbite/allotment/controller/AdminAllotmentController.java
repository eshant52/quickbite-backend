package com.quickbite.quickbite.allotment.controller;

import com.quickbite.quickbite.allotment.dto.AllotmentResponse;
import com.quickbite.quickbite.allotment.model.AdminAllotment;
import com.quickbite.quickbite.allotment.service.AdminAllotmentService;
import com.quickbite.quickbite.auth.util.AuthenticatedSessionResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/allotments")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAllotmentController {

    private final AdminAllotmentService allotmentService;
    private final AuthenticatedSessionResolver sessionResolver;

    public AdminAllotmentController(
            AdminAllotmentService allotmentService,
            AuthenticatedSessionResolver sessionResolver) {
        this.allotmentService = allotmentService;
        this.sessionResolver = sessionResolver;
    }

    @GetMapping("/pending")
    public ResponseEntity<List<AllotmentResponse>> getMyPendingAllotments(
            @AuthenticationPrincipal Jwt jwt) {
        UUID adminId = sessionResolver.userIdFromJwt(jwt);
        List<AdminAllotment> pending = allotmentService.getMyPendingAllotments(adminId);
        return ResponseEntity.ok(pending.stream().map(AllotmentResponse::from).toList());
    }

    @PostMapping("/{allotmentId}/accept")
    public ResponseEntity<AllotmentResponse> acceptAllotment(
            @PathVariable UUID allotmentId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID adminId = sessionResolver.userIdFromJwt(jwt);
        AdminAllotment accepted = allotmentService.accept(allotmentId, adminId);
        return ResponseEntity.ok(AllotmentResponse.from(accepted));
    }

    @PostMapping("/{allotmentId}/decline")
    public ResponseEntity<Void> declineAllotment(
            @PathVariable UUID allotmentId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID adminId = sessionResolver.userIdFromJwt(jwt);
        allotmentService.decline(allotmentId, adminId);
        return ResponseEntity.noContent().build();
    }
}
