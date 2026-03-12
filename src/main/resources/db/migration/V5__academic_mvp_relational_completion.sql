-- Flyway Migration V5: Academic MVP relational completion
-- Purpose: Complete the relational schema required by the academic multi-tenant MVP
-- Strategy: Apply additive, backward-compatible changes so current code keeps running

-- ========================================
-- ORGANIZATION: tenant and memberships
-- ========================================

CREATE INDEX IF NOT EXISTS idx_iam_tenant_type_status
    ON iam_tenant(type, status);

CREATE UNIQUE INDEX IF NOT EXISTS uk_iam_user_tenant_membership_primary_active
    ON iam_user_tenant_membership(user_id)
    WHERE membership_type = 'PRIMARY' AND status = 'ACTIVE';

-- ========================================
-- ACADEMIC CYCLE
-- ========================================

ALTER TABLE iam_academic_cycle
    DROP CONSTRAINT IF EXISTS iam_academic_cycle_code_key;

ALTER TABLE iam_academic_cycle
    ADD CONSTRAINT uk_iam_academic_cycle_owner_code
        UNIQUE (owner_tenant_id, code);

ALTER TABLE iam_academic_cycle
    ADD CONSTRAINT ck_iam_academic_cycle_dates
        CHECK (start_date <= end_date);

CREATE INDEX IF NOT EXISTS idx_iam_academic_cycle_owner
    ON iam_academic_cycle(owner_tenant_id);

CREATE INDEX IF NOT EXISTS idx_iam_academic_cycle_dates
    ON iam_academic_cycle(start_date, end_date);

-- ========================================
-- COMPETITION
-- ========================================

ALTER TABLE iam_competition
    ADD COLUMN IF NOT EXISTS product_name VARCHAR(255);

ALTER TABLE iam_competition
    ADD COLUMN IF NOT EXISTS industry_code VARCHAR(80);

ALTER TABLE iam_competition
    ADD COLUMN IF NOT EXISTS industry_name VARCHAR(255);

ALTER TABLE iam_competition
    ADD COLUMN IF NOT EXISTS initial_capital_amount NUMERIC(19, 2);

ALTER TABLE iam_competition
    ADD COLUMN IF NOT EXISTS currency VARCHAR(3);

ALTER TABLE iam_competition
    ADD COLUMN IF NOT EXISTS min_team_size SMALLINT;

ALTER TABLE iam_competition
    ADD COLUMN IF NOT EXISTS max_team_size SMALLINT;

ALTER TABLE iam_competition
    ADD COLUMN IF NOT EXISTS team_creation_mode VARCHAR(32);

ALTER TABLE iam_competition
    ADD COLUMN IF NOT EXISTS role_assignment_method VARCHAR(32);

ALTER TABLE iam_competition
    ADD COLUMN IF NOT EXISTS allow_optional_coo BOOLEAN;

UPDATE iam_competition
SET product_name = COALESCE(product_name, 'Generic Product'),
    industry_code = COALESCE(industry_code, 'general'),
    industry_name = COALESCE(industry_name, 'General'),
    initial_capital_amount = COALESCE(initial_capital_amount, 100000.00),
    currency = COALESCE(currency, 'USD'),
    min_team_size = COALESCE(min_team_size, 4),
    max_team_size = COALESCE(max_team_size, 6),
    team_creation_mode = COALESCE(team_creation_mode, 'ADMIN_MANAGED'),
    role_assignment_method = COALESCE(role_assignment_method, 'ADMIN_ASSIGNMENT'),
    allow_optional_coo = COALESCE(allow_optional_coo, true);

ALTER TABLE iam_competition
    ALTER COLUMN product_name SET NOT NULL;

ALTER TABLE iam_competition
    ALTER COLUMN industry_code SET NOT NULL;

ALTER TABLE iam_competition
    ALTER COLUMN industry_name SET NOT NULL;

ALTER TABLE iam_competition
    ALTER COLUMN initial_capital_amount SET NOT NULL;

ALTER TABLE iam_competition
    ALTER COLUMN currency SET NOT NULL;

ALTER TABLE iam_competition
    ALTER COLUMN min_team_size SET NOT NULL;

ALTER TABLE iam_competition
    ALTER COLUMN min_team_size SET DEFAULT 4;

ALTER TABLE iam_competition
    ALTER COLUMN max_team_size SET NOT NULL;

ALTER TABLE iam_competition
    ALTER COLUMN max_team_size SET DEFAULT 6;

ALTER TABLE iam_competition
    ALTER COLUMN team_creation_mode SET NOT NULL;

ALTER TABLE iam_competition
    ALTER COLUMN role_assignment_method SET NOT NULL;

ALTER TABLE iam_competition
    ALTER COLUMN allow_optional_coo SET NOT NULL;

ALTER TABLE iam_competition
    ALTER COLUMN allow_optional_coo SET DEFAULT true;

ALTER TABLE iam_competition
    ADD CONSTRAINT ck_iam_competition_initial_capital
        CHECK (initial_capital_amount > 0);

ALTER TABLE iam_competition
    ADD CONSTRAINT ck_iam_competition_team_sizes
        CHECK (min_team_size >= 4 AND max_team_size <= 6 AND min_team_size <= max_team_size);

ALTER TABLE iam_competition
    ADD CONSTRAINT ck_iam_competition_dates
        CHECK (starts_at IS NULL OR ends_at IS NULL OR starts_at < ends_at);

CREATE INDEX IF NOT EXISTS idx_iam_competition_owner
    ON iam_competition(owner_tenant_id);

CREATE INDEX IF NOT EXISTS idx_iam_competition_cycle
    ON iam_competition(academic_cycle_id);

CREATE INDEX IF NOT EXISTS idx_iam_competition_status
    ON iam_competition(status);

CREATE INDEX IF NOT EXISTS idx_iam_competition_scope
    ON iam_competition(scope);

CREATE INDEX IF NOT EXISTS idx_iam_competition_tenant_competition
    ON iam_competition_tenant(competition_id);

CREATE INDEX IF NOT EXISTS idx_iam_competition_tenant_tenant
    ON iam_competition_tenant(tenant_id);

-- ========================================
-- COMPETITION ENROLLMENT
-- ========================================

CREATE TABLE IF NOT EXISTS iam_competition_enrollment (
    id UUID PRIMARY KEY,
    competition_id UUID NOT NULL REFERENCES iam_competition(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES iam_user(id) ON DELETE CASCADE,
    origin_tenant_id UUID NOT NULL REFERENCES iam_tenant(id) ON DELETE CASCADE,
    participant_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    invited_by_user_id BIGINT REFERENCES iam_user(id) ON DELETE SET NULL,
    approved_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_iam_competition_enrollment UNIQUE (competition_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_iam_competition_enrollment_competition
    ON iam_competition_enrollment(competition_id);

CREATE INDEX IF NOT EXISTS idx_iam_competition_enrollment_user
    ON iam_competition_enrollment(user_id);

CREATE INDEX IF NOT EXISTS idx_iam_competition_enrollment_origin_tenant
    ON iam_competition_enrollment(origin_tenant_id);

CREATE INDEX IF NOT EXISTS idx_iam_competition_enrollment_status
    ON iam_competition_enrollment(status);

CREATE INDEX IF NOT EXISTS idx_iam_competition_enrollment_competition_status
    ON iam_competition_enrollment(competition_id, status);

-- ========================================
-- TEAM
-- ========================================

CREATE INDEX IF NOT EXISTS idx_iam_team_origin_tenant
    ON iam_team(origin_tenant_id);

CREATE INDEX IF NOT EXISTS idx_iam_team_status
    ON iam_team(status);

-- ========================================
-- TEAM MEMBER
-- ========================================

ALTER TABLE iam_team_member
    ADD COLUMN IF NOT EXISTS competition_enrollment_id UUID;

ALTER TABLE iam_team_member
    ADD COLUMN IF NOT EXISTS team_role VARCHAR(32);

ALTER TABLE iam_team_member
    ADD COLUMN IF NOT EXISTS executive_role VARCHAR(32);

ALTER TABLE iam_team_member
    ADD COLUMN IF NOT EXISTS secondary_executive_role VARCHAR(32);

UPDATE iam_team_member
SET team_role = CASE
    WHEN team_role IS NOT NULL THEN team_role
    WHEN member_role = 'CAPTAIN' THEN 'TEAM_CAPTAIN'
    ELSE 'TEAM_MEMBER'
END;

ALTER TABLE iam_team_member
    ADD CONSTRAINT fk_iam_team_member_competition_enrollment
        FOREIGN KEY (competition_enrollment_id)
        REFERENCES iam_competition_enrollment(id)
        ON DELETE CASCADE;

CREATE INDEX IF NOT EXISTS idx_iam_team_member_team
    ON iam_team_member(team_id);

CREATE INDEX IF NOT EXISTS idx_iam_team_member_status
    ON iam_team_member(status);

CREATE INDEX IF NOT EXISTS idx_iam_team_member_enrollment
    ON iam_team_member(competition_enrollment_id);

CREATE INDEX IF NOT EXISTS idx_iam_team_member_executive_role
    ON iam_team_member(executive_role);

CREATE UNIQUE INDEX IF NOT EXISTS uk_iam_team_member_enrollment
    ON iam_team_member(competition_enrollment_id)
    WHERE competition_enrollment_id IS NOT NULL;
