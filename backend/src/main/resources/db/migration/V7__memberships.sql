-- Membership plans and purchased memberships (CLAUDE_CODE.md §17,
-- API_CONTRACT.md membership state machine). paymentId is nullable and always
-- null this phase — no payment provider is wired up yet (CLAUDE_CODE.md §22).

CREATE TABLE membership_plan (
    id             UUID PRIMARY KEY,
    business_id    UUID NOT NULL REFERENCES business (id),
    name           VARCHAR(255) NOT NULL,
    description    TEXT,
    price          NUMERIC(12, 2) NOT NULL,
    currency       VARCHAR(3) NOT NULL,
    duration       INTEGER NOT NULL,
    duration_unit  VARCHAR(10) NOT NULL,
    status         VARCHAR(10) NOT NULL DEFAULT 'ACTIVE',
    created_at     TIMESTAMPTZ NOT NULL,
    updated_at     TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_membership_plan_duration_positive CHECK (duration > 0)
);

CREATE INDEX idx_membership_plan_business_id ON membership_plan (business_id);

CREATE TABLE membership (
    id                   UUID PRIMARY KEY,
    business_id          UUID NOT NULL REFERENCES business (id),
    customer_id          UUID NOT NULL REFERENCES customer_profile (id),
    membership_plan_id   UUID NOT NULL REFERENCES membership_plan (id),
    start_date           DATE NOT NULL,
    end_date             DATE NOT NULL,
    status               VARCHAR(10) NOT NULL DEFAULT 'PENDING',
    payment_id           UUID,
    created_at           TIMESTAMPTZ NOT NULL,
    updated_at           TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_membership_end_after_start CHECK (end_date >= start_date)
);

-- Named explicitly per CLAUDE_CODE.md §38 ("membership.business_id + customer_id").
CREATE INDEX idx_membership_business_customer ON membership (business_id, customer_id);
CREATE INDEX idx_membership_business_status ON membership (business_id, status);
CREATE INDEX idx_membership_customer_id ON membership (customer_id);
