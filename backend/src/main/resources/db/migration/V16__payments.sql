CREATE TABLE payment (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id         UUID NOT NULL REFERENCES business(id),
    customer_profile_id UUID NOT NULL REFERENCES customer_profile(id),
    reference_type      VARCHAR(50) NOT NULL,
    reference_id        UUID NOT NULL,
    amount              NUMERIC(12,2) NOT NULL CHECK (amount >= 0),
    currency            VARCHAR(3) NOT NULL DEFAULT 'INR',
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    provider            VARCHAR(50),
    provider_reference  VARCHAR(255),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_payment_business_id ON payment (business_id);
CREATE INDEX idx_payment_customer_id ON payment (customer_profile_id);
CREATE INDEX idx_payment_reference ON payment (reference_type, reference_id);
CREATE INDEX idx_payment_status ON payment (status);
