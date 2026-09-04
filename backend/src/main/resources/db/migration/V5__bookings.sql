-- Appointment bookings and the explicit state machine (CLAUDE_CODE.md §14, §33,
-- API_CONTRACT.md booking state machine). endAt/price/currency are always
-- computed server-side from the Service at creation time — there is no
-- application code path that accepts them from a client.

CREATE TABLE booking (
    id             UUID PRIMARY KEY,
    business_id    UUID NOT NULL REFERENCES business (id),
    customer_id    UUID NOT NULL REFERENCES customer_profile (id),
    service_id     UUID NOT NULL REFERENCES service (id),
    staff_id       UUID REFERENCES staff_member (id),
    start_at       TIMESTAMPTZ NOT NULL,
    end_at         TIMESTAMPTZ NOT NULL,
    status         VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    price          NUMERIC(12, 2) NOT NULL,
    currency       VARCHAR(3) NOT NULL,
    notes          TEXT,
    cancel_reason  VARCHAR(1024),
    created_at     TIMESTAMPTZ NOT NULL,
    updated_at     TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_booking_end_after_start CHECK (end_at > start_at)
);

-- Named explicitly per CLAUDE_CODE.md §38 for the "list bookings in a business's
-- time range" query path (owner dashboards, availability day-window lookups).
CREATE INDEX idx_booking_business_start ON booking (business_id, start_at);
CREATE INDEX idx_booking_business_status ON booking (business_id, status);
CREATE INDEX idx_booking_customer_id ON booking (customer_id);
CREATE INDEX idx_booking_staff_id ON booking (staff_id);
