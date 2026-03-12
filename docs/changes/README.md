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
