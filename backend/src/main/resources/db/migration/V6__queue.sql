-- Walk-in queue entries and the explicit state machine (CLAUDE_CODE.md §15-16,
-- §35, API_CONTRACT.md queue state machine). "position" is intentionally NOT a
-- column: it is always derived server-side by joined_at ordering, never stored
-- as a mutable client-facing fact.

CREATE TABLE queue_entry (
    id             UUID PRIMARY KEY,
    business_id    UUID NOT NULL REFERENCES business (id),
    customer_id    UUID NOT NULL REFERENCES customer_profile (id),
    service_id     UUID NOT NULL REFERENCES service (id),
    staff_id       UUID REFERENCES staff_member (id),
    status         VARCHAR(20) NOT NULL DEFAULT 'WAITING',
    joined_at      TIMESTAMPTZ NOT NULL,
    called_at      TIMESTAMPTZ,
    started_at     TIMESTAMPTZ,
    completed_at   TIMESTAMPTZ,
    created_at     TIMESTAMPTZ NOT NULL,
    updated_at     TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_queue_entry_business_status ON queue_entry (business_id, status);
CREATE INDEX idx_queue_entry_business_joined_at ON queue_entry (business_id, joined_at);
CREATE INDEX idx_queue_entry_customer_id ON queue_entry (customer_id);
