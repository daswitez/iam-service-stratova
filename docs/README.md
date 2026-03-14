# Documentación IAM Service

Esta carpeta contiene toda la documentación operativa y técnica del microservicio IAM Service.

## Estructura

### 📋 `prompts/`
Contiene prompts listos para usar con Cursor AI, organizados por orden de ejecución:
- **000-conventions.md**: Convenciones y reglas del proyecto (LEER PRIMERO)
- **010-bootstrap-iam-service.md**: Configuración inicial del servicio
- **020-create-role.md**: Implementación del caso de uso CreateRole
- **030-assign-permissions-to-role.md**: Implementación del caso de uso AssignPermissionsToRole
- **040-global-exception-handler.md**: Manejo global de excepciones
- **050-openapi-api-maturity.md**: Documentación OpenAPI y madurez de API
- **060-contract-testing-mockmvc.md**: Testing de contratos con MockMvc
- **070-pact-provider.md**: Testing de contratos con Pact (Provider)

**Cómo usar los prompts:**
1. Abre el archivo del prompt correspondiente
2. Copia el bloque "Prompt para Cursor"
3. Pégalo en Cursor AI
4. Revisa los archivos generados según la checklist
5. Ejecuta las validaciones indicadas

### 📚 `runbooks/`
Runbooks operativos para desarrollo y release:
- **MASTER-RUNBOOK.md**: Guía maestra con orden de ejecución de todas las fases
- **DEV-CHECKLIST.md**: Checklist diario para desarrollo
- **RELEASE-CHECKLIST.md**: Checklist para releases

### 📝 `adr/`
Architecture Decision Records (ADRs) - decisiones arquitectónicas documentadas.

### 🗺️ `plans/`
Planes de implementacion detallados:
- **001-academic-multitenant-mvp-implementation.md**: MVP academico multi-tenant con universidades, competencias y equipos
- **D-relational-model-contract-mvp.md**: contrato relacional final del MVP para ejecutar migraciones

### 📝 `changes/`
Cambios funcionales documentados por corte:
- **`changes/001-platform-admin-bootstrap-and-auth-hardening.md`**: bootstrap de `PLATFORM_ADMIN` y cierre de registro publico
- **`changes/002-academic-mvp-relational-migration-v5.md`**: cierre del schema relacional del MVP en `V5`
- **`changes/003-p0-4-real-startup-validation.md`**: evidencia de arranque real con PostgreSQL y Flyway aplicando hasta `V5`
- **`changes/004-admin-users-crud-and-dev-jwt-protection.md`**: CRUD de usuarios y cierre de seguridad local para `/api/v1/admin/**`
- **`changes/005-admin-universities-crud.md`**: CRUD de universidades administrativas
- **`changes/006-admin-sub-tenants-crud.md`**: CRUD de facultades y programas
- **`changes/007-admin-memberships-crud.md`**: CRUD administrativo de memberships
- **`changes/008-admin-module-authorization-and-error-normalization.md`**: autorizacion declarativa y contrato de error unificado
- **`changes/009-admin-module-documentation-alignment.md`**: documentacion del modulo organizacional alineada con el estado real
- **`changes/010-admin-competitions-crud.md`**: CRUD administrativo de competencias
- **`changes/011-admin-competition-tenants.md`**: gestion administrativa de tenants participantes por competencia
- **`changes/012-admin-competition-search-filters.md`**: filtros administrativos por estado, tenant host y ciclo academico en competencias
- **`changes/013-competition-staff-enrollments.md`**: enrollments de staff multiuniversidad con `originTenantId`

### 🔌 `api/`
Documentación de la API REST:
- Especificaciones OpenAPI
- Ejemplos de requests/responses
- Guías de integración
- **`api/multi-tenant-real-data-testing.md`**: Cómo probar el modelo multi-tenant actual con PostgreSQL real
- **`api/admin-users.md`**: CRUD administrativo de usuarios con ejemplos `curl`
- **`api/admin-universities.md`**: CRUD administrativo de universidades con ejemplos `curl`
- **`api/admin-sub-tenants.md`**: CRUD administrativo de facultades y programas con ejemplos `curl`
- **`api/admin-memberships.md`**: CRUD administrativo de memberships con ejemplos `curl`
- **`api/admin-competitions.md`**: CRUD administrativo de competencias y filtros por estado, tenant host y ciclo academico
- **`api/admin-competition-tenants.md`**: gestion administrativa de tenants participantes por competencia
- **`api/competition-enrollments.md`**: enrollments de staff en competencias con detalle, cambio de estado y retiro logico

## Flujo de trabajo recomendado

1. **Primera vez**: Lee `runbooks/MASTER-RUNBOOK.md` completo
2. **Antes de empezar**: Revisa `prompts/000-conventions.md`
3. **Durante desarrollo**: Sigue el orden de los prompts (010 → 020 → ...)
4. **Antes de commit**: Usa `runbooks/DEV-CHECKLIST.md`
5. **Antes de release**: Usa `runbooks/RELEASE-CHECKLIST.md`

## Principios clave

- ✅ **NO hardcodear mensajes**: Usar errorCode (keys i18n)
- ✅ **Logs estructurados**: Formato `event=... key=value`
- ✅ **Separación de capas**: API/Orchestration en iam-service, negocio en core-platform
- ✅ **Trazabilidad**: Cada cambio documentado y validado

## Contribuir

Al agregar nueva funcionalidad:
1. Crea un nuevo prompt en `prompts/` siguiendo la numeración
2. Actualiza `MASTER-RUNBOOK.md` si es necesario
3. Documenta decisiones arquitectónicas en `adr/` si aplica
