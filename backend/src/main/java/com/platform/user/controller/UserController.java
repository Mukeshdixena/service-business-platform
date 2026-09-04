package com.platform.user.controller;

import com.platform.business.dto.BusinessMembershipDto;
import com.platform.business.service.BusinessService;
import com.platform.common.security.CurrentUser;
import com.platform.customer.dto.CustomerProfileDto;
import com.platform.customer.service.CustomerProfileService;
import com.platform.user.dto.UserDto;
import com.platform.user.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;
    private final BusinessService businessService;
    private final CustomerProfileService customerProfileService;

    public UserController(UserService userService, BusinessService businessService,
                           CustomerProfileService customerProfileService) {
        this.userService = userService;
        this.businessService = businessService;
        this.customerProfileService = customerProfileService;
    }

    @GetMapping("/me")
    public UserDto me() {
        return userService.getById(CurrentUser.id());
    }

    @GetMapping("/me/businesses")
    public List<BusinessMembershipDto> myBusinesses() {
        return businessService.getMembershipsForUser(CurrentUser.id());
    }

    @GetMapping("/me/customer-profile")
    public CustomerProfileDto myCustomerProfile() {
        return customerProfileService.getOrCreateDto(CurrentUser.id());
    }
}
