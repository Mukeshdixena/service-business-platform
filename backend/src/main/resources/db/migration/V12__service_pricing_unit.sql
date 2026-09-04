-- Rental pricing unit (CLAUDE_CODE.md §9 "Rental services may be priced by
-- hour/day/custom period"). Nullable overall, but required by the application
-- layer for RENTAL-type services: a rental booking's total price is
-- service.price x ceil(requested duration / pricing unit).

ALTER TABLE service ADD COLUMN pricing_unit VARCHAR(10);
