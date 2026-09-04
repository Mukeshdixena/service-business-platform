-- Scheduled classes and their enrollments (CLAUDE_CODE.md §20).
-- Table is named class_session rather than "class" because CLASS is awkward to
-- quote across tooling and the Java entity cannot sensibly be named Class
-- (it would shadow java.lang.Class) — see ARCHITECTURE.md.
--
-- enrolled_count is intentionally NOT a column: it is always derived as a
-- COUNT of class_enrollment rows in ENROLLED status (API_CONTRACT.md ClassDto).

CREATE TABLE class_session (
    id             UUID PRIMARY KEY,
    business_id    UUID NOT NULL REFERENCES business (id),
    name           VARCHAR(255) NOT NULL,
    description    TEXT,
    staff_id       UUID REFERENCES staff_member (id),
    start_at       TIMESTAMPTZ NOT NULL,
    end_at         TIMESTAMPTZ NOT NULL,
    capacity       INTEGER NOT NULL,
    status         VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    created_at     TIMESTAMPTZ NOT NULL,
    updated_at     TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_class_session_end_after_start CHECK (end_at > start_at),
    CONSTRAINT chk_class_session_capacity_positive CHECK (capacity > 0)
);

CREATE INDEX idx_class_session_business_start ON class_session (business_id, start_at);
CREATE INDEX idx_class_session_business_status ON class_session (business_id, status);

CREATE TABLE class_enrollment (
    id             UUID PRIMARY KEY,
    class_id       UUID NOT NULL REFERENCES class_session (id),
    customer_id    UUID NOT NULL REFERENCES customer_profile (id),
    status         VARCHAR(20) NOT NULL DEFAULT 'ENROLLED',
    created_at     TIMESTAMPTZ NOT NULL,
    updated_at     TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_class_enrollment_class_status ON class_enrollment (class_id, status);
CREATE INDEX idx_class_enrollment_customer_id ON class_enrollment (customer_id);

-- At most one non-cancelled enrollment per (class, customer). CANCELLED rows are
-- excluded so a customer who cancels can re-enroll later.
CREATE UNIQUE INDEX uq_class_enrollment_active_per_customer
    ON class_enrollment (class_id, customer_id)
    WHERE status <> 'CANCELLED';
