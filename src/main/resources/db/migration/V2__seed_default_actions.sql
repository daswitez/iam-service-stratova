-- Flyway Migration V2: Seed Default Actions
-- Author: Phase B Remediation (2026-02-23)
-- Description: Inserts default CRUD actions for IAM system

-- Note: Using MERGE for H2 compatibility (used in tests)
-- For Postgres production, this becomes INSERT ... ON CONFLICT
MERGE INTO iam_action (name, description, created_at, version) KEY (name)
VALUES
    ('CREATE', 'Create new entities', CURRENT_TIMESTAMP, 0),
    ('READ', 'Read/view entities', CURRENT_TIMESTAMP, 0),
    ('UPDATE', 'Update existing entities', CURRENT_TIMESTAMP, 0),
    ('DELETE', 'Delete entities', CURRENT_TIMESTAMP, 0),
    ('EXECUTE', 'Execute operations/actions', CURRENT_TIMESTAMP, 0);
