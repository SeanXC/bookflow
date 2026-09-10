ALTER TABLE tenants
    ADD COLUMN slug VARCHAR(80),
    ADD COLUMN description VARCHAR(500),
    ADD COLUMN public_booking_enabled BOOLEAN NOT NULL DEFAULT TRUE;

UPDATE tenants
SET slug = 'tenant-' || id
WHERE slug IS NULL;

ALTER TABLE tenants
    ALTER COLUMN slug SET NOT NULL;

ALTER TABLE tenants
    ADD CONSTRAINT uq_tenants_slug UNIQUE (slug),
    ADD CONSTRAINT chk_tenants_slug
        CHECK (slug ~ '^[a-z0-9]+(-[a-z0-9]+)*$');
