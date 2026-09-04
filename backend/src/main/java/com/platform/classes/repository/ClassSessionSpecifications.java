package com.platform.classes.repository;

import com.platform.classes.domain.ClassSession;
import com.platform.classes.domain.ClassStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.UUID;

public final class ClassSessionSpecifications {

    private ClassSessionSpecifications() {
    }

    public static Specification<ClassSession> businessId(UUID businessId) {
        return (root, query, cb) -> cb.equal(root.get("businessId"), businessId);
    }

    public static Specification<ClassSession> status(ClassStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<ClassSession> startAtFrom(Instant from) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("startAt"), from);
    }

    public static Specification<ClassSession> startAtTo(Instant to) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("startAt"), to);
    }
}
