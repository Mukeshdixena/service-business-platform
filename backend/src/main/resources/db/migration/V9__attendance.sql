-- Gym-style attendance (CLAUDE_CODE.md §18). Attendance is deliberately
-- separate from membership: a member may hold an ACTIVE membership without
-- currently being inside the premises.
--
-- There is intentionally no "current_occupancy" counter column anywhere:
-- occupancy is always COUNT(*) over rows with check_out_at IS NULL
-- (CLAUDE_CODE.md §18-19 "Do not store occupancy as the only source of truth").

CREATE TABLE attendance (
    id             UUID PRIMARY KEY,
    business_id    UUID NOT NULL REFERENCES business (id),
    customer_id    UUID NOT NULL REFERENCES customer_profile (id),
    check_in_at    TIMESTAMPTZ NOT NULL,
    check_out_at   TIMESTAMPTZ,
    created_at     TIMESTAMPTZ NOT NULL,
    updated_at     TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_attendance_check_out_after_check_in
        CHECK (check_out_at IS NULL OR check_out_at >= check_in_at)
);

-- Named explicitly per CLAUDE_CODE.md §38 ("attendance.business_id + check_out_at")
-- — this is the occupancy-count query path.
CREATE INDEX idx_attendance_business_check_out_at ON attendance (business_id, check_out_at);
CREATE INDEX idx_attendance_customer_id ON attendance (customer_id);

-- At most one OPEN attendance record per (business, customer). A partial unique
-- index enforces the rule in the database as well as in AttendanceService, so a
-- race that slipped past the application check still cannot create a duplicate.
CREATE UNIQUE INDEX uq_attendance_open_per_customer
    ON attendance (business_id, customer_id)
    WHERE check_out_at IS NULL;
