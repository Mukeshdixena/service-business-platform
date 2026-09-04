package com.platform.payment.repository;

import com.platform.payment.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByReferenceTypeAndReferenceId(String referenceType, UUID referenceId);
}
