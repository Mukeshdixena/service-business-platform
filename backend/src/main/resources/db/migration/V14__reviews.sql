CREATE TABLE review (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id     UUID NOT NULL REFERENCES business(id),
    customer_profile_id UUID NOT NULL REFERENCES customer_profile(id),
    booking_id      UUID NOT NULL REFERENCES booking(id),
    rating          SMALLINT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment         TEXT,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_review_per_booking ON review (booking_id);

CREATE INDEX idx_review_business_id ON review (business_id);
CREATE INDEX idx_review_customer_id ON review (customer_profile_id);
CREATE INDEX idx_review_status ON review (status);
CREATE INDEX idx_review_business_status ON review (business_id, status);
