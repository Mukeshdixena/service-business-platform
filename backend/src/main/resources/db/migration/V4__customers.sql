-- Customer identity model (CLAUDE_CODE.md §21): one platform-wide
-- CustomerProfile per user, and a per-business BusinessCustomer relationship
-- row auto-created on a user's first booking at that business.

CREATE TABLE customer_profile (
    id          UUID PRIMARY KEY,
    user_id     UUID NOT NULL REFERENCES app_user (id),
    phone       VARCHAR(50),
    created_at  TIMESTAMPTZ NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_customer_profile_user_id UNIQUE (user_id)
);

CREATE TABLE business_customer (
    id                   UUID PRIMARY KEY,
    business_id          UUID NOT NULL REFERENCES business (id),
    customer_profile_id  UUID NOT NULL REFERENCES customer_profile (id),
    created_at           TIMESTAMPTZ NOT NULL,
    updated_at           TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_business_customer UNIQUE (business_id, customer_profile_id)
);

CREATE INDEX idx_business_customer_business_id ON business_customer (business_id);
CREATE INDEX idx_business_customer_customer_profile_id ON business_customer (customer_profile_id);
