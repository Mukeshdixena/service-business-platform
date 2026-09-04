-- Business core: the tenant entity, its capabilities, locations, hours and the
-- BusinessMembership tenant-boundary join (CLAUDE_CODE.md §6-8, §12, §31, §38-39).

CREATE TABLE business (
    id                  UUID PRIMARY KEY,
    name                VARCHAR(255) NOT NULL,
    slug                VARCHAR(255) NOT NULL,
    description         TEXT,
    phone               VARCHAR(50),
    email               VARCHAR(255),
    logo_url            VARCHAR(1024),
    cover_image_url     VARCHAR(1024),
    category            VARCHAR(50) NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    verification_status VARCHAR(20) NOT NULL DEFAULT 'UNVERIFIED',
    created_at          TIMESTAMPTZ NOT NULL,
    updated_at          TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_business_slug UNIQUE (slug)
);

-- Named explicitly per CLAUDE_CODE.md §38 ("business.category_id"): category is
-- modeled as an enum column (not a separate lookup table) since API_CONTRACT.md
-- returns/accepts it as a plain string enum, but it is still indexed for the
-- discovery search/filter query path.
CREATE INDEX idx_business_category ON business (category);
CREATE INDEX idx_business_status ON business (status);

CREATE TABLE business_capability (
    business_id UUID NOT NULL REFERENCES business (id),
    capability  VARCHAR(50) NOT NULL,
    CONSTRAINT pk_business_capability PRIMARY KEY (business_id, capability)
);

CREATE TABLE business_location (
    id             UUID PRIMARY KEY,
    business_id    UUID NOT NULL REFERENCES business (id),
    label          VARCHAR(255),
    address_line1  VARCHAR(255) NOT NULL,
    address_line2  VARCHAR(255),
    city           VARCHAR(255) NOT NULL,
    state          VARCHAR(255),
    postal_code    VARCHAR(50),
    country        VARCHAR(100) NOT NULL,
    latitude       NUMERIC(9, 6),
    longitude      NUMERIC(9, 6),
    is_primary     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMPTZ NOT NULL,
    updated_at     TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_business_location_business_id ON business_location (business_id);
CREATE INDEX idx_business_location_city ON business_location (city);

-- Multiple rows per (business_id, day_of_week) are allowed on purpose
-- (CLAUDE_CODE.md §12 — e.g. a lunch closure means two intervals in one day).
CREATE TABLE business_hours (
    id          UUID PRIMARY KEY,
    business_id UUID NOT NULL REFERENCES business (id),
    day_of_week VARCHAR(10) NOT NULL,
    open_time   TIME NOT NULL,
    close_time  TIME NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_business_hours_open_before_close CHECK (open_time < close_time)
);

CREATE INDEX idx_business_hours_business_day ON business_hours (business_id, day_of_week);

-- The tenant-boundary join every authorization check goes through (§31).
CREATE TABLE business_membership (
    id          UUID PRIMARY KEY,
    business_id UUID NOT NULL REFERENCES business (id),
    user_id     UUID NOT NULL REFERENCES app_user (id),
    role        VARCHAR(20) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_business_membership_business_user UNIQUE (business_id, user_id)
);

CREATE INDEX idx_business_membership_user_id ON business_membership (user_id);
CREATE INDEX idx_business_membership_business_id ON business_membership (business_id);
