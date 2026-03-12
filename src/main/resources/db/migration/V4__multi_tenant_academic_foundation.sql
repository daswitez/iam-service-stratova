CREATE TABLE IF NOT EXISTS iam_tenant (
    id UUID PRIMARY KEY,
    code VARCHAR(80) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(32) NOT NULL,
    parent_tenant_id UUID REFERENCES iam_tenant(id) ON DELETE RESTRICT,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_iam_tenant_parent ON iam_tenant(parent_tenant_id);

CREATE TABLE IF NOT EXISTS iam_user_tenant_membership (
    id UUID PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES iam_user(id) ON DELETE CASCADE,
    tenant_id UUID NOT NULL REFERENCES iam_tenant(id) ON DELETE CASCADE,
    membership_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_iam_user_tenant_membership UNIQUE (user_id, tenant_id)
);

CREATE INDEX IF NOT EXISTS idx_iam_user_tenant_membership_user
    ON iam_user_tenant_membership(user_id);
CREATE INDEX IF NOT EXISTS idx_iam_user_tenant_membership_tenant
    ON iam_user_tenant_membership(tenant_id);

CREATE TABLE IF NOT EXISTS iam_academic_cycle (
    id UUID PRIMARY KEY,
    code VARCHAR(80) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    owner_tenant_id UUID NOT NULL REFERENCES iam_tenant(id) ON DELETE CASCADE,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS iam_competition (
    id UUID PRIMARY KEY,
    code VARCHAR(80) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    scope VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    owner_tenant_id UUID NOT NULL REFERENCES iam_tenant(id) ON DELETE CASCADE,
    academic_cycle_id UUID REFERENCES iam_academic_cycle(id) ON DELETE SET NULL,
    starts_at TIMESTAMP WITH TIME ZONE,
    ends_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS iam_competition_tenant (
    id UUID PRIMARY KEY,
    competition_id UUID NOT NULL REFERENCES iam_competition(id) ON DELETE CASCADE,
    tenant_id UUID NOT NULL REFERENCES iam_tenant(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_iam_competition_tenant UNIQUE (competition_id, tenant_id)
);

CREATE TABLE IF NOT EXISTS iam_team (
    id UUID PRIMARY KEY,
    competition_id UUID NOT NULL REFERENCES iam_competition(id) ON DELETE CASCADE,
    origin_tenant_id UUID NOT NULL REFERENCES iam_tenant(id) ON DELETE CASCADE,
    code VARCHAR(80) NOT NULL,
    name VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_iam_team_competition_code UNIQUE (competition_id, code)
);

CREATE INDEX IF NOT EXISTS idx_iam_team_competition ON iam_team(competition_id);

CREATE TABLE IF NOT EXISTS iam_team_member (
    id UUID PRIMARY KEY,
    team_id UUID NOT NULL REFERENCES iam_team(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES iam_user(id) ON DELETE CASCADE,
    member_role VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_iam_team_member UNIQUE (team_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_iam_team_member_user ON iam_team_member(user_id);
