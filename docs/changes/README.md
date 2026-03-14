# Cambios Documentados

Esta carpeta registra cambios funcionales relevantes del `iam-service`, con foco en:

- que cambio
- que endpoints fueron afectados
- como probarlos con `curl`
- que documentacion relacionada se actualizo

## Indice

- `001-platform-admin-bootstrap-and-auth-hardening.md`
  - bootstrap de `PLATFORM_ADMIN`, cierre de registro publico y ajustes de auth
- `002-academic-mvp-relational-migration-v5.md`
  - migracion `V5` para completar el schema relacional del MVP academico
- `003-p0-4-real-startup-validation.md`
  - validacion real de arranque con PostgreSQL limpio, Flyway hasta `V5` y login del admin bootstrap
- `004-admin-users-crud-and-dev-jwt-protection.md`
  - CRUD de usuarios administrativos y proteccion JWT local para `/api/v1/admin/**`
- `005-admin-universities-crud.md`
  - CRUD administrativo de universidades sobre `iam_tenant`
- `006-admin-sub-tenants-crud.md`
  - CRUD administrativo de facultades y programas con jerarquia valida
- `007-admin-memberships-crud.md`
  - CRUD administrativo de memberships con primaria activa unica por usuario
- `008-admin-module-authorization-and-error-normalization.md`
  - autorizacion declarativa para endpoints admin y contrato de error unificado
- `009-admin-module-documentation-alignment.md`
  - documentacion publica del modulo organizacional alineada con users, universities, sub-tenants y memberships
- `010-admin-competitions-crud.md`
  - CRUD administrativo de competencias con reglas base del MVP academico
- `011-admin-competition-tenants.md`
  - gestion administrativa de tenants participantes en una competencia
- `012-admin-competition-search-filters.md`
  - filtros administrativos por estado, tenant host y ciclo academico para competencias
