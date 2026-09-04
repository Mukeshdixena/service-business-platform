package com.platform.business.controller;

import com.platform.business.dto.BusinessPublicDto;
import com.platform.business.dto.CategoryOptionDto;
import com.platform.business.service.DiscoveryService;
import com.platform.common.pagination.PageResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Fully public — no authentication required (see SecurityConfig's permitAll
 * on /api/v1/discovery/**). Only ever surfaces ACTIVE businesses.
 */
@RestController
@RequestMapping("/api/v1/discovery")
public class DiscoveryController {

    private final DiscoveryService discoveryService;

    public DiscoveryController(DiscoveryService discoveryService) {
        this.discoveryService = discoveryService;
    }

    @GetMapping("/categories")
    public List<CategoryOptionDto> categories() {
        return discoveryService.listCategories();
    }

    @GetMapping("/businesses")
    public PageResponse<BusinessPublicDto> search(@RequestParam(required = false) String query,
                                                    @RequestParam(required = false) String category,
                                                    @RequestParam(required = false) String city,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        int cappedSize = Math.min(Math.max(size, 1), 100);
        return discoveryService.search(query, category, city, PageRequest.of(Math.max(page, 0), cappedSize));
    }

    @GetMapping("/businesses/{slug}")
    public BusinessPublicDto getBySlug(@PathVariable String slug) {
        return discoveryService.getBySlug(slug);
    }
}
