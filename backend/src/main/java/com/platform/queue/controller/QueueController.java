package com.platform.queue.controller;

import com.platform.queue.domain.QueueEntryStatus;
import com.platform.queue.dto.CreateQueueEntryRequest;
import com.platform.queue.dto.QueueEntryDto;
import com.platform.queue.service.QueueService;
import com.platform.common.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/businesses/{businessId}/queue")
public class QueueController {

    private final QueueService queueService;

    public QueueController(QueueService queueService) {
        this.queueService = queueService;
    }

    /** Customer-facing join — any authenticated user acting as a customer, not membership-gated. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    public QueueEntryDto join(@PathVariable UUID businessId, @Valid @RequestBody CreateQueueEntryRequest request) {
        return queueService.join(businessId, CurrentUser.id(), request);
    }

    @GetMapping
    @PreAuthorize("@businessAccessService.hasRole(#businessId, authentication, 'STAFF')")
    public List<QueueEntryDto> list(@PathVariable UUID businessId,
                                     @RequestParam(required = false) QueueEntryStatus status) {
        return queueService.listForBusiness(businessId, status);
    }

    @PostMapping("/{id}/call")
    @PreAuthorize("@businessAccessService.hasRole(#businessId, authentication, 'STAFF')")
    public QueueEntryDto call(@PathVariable UUID businessId, @PathVariable UUID id) {
        return queueService.call(businessId, id);
    }

    @PostMapping("/{id}/start")
    @PreAuthorize("@businessAccessService.hasRole(#businessId, authentication, 'STAFF')")
    public QueueEntryDto start(@PathVariable UUID businessId, @PathVariable UUID id) {
        return queueService.start(businessId, id);
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("@businessAccessService.hasRole(#businessId, authentication, 'STAFF')")
    public QueueEntryDto complete(@PathVariable UUID businessId, @PathVariable UUID id) {
        return queueService.complete(businessId, id);
    }

    @PostMapping("/{id}/skip")
    @PreAuthorize("@businessAccessService.hasRole(#businessId, authentication, 'STAFF')")
    public QueueEntryDto skip(@PathVariable UUID businessId, @PathVariable UUID id) {
        return queueService.skip(businessId, id);
    }

    @PostMapping("/{id}/no-show")
    @PreAuthorize("@businessAccessService.hasRole(#businessId, authentication, 'STAFF')")
    public QueueEntryDto noShow(@PathVariable UUID businessId, @PathVariable UUID id) {
        return queueService.noShow(businessId, id);
    }

    /** OWNER/STAFF or the owning customer may cancel — the owning-customer case is handled via /me/queue-entries. */
    @PostMapping("/{id}/cancel")
    @PreAuthorize("@businessAccessService.hasRole(#businessId, authentication, 'STAFF')")
    public QueueEntryDto cancel(@PathVariable UUID businessId, @PathVariable UUID id) {
        return queueService.cancelByBusiness(businessId, id);
    }
}
