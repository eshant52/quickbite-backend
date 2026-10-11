package com.quickbite.quickbite.menu.repository;

import com.quickbite.quickbite.menu.model.CuisineRequest;
import com.quickbite.quickbite.menu.model.CuisineStatus;
import com.quickbite.quickbite.user.model.User;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CuisineRequestRepository extends JpaRepository<CuisineRequest, UUID> {

    boolean existsByNameIgnoreCaseAndStatus(String name, CuisineStatus status);

    Page<CuisineRequest> findByRequestedBy(User requestedBy, Pageable pageable);

    List<CuisineRequest> findByStatusOrderByIdAsc(CuisineStatus status, Limit limit);

    List<CuisineRequest> findByStatusAndIdGreaterThanOrderByIdAsc(CuisineStatus status, UUID cursor, Limit limit);

    List<CuisineRequest> findAllByOrderByIdAsc(Limit limit);

    List<CuisineRequest> findByIdGreaterThanOrderByIdAsc(UUID cursor, Limit limit);

    default List<CuisineRequest> findWithCursor(
            UUID cursor,
            CuisineStatus status,
            Limit limit
    ) {
        if (status != null) {
            return cursor == null
                    ? findByStatusOrderByIdAsc(status, limit)
                    : findByStatusAndIdGreaterThanOrderByIdAsc(status, cursor, limit);
        } else {
            return cursor == null
                    ? findAllByOrderByIdAsc(limit)
                    : findByIdGreaterThanOrderByIdAsc(cursor, limit);
        }
    }
}
