package com.platform.business.repository;

import com.platform.business.domain.Business;
import com.platform.business.domain.BusinessStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface BusinessRepository extends JpaRepository<Business, UUID> {

    Optional<Business> findBySlug(String slug);

    boolean existsBySlug(String slug);

    Optional<Business> findBySlugAndStatus(String slug, BusinessStatus status);

    /**
     * Native query (rather than JPQL) because filtering by city requires joining
     * business_location, and there is deliberately no JPA @OneToMany association
     * from Business to its children (see ARCHITECTURE.md — child aggregates are
     * referenced by plain UUID foreign keys, not object graph navigation). The
     * ORDER BY is fixed in SQL and callers must pass an unsorted Pageable, since
     * Spring Data cannot safely translate a dynamic Sort onto native SQL columns.
     */
    @Query(value = """
            select distinct b.* from business b
            left join business_location loc on loc.business_id = b.id
            where b.status = :status
              and (:category is null or b.category = :category)
              and (:query is null or lower(b.name) like lower(concat('%', cast(:query as text), '%')))
              and (:city is null or lower(loc.city) = lower(cast(:city as text)))
            order by b.created_at desc
            """,
            countQuery = """
            select count(distinct b.id) from business b
            left join business_location loc on loc.business_id = b.id
            where b.status = :status
              and (:category is null or b.category = :category)
              and (:query is null or lower(b.name) like lower(concat('%', cast(:query as text), '%')))
              and (:city is null or lower(loc.city) = lower(cast(:city as text)))
            """,
            nativeQuery = true)
    Page<Business> search(@Param("status") String status,
                           @Param("category") String category,
                           @Param("query") String query,
                           @Param("city") String city,
                           Pageable pageable);
}
