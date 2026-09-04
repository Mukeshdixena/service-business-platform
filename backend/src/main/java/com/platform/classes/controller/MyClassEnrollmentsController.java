package com.platform.classes.controller;

import com.platform.classes.dto.ClassEnrollmentDto;
import com.platform.classes.service.ClassService;
import com.platform.common.security.CurrentUser;
import com.platform.customer.service.CustomerProfileService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** The caller can only ever see their own enrollments (API_CONTRACT.md: "/me/** "). */
@RestController
@RequestMapping("/api/v1/me/class-enrollments")
@PreAuthorize("isAuthenticated()")
public class MyClassEnrollmentsController {

    private final ClassService classService;
    private final CustomerProfileService customerProfileService;

    public MyClassEnrollmentsController(ClassService classService, CustomerProfileService customerProfileService) {
        this.classService = classService;
        this.customerProfileService = customerProfileService;
    }

    @GetMapping
    public List<ClassEnrollmentDto> myEnrollments() {
        UUID customerProfileId = customerProfileService.getOrCreateEntity(CurrentUser.id()).getId();
        return classService.listForCustomer(customerProfileId);
    }
}
