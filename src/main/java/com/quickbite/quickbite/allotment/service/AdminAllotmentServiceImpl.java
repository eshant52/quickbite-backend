package com.quickbite.quickbite.allotment.service;

import com.quickbite.quickbite.allotment.exception.AllotmentAlreadyClaimedException;
import com.quickbite.quickbite.allotment.exception.AllotmentAlreadyExistsException;
import com.quickbite.quickbite.allotment.exception.AllotmentNotFoundException;
import com.quickbite.quickbite.allotment.model.AdminAllotment;
import com.quickbite.quickbite.allotment.model.AllotmentReferenceType;
import com.quickbite.quickbite.allotment.model.AllotmentStatus;
import com.quickbite.quickbite.allotment.repository.AdminAllotmentRepository;
import com.quickbite.quickbite.allotment.service.strategy.AdminSelectionStrategy;
import com.quickbite.quickbite.common.config.AllotmentProperties;
import com.quickbite.quickbite.common.exception.BadRequestException;
import com.quickbite.quickbite.common.exception.ResourceNotFoundException;
import com.quickbite.quickbite.user.model.User;
import com.quickbite.quickbite.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AdminAllotmentServiceImpl implements AdminAllotmentService {

    private static final Logger log = LoggerFactory.getLogger(AdminAllotmentServiceImpl.class);

    private final AdminAllotmentRepository allotmentRepository;
    private final UserRepository userRepository;
    private final AdminSelectionStrategy selectionStrategy;
    private final AllotmentProperties properties;

    public AdminAllotmentServiceImpl(
            AdminAllotmentRepository allotmentRepository,
            UserRepository userRepository,
            AdminSelectionStrategy selectionStrategy,
            AllotmentProperties properties) {
        this.allotmentRepository = allotmentRepository;
        this.userRepository = userRepository;
        this.selectionStrategy = selectionStrategy;
        this.properties = properties;
    }

    @Override
    @Transactional
    public List<AdminAllotment> allot(UUID referenceId, AllotmentReferenceType referenceType) {
        if (allotmentRepository.existsByReferenceId(referenceId)) {
            throw new AllotmentAlreadyExistsException("Allotment already created for reference: " + referenceId);
        }

        List<User> selectedAdmins = selectionStrategy.select(referenceType, properties.maxAssignees());
        if (selectedAdmins.isEmpty()) {
            log.warn("[ALLOTMENT] No admins available to allot for referenceId={} type={}",
                    referenceId, referenceType);
            return List.of();
        }

        Instant now = Instant.now();
        List<AdminAllotment> allotments = selectedAdmins.stream()
                .map(admin -> {
                    AdminAllotment allotment = new AdminAllotment();
                    allotment.setAdmin(admin);
                    allotment.setReferenceId(referenceId);
                    allotment.setReferenceType(referenceType);
                    allotment.setStatus(AllotmentStatus.PENDING);
                    allotment.setNotifiedAt(now);
                    return allotment;
                })
                .toList();

        return allotmentRepository.saveAll(allotments);
    }

    @Override
    @Transactional
    public AdminAllotment accept(UUID allotmentId, UUID adminId) {
        AdminAllotment allotment = allotmentRepository.findById(allotmentId)
                .orElseThrow(() -> new AllotmentNotFoundException("Allotment not found: " + allotmentId));

        if (!allotment.getAdmin().getId().equals(adminId)) {
            throw new BadRequestException("This allotment is not assigned to your account");
        }

        if (allotment.getStatus() != AllotmentStatus.PENDING) {
            throw new AllotmentAlreadyClaimedException(
                    allotment.getStatus() == AllotmentStatus.ACCEPTED
                            ? "You have already accepted this review task"
                            : "This review task has already been claimed by another administrator"
            );
        }

        Instant now = Instant.now();
        allotment.setStatus(AllotmentStatus.ACCEPTED);
        allotment.setRespondedAt(now);
        AdminAllotment saved = allotmentRepository.save(allotment);

        int declined = allotmentRepository.declineOthers(allotment.getReferenceId(), allotmentId, now);
        log.info("[ALLOTMENT] Admin {} accepted allotment {} for referenceId {}. Declined {} other candidates.",
                adminId, allotmentId, allotment.getReferenceId(), declined);

        return saved;
    }

    @Override
    @Transactional
    public void decline(UUID allotmentId, UUID adminId) {
        AdminAllotment allotment = allotmentRepository.findById(allotmentId)
                .orElseThrow(() -> new AllotmentNotFoundException("Allotment not found: " + allotmentId));

        if (!allotment.getAdmin().getId().equals(adminId)) {
            throw new BadRequestException("This allotment is not assigned to your account");
        }

        if (allotment.getStatus() != AllotmentStatus.PENDING) {
            throw new AllotmentAlreadyClaimedException("Allotment is no longer in PENDING state");
        }

        allotment.setStatus(AllotmentStatus.DECLINED);
        allotment.setRespondedAt(Instant.now());
        allotmentRepository.save(allotment);

        log.info("[ALLOTMENT] Admin {} declined allotment {}", adminId, allotmentId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminAllotment> getPendingAllotments(UUID referenceId) {
        return allotmentRepository.findByReferenceIdAndStatus(referenceId, AllotmentStatus.PENDING);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminAllotment> getMyPendingAllotments(UUID adminId) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found"));
        return allotmentRepository.findByAdminAndStatus(admin, AllotmentStatus.PENDING);
    }
}
