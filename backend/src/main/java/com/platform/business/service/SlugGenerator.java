package com.platform.business.service;

import com.platform.business.repository.BusinessRepository;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Generates a URL-safe, unique slug from a business name (API_CONTRACT.md
 * validation summary). Collisions are resolved with a numeric suffix rather
 * than failing, since slug is normally system-generated on create.
 */
@Component
public class SlugGenerator {

    private static final Pattern NON_ALNUM = Pattern.compile("[^a-z0-9]+");
    private static final Pattern EDGE_DASHES = Pattern.compile("^-+|-+$");

    private final BusinessRepository businessRepository;

    public SlugGenerator(BusinessRepository businessRepository) {
        this.businessRepository = businessRepository;
    }

    public String slugify(String input) {
        String base = NON_ALNUM.matcher(input.toLowerCase()).replaceAll("-");
        base = EDGE_DASHES.matcher(base).replaceAll("");
        return base.isBlank() ? "business" : base;
    }

    public String generateUnique(String candidateBase) {
        String base = slugify(candidateBase);
        String candidate = base;
        int suffix = 2;
        while (businessRepository.existsBySlug(candidate)) {
            candidate = base + "-" + suffix;
            suffix++;
        }
        return candidate;
    }
}
