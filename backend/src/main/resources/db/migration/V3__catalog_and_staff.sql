-- The bookable-item catalog ("service", CLAUDE_CODE.md §9) and staff (§10),
-- plus the staff<->service assignment join.

CREATE TABLE service (
    id                UUID PRIMARY KEY,
    business_id       UUID NOT NULL REFERENCES business (id),
    name              VARCHAR(255) NOT NULL,
    description       TEXT,
    price             NUMERIC(12, 2) NOT NULL,
    currency          VARCHAR(3) NOT NULL,
    duration_minutes  INTEGER,
    booking_type      VARCHAR(20) NOT NULL,
    status            VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at        TIMESTAMPTZ NOT NULL,
    updated_at        TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_service_price_non_negative CHECK (price >= 0)
);

CREATE INDEX idx_service_business_id ON service (business_id);
CREATE INDEX idx_service_business_status ON service (business_id, status);

CREATE TABLE staff_member (
    id            UUID PRIMARY KEY,
    business_id   UUID NOT NULL REFERENCES business (id),
    user_id       UUID REFERENCES app_user (id),
    display_name  VARCHAR(255) NOT NULL,
    title         VARCHAR(255),
    bio           TEXT,
    image_url     VARCHAR(1024),
    status        VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at    TIMESTAMPTZ NOT NULL,
    updated_at    TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_staff_member_business_id ON staff_member (business_id);
CREATE INDEX idx_staff_member_business_status ON staff_member (business_id, status);

-- Many-to-many: which staff can perform which services.
CREATE TABLE staff_service (
    id          UUID PRIMARY KEY,
    staff_id    UUID NOT NULL REFERENCES staff_member (id),
    service_id  UUID NOT NULL REFERENCES service (id),
    created_at  TIMESTAMPTZ NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_staff_service UNIQUE (staff_id, service_id)
);

CREATE INDEX idx_staff_service_staff_id ON staff_service (staff_id);
CREATE INDEX idx_staff_service_service_id ON staff_service (service_id);
