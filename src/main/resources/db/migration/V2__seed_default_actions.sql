-- Flyway Migration V2: Seed Default Actions
-- Author: Phase B Remediation (2026-02-23)
-- Description: Inserts default CRUD actions for IAM system

-- Note: Using MERGE for H2 compatibility (used in tests)
INSERT INTO iam_action (code, name, description, created_at, version)
VALUES
    ('ACT_CREATE', 'CREATE', 'Create new entities', CURRENT_TIMESTAMP, 0),
    ('ACT_READ', 'READ', 'Read/view entities', CURRENT_TIMESTAMP, 0),
    ('ACT_UPDATE', 'UPDATE', 'Update existing entities', CURRENT_TIMESTAMP, 0),
    ('ACT_DELETE', 'DELETE', 'Delete entities', CURRENT_TIMESTAMP, 0),
    ('ACT_EXECUTE', 'EXECUTE', 'Execute operations/actions', CURRENT_TIMESTAMP, 0)
ON CONFLICT (name) DO NOTHING;

