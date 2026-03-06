-- Flyway Migration V1: Initial IAM Schema
-- Author: Phase B Remediation (2026-02-23)
-- Description: Creates base IAM tables for core-platform entities

-- Note: This migration creates ONLY the schema structure.
-- The actual entities are managed by core-platform JPA entities.
-- TODO: When core-platform moves to clean architecture (ADR-001),
--       these migrations will be moved to core-platform itself.

-- ========================================
-- IAM: Modules
-- ========================================
CREATE TABLE IF NOT EXISTS iam_module (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500),
    tenant_id VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_at TIMESTAMP,
    updated_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_iam_module_tenant ON iam_module(tenant_id);

-- ========================================
-- IAM: Resources
-- ========================================
CREATE TABLE IF NOT EXISTS iam_resource (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    tenant_id VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_at TIMESTAMP,
    updated_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE(name, tenant_id)
);

CREATE INDEX idx_iam_resource_tenant ON iam_resource(tenant_id);

-- ========================================
-- IAM: Actions
-- ========================================
CREATE TABLE IF NOT EXISTS iam_action (
    id UUID PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(500),
    tenant_id VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_at TIMESTAMP,
    updated_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_iam_action_tenant ON iam_action(tenant_id);

-- ========================================
-- IAM: Fields
-- ========================================
CREATE TABLE IF NOT EXISTS iam_field (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    tenant_id VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_at TIMESTAMP,
    updated_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_iam_field_tenant ON iam_field(tenant_id);

-- ========================================
-- IAM: Roles
-- ========================================
CREATE TABLE IF NOT EXISTS iam_role (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    tenant_id VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_at TIMESTAMP,
    updated_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE(name, tenant_id)
);

CREATE INDEX idx_iam_role_tenant ON iam_role(tenant_id);

-- ========================================
-- IAM: Users
-- ========================================
CREATE TABLE IF NOT EXISTS iam_user (
    id UUID PRIMARY KEY,
    username VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    tenant_id VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_at TIMESTAMP,
    updated_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE(username, tenant_id),
    UNIQUE(email, tenant_id)
);

CREATE INDEX idx_iam_user_tenant ON iam_user(tenant_id);
CREATE INDEX idx_iam_user_email ON iam_user(email);

-- ========================================
-- IAM: Permissions
-- ========================================
CREATE TABLE IF NOT EXISTS iam_permission (
    id UUID PRIMARY KEY,
    role_id UUID NOT NULL REFERENCES iam_role(id) ON DELETE CASCADE,
    module_id UUID NOT NULL REFERENCES iam_module(id),
    resource_id UUID NOT NULL REFERENCES iam_resource(id),
    action_id UUID NOT NULL REFERENCES iam_action(id),
    field_id UUID REFERENCES iam_field(id),
    tenant_id VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_at TIMESTAMP,
    updated_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_iam_permission_role ON iam_permission(role_id);
CREATE INDEX idx_iam_permission_tenant ON iam_permission(tenant_id);

-- ========================================
-- IAM: User-Role Junction Table
-- ========================================
CREATE TABLE IF NOT EXISTS iam_user_roles (
    user_id UUID NOT NULL REFERENCES iam_user(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES iam_role(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE INDEX idx_iam_user_roles_user ON iam_user_roles(user_id);
CREATE INDEX idx_iam_user_roles_role ON iam_user_roles(role_id);

-- ========================================
-- Audit: Audit Log
-- ========================================
CREATE TABLE IF NOT EXISTS audit_log (
    id UUID PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    entity_type VARCHAR(255),
    entity_id VARCHAR(255),
    user_id VARCHAR(100),
    tenant_id VARCHAR(100),
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    details TEXT,
    correlation_id VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_at TIMESTAMP,
    updated_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_audit_log_event_type ON audit_log(event_type);
CREATE INDEX idx_audit_log_tenant ON audit_log(tenant_id);
CREATE INDEX idx_audit_log_user ON audit_log(user_id);
CREATE INDEX idx_audit_log_correlation ON audit_log(correlation_id);
CREATE INDEX idx_audit_log_timestamp ON audit_log(timestamp DESC);
