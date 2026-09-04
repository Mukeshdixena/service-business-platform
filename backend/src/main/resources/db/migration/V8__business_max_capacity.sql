-- Configurable maximum capacity for a business (CLAUDE_CODE.md §19,
-- API_CONTRACT.md BusinessDto.maxCapacity). Nullable: only meaningful when the
-- CAPACITY capability is enabled, and a business that never sets it reports
-- capacity/available as null. Occupancy itself is NEVER stored here — it is
-- always derived from open attendance rows (see V9).

ALTER TABLE business ADD COLUMN max_capacity INTEGER;

ALTER TABLE business
    ADD CONSTRAINT chk_business_max_capacity_positive
        CHECK (max_capacity IS NULL OR max_capacity > 0);
