-- Bookable physical assets (CLAUDE_CODE.md §11): JCBs, cars, bikes, rooms,
-- treatment chairs. Deliberately a first-class entity rather than being modelled
-- as staff ("Do not model vehicles/equipment as staff").

CREATE TABLE resource (
    id             UUID PRIMARY KEY,
    business_id    UUID NOT NULL REFERENCES business (id),
    name           VARCHAR(255) NOT NULL,
    type           VARCHAR(100),
    description    TEXT,
    image_url      VARCHAR(1024),
    identifier     VARCHAR(255),
    status         VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    created_at     TIMESTAMPTZ NOT NULL,
    updated_at     TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_resource_business_id ON resource (business_id);
CREATE INDEX idx_resource_business_status ON resource (business_id, status);
