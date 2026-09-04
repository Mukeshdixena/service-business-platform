-- Rental bookings reuse the existing booking table rather than introducing a
-- parallel "rental" aggregate (CLAUDE_CODE.md §14 lists resourceId on Booking).
-- A booking is keyed on a staff member (APPOINTMENT) or a resource (RENTAL);
-- the §34 overlap rule is applied identically to whichever key is present.

ALTER TABLE booking ADD COLUMN resource_id UUID REFERENCES resource (id);

CREATE INDEX idx_booking_resource_id ON booking (resource_id);
CREATE INDEX idx_booking_resource_start ON booking (resource_id, start_at);
