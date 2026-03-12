# Contrato Relacional Final - Academic Multi-Tenant MVP

## 1. Proposito

Este documento cierra el punto `P0.2 Cerrar modelo relacional final del MVP`.

Su objetivo es dejar una fuente unica de verdad para `P0.3`, es decir, para las migraciones que completaran el esquema del MVP academico multi-tenant.

Este documento define:

- tablas finales del MVP
- columnas obligatorias
- constraints
- foreign keys
- indices
- decisiones de compatibilidad con el esquema actual

## 2. Decision de base

El modelo relacional final del MVP se apoya en cuatro capas:

1. identidad
2. organizacion
3. competencia
4. equipo

La identidad vive en `iam_user`.
La pertenencia organizacional vive en `iam_user_tenant_membership`.
La participacion en competencia vive en `iam_competition_enrollment`.
La participacion en equipo vive en `iam_team_member`.

## 3. Regla de compatibilidad

Durante este MVP:

- `iam_user.tenant_id` se mantiene por compatibilidad tecnica
- la fuente de verdad de pertenencia organizacional pasa a ser `iam_user_tenant_membership`
- el tenant `system` sigue existiendo para cuentas de plataforma como `PLATFORM_ADMIN`

Consecuencia:

- un usuario academico normal debe tener memberships activas
- un `PLATFORM_ADMIN` puede existir sin membership academica y usar `tenant_id = 'system'`

## 4. Tablas del contrato final

## 4.1 Identidad e IAM base

### `iam_user`

Estado:

- existente
- se mantiene

Columnas relevantes:

- `id BIGSERIAL PRIMARY KEY`
- `username VARCHAR(100) NOT NULL`
- `email VARCHAR(255) NOT NULL`
- `password VARCHAR(255) NOT NULL`
- `user_category VARCHAR(50) NOT NULL`
- `active BOOLEAN NOT NULL DEFAULT true`
- `tenant_id VARCHAR(100) NOT NULL`
- columnas de auditoria heredadas

Reglas:

- `email` se trata como unico a nivel operativo del servicio
- `tenant_id` no reemplaza memberships
- para usuarios de plataforma se usa `tenant_id = 'system'`

### `iam_role`

Estado:

- existente
- se mantiene

Uso en el MVP:

- `PLATFORM_ADMIN`
- luego `UNIVERSITY_ADMIN`, `PROGRAM_ADMIN` y otros roles administrativos

### `iam_user_roles`

Estado:

- existente
- se mantiene

Uso:

- asociacion usuario <-> rol administrativo

## 4.2 Organizacion

### `iam_tenant`

Estado:

- existente en `V4`
- se mantiene

Columnas:

- `id UUID PRIMARY KEY`
- `code VARCHAR(80) NOT NULL UNIQUE`
- `name VARCHAR(255) NOT NULL`
- `type VARCHAR(32) NOT NULL`
- `parent_tenant_id UUID NULL REFERENCES iam_tenant(id) ON DELETE RESTRICT`
- `status VARCHAR(32) NOT NULL`
- `created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP`

Enums:

- `type`: `UNIVERSITY`, `FACULTY`, `PROGRAM`, `HOLDING`, `ENTERPRISE`, `STARTUP`
- `status`: `ACTIVE`, `INACTIVE`, `ARCHIVED`

Indices:

- `idx_iam_tenant_parent(parent_tenant_id)`
- `idx_iam_tenant_type_status(type, status)`

### `iam_user_tenant_membership`

Estado:

- existente en `V4`
- se mantiene con un ajuste de reglas, no necesariamente de columnas

Columnas:

- `id UUID PRIMARY KEY`
- `user_id BIGINT NOT NULL REFERENCES iam_user(id) ON DELETE CASCADE`
- `tenant_id UUID NOT NULL REFERENCES iam_tenant(id) ON DELETE CASCADE`
- `membership_type VARCHAR(32) NOT NULL`
- `status VARCHAR(32) NOT NULL`
- `created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP`

Enums:

- `membership_type`: `PRIMARY`, `SECONDARY`
- `status`: `ACTIVE`, `INVITED`, `LEFT`, `SUSPENDED`

Constraints:

- `UNIQUE (user_id, tenant_id)`

Indices:

- `idx_iam_user_tenant_membership_user(user_id)`
- `idx_iam_user_tenant_membership_tenant(tenant_id)`
- indice parcial unico recomendado en Postgres:
  - un solo `PRIMARY` activo por usuario

Decision:

- no se agrega `is_primary`
- `membership_type` ya cubre esa necesidad

## 4.3 Ciclo academico

### `iam_academic_cycle`

Estado:

- existente en `V4`
- requiere ajuste de unicidad

Columnas:

- `id UUID PRIMARY KEY`
- `code VARCHAR(80) NOT NULL`
- `name VARCHAR(255) NOT NULL`
- `owner_tenant_id UUID NOT NULL REFERENCES iam_tenant(id) ON DELETE CASCADE`
- `start_date DATE NOT NULL`
- `end_date DATE NOT NULL`
- `created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP`

Constraints:

- `UNIQUE (owner_tenant_id, code)`
- `CHECK (start_date <= end_date)`

Decision:

- el `code` no debe ser globalmente unico
- el mismo codigo como `2026-S1` puede repetirse entre universidades

Indices:

- `idx_iam_academic_cycle_owner(owner_tenant_id)`
- `idx_iam_academic_cycle_dates(start_date, end_date)`

## 4.4 Competencia

### `iam_competition`

Estado:

- existente en `V4`
- requiere ampliacion estructural

Columnas finales:

- `id UUID PRIMARY KEY`
- `code VARCHAR(80) NOT NULL UNIQUE`
- `name VARCHAR(255) NOT NULL`
- `description VARCHAR(1000) NULL`
- `scope VARCHAR(32) NOT NULL`
- `status VARCHAR(32) NOT NULL`
- `owner_tenant_id UUID NOT NULL REFERENCES iam_tenant(id) ON DELETE CASCADE`
- `academic_cycle_id UUID NULL REFERENCES iam_academic_cycle(id) ON DELETE SET NULL`
- `product_name VARCHAR(255) NOT NULL`
- `industry_code VARCHAR(80) NOT NULL`
- `industry_name VARCHAR(255) NOT NULL`
- `initial_capital_amount NUMERIC(19,2) NOT NULL`
- `currency VARCHAR(3) NOT NULL`
- `min_team_size SMALLINT NOT NULL DEFAULT 4`
- `max_team_size SMALLINT NOT NULL DEFAULT 6`
- `team_creation_mode VARCHAR(32) NOT NULL`
- `role_assignment_method VARCHAR(32) NOT NULL`
- `allow_optional_coo BOOLEAN NOT NULL DEFAULT true`
- `starts_at TIMESTAMPTZ NULL`
- `ends_at TIMESTAMPTZ NULL`
- `created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP`

Enums:

- `scope`: `INTRA_TENANT`, `CROSS_TENANT`
- `status`: `DRAFT`, `PUBLISHED`, `ENROLLMENT_OPEN`, `ENROLLMENT_CLOSED`, `ACTIVE`, `FINISHED`, `ARCHIVED`
- `team_creation_mode`: `ADMIN_MANAGED`, `SELF_MANAGED`
- `role_assignment_method`: `ADMIN_ASSIGNMENT`, `DEMOCRATIC_ELECTION`, `SELF_DECLARED`

Checks:

- `initial_capital_amount > 0`
- `min_team_size >= 4`
- `max_team_size <= 6`
- `min_team_size <= max_team_size`
- `starts_at < ends_at` cuando ambos existan

Indices:

- `idx_iam_competition_owner(owner_tenant_id)`
- `idx_iam_competition_cycle(academic_cycle_id)`
- `idx_iam_competition_status(status)`
- `idx_iam_competition_scope(scope)`

### `iam_competition_tenant`

Estado:

- existente en `V4`
- se mantiene

Columnas:

- `id UUID PRIMARY KEY`
- `competition_id UUID NOT NULL REFERENCES iam_competition(id) ON DELETE CASCADE`
- `tenant_id UUID NOT NULL REFERENCES iam_tenant(id) ON DELETE CASCADE`
- `created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP`

Constraints:

- `UNIQUE (competition_id, tenant_id)`

Indices:

- `idx_iam_competition_tenant_competition(competition_id)`
- `idx_iam_competition_tenant_tenant(tenant_id)`

### `iam_competition_enrollment`

Estado:

- faltante en `V4`
- obligatoria en el contrato final

Columnas:

- `id UUID PRIMARY KEY`
- `competition_id UUID NOT NULL REFERENCES iam_competition(id) ON DELETE CASCADE`
- `user_id BIGINT NOT NULL REFERENCES iam_user(id) ON DELETE CASCADE`
- `origin_tenant_id UUID NOT NULL REFERENCES iam_tenant(id) ON DELETE CASCADE`
- `participant_type VARCHAR(32) NOT NULL`
- `status VARCHAR(32) NOT NULL`
- `invited_by_user_id BIGINT NULL REFERENCES iam_user(id) ON DELETE SET NULL`
- `approved_at TIMESTAMPTZ NULL`
- `created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP`

Enums:

- `participant_type`: `COMPETITOR`, `JUDGE`, `INVESTOR`, `MENTOR`, `MANAGER`
- `status`: `INVITED`, `APPROVED`, `REJECTED`, `WAITLISTED`, `WITHDRAWN`

Constraints:

- `UNIQUE (competition_id, user_id)`

Indices:

- `idx_iam_competition_enrollment_competition(competition_id)`
- `idx_iam_competition_enrollment_user(user_id)`
- `idx_iam_competition_enrollment_origin_tenant(origin_tenant_id)`
- `idx_iam_competition_enrollment_status(status)`
- `idx_iam_competition_enrollment_competition_status(competition_id, status)`

Decision:

- esta tabla reemplaza la ambiguedad de mandar usuarios directo a equipos
- staff y estudiantes viven aqui antes de cualquier asignacion posterior

## 4.5 Equipo

### `iam_team`

Estado:

- existente en `V4`
- requiere ampliacion de estados

Columnas:

- `id UUID PRIMARY KEY`
- `competition_id UUID NOT NULL REFERENCES iam_competition(id) ON DELETE CASCADE`
- `origin_tenant_id UUID NOT NULL REFERENCES iam_tenant(id) ON DELETE CASCADE`
- `code VARCHAR(80) NOT NULL`
- `name VARCHAR(255) NOT NULL`
- `status VARCHAR(32) NOT NULL`
- `created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP`

Enums:

- `status`: `FORMING`, `READY`, `LOCKED`, `ACTIVE`, `FINISHED`

Constraints:

- `UNIQUE (competition_id, code)`

Indices:

- `idx_iam_team_competition(competition_id)`
- `idx_iam_team_origin_tenant(origin_tenant_id)`
- `idx_iam_team_status(status)`

### `iam_team_member`

Estado:

- existe en `V4`
- requiere rediseño

Columnas finales:

- `id UUID PRIMARY KEY`
- `team_id UUID NOT NULL REFERENCES iam_team(id) ON DELETE CASCADE`
- `competition_enrollment_id UUID NOT NULL REFERENCES iam_competition_enrollment(id) ON DELETE CASCADE`
- `team_role VARCHAR(32) NOT NULL`
- `executive_role VARCHAR(32) NULL`
- `secondary_executive_role VARCHAR(32) NULL`
- `status VARCHAR(32) NOT NULL`
- `created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP`

Enums:

- `team_role`: `TEAM_CAPTAIN`, `TEAM_MEMBER`
- `status`: `ACTIVE`, `INVITED`, `LEFT`, `SUSPENDED`
- `executive_role`: `CEO`, `CPO`, `CTO`, `CMO`, `CFO`, `COO`
- `secondary_executive_role`: mismo dominio que `executive_role`

Constraints:

- `UNIQUE (competition_enrollment_id)`
- `UNIQUE (team_id, competition_enrollment_id)`
- `CHECK (executive_role IS NULL OR executive_role <> secondary_executive_role)`

Indices:

- `idx_iam_team_member_team(team_id)`
- `idx_iam_team_member_enrollment(competition_enrollment_id)`
- `idx_iam_team_member_status(status)`
- `idx_iam_team_member_executive_role(executive_role)`

Decision:

- `competition_enrollment_id` pasa a ser la FK correcta
- `user_id` deja de ser la relacion principal en esta tabla
- `executive_role` puede ser `NULL` mientras el equipo este en `FORMING`
- la app debe impedir que un equipo pase a `READY` sin cobertura ejecutiva valida

## 5. Reglas relacionales clave

## 5.1 Un usuario no entra a equipo sin enrollment

Esto queda garantizado porque `iam_team_member` depende de `iam_competition_enrollment`.

## 5.2 Un usuario no puede estar en dos equipos de la misma competencia

Esto queda garantizado con:

- `UNIQUE (competition_enrollment_id)` en `iam_team_member`

## 5.3 Una competencia puede usar staff multiuniversidad

Esto queda garantizado porque `iam_competition_enrollment.origin_tenant_id` apunta al tenant desde el cual participa el usuario.

## 5.4 El tenant organizacional no se mezcla con equipo ni competencia

Esto queda garantizado por separacion fisica de tablas:

- `iam_tenant`
- `iam_competition`
- `iam_team`

## 6. Huecos concretos respecto a `V4`

El schema actual `V4__multi_tenant_academic_foundation.sql` todavia no cumple el contrato final porque:

- no existe `iam_competition_enrollment`
- `iam_competition` no tiene producto, industria, capital ni reglas de equipo
- `iam_team_member` sigue modelado por `user_id` y no por enrollment
- `iam_team_member` no tiene roles ejecutivos
- `iam_team.status` no contempla `READY` ni `LOCKED`
- `iam_academic_cycle.code` esta demasiado globalizado si queda unico solo

## 7. Decisiones cerradas para `P0.3`

`P0.3` debe implementar exactamente estas decisiones:

1. crear `iam_competition_enrollment`
2. ampliar `iam_competition`
3. rediseñar `iam_team_member`
4. ajustar unicidad de `iam_academic_cycle`
5. ampliar enums y checks de estado
6. agregar indices operativos

## 8. Fuera del contrato de este MVP

No entran en este contrato:

- estado del motor de simulacion
- turnos
- scoring
- ranking
- pagos
- dashboards

## 9. Resultado de `P0.2`

Con este documento, el modelo relacional final del MVP queda cerrado a nivel de diseño.

El siguiente paso correcto es `P0.3 Crear migraciones complementarias`.
