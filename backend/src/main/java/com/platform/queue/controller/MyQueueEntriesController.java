package com.platform.queue.controller;

import com.platform.customer.service.CustomerProfileService;
import com.platform.queue.dto.QueueEntryDto;
import com.platform.queue.dto.QueueStatusResponse;
import com.platform.queue.service.QueueService;
import com.platform.common.security.CurrentUser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** The caller can only ever see/act on their own queue entries (API_CONTRACT.md: "/me/** "). */
@RestController
@RequestMapping("/api/v1/me/queue-entries")
@PreAuthorize("isAuthenticated()")
public class MyQueueEntriesController {

    private final QueueService queueService;
    private final CustomerProfileService customerProfileService;

    public MyQueueEntriesController(QueueService queueService, CustomerProfileService customerProfileService) {
        this.queueService = queueService;
        this.customerProfileService = customerProfileService;
    }

    @GetMapping
    public List<QueueEntryDto> myQueueEntries() {
        UUID customerProfileId = customerProfileService.getOrCreateEntity(CurrentUser.id()).getId();
        return queueService.listForCustomer(customerProfileId);
    }

    @GetMapping("/{id}/status")
    public QueueStatusResponse status(@PathVariable UUID id) {
        UUID customerProfileId = customerProfileService.getOrCreateEntity(CurrentUser.id()).getId();
        return queueService.statusForCustomer(id, customerProfileId);
    }

    @PostMapping("/{id}/cancel")
    public QueueEntryDto cancel(@PathVariable UUID id) {
        UUID customerProfileId = customerProfileService.getOrCreateEntity(CurrentUser.id()).getId();
        return queueService.cancelByCustomer(id, customerProfileId);
    }
}
