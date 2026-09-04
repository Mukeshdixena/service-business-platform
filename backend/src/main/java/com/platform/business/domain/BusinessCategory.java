package com.platform.business.domain;

/**
 * Describes what kind of business this is (CLAUDE_CODE.md §8). Every value from
 * the spec is defined here even though this pass only exercises a few of them,
 * since the frontend and later phases depend on the full enum existing now.
 */
public enum BusinessCategory {
    SALON,
    CLINIC,
    GYM,
    CAR_RENTAL,
    BIKE_RENTAL,
    EQUIPMENT_RENTAL,
    MECHANIC,
    SPA,
    ACADEMY,
    COWORKING,
    OTHER
}
