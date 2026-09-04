package com.platform.business.domain;

/**
 * Describes what a business can DO (CLAUDE_CODE.md §7), kept strictly separate
 * from {@link BusinessCategory} (what it IS). Every value from the spec is
 * defined even though only APPOINTMENTS/STAFF have working logic this pass —
 * QUEUE/MEMBERSHIPS/RENTALS/CLASSES/CAPACITY/RESOURCES/PAYMENTS are reserved for
 * later phases so the enum set doesn't shift under the frontend later.
 */
public enum BusinessCapability {
    APPOINTMENTS,
    QUEUE,
    MEMBERSHIPS,
    RENTALS,
    CLASSES,
    CAPACITY,
    STAFF,
    RESOURCES,
    PAYMENTS
}
