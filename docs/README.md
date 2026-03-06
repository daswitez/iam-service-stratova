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

### 🔌 `api/`
Documentación de la API REST:
- Especificaciones OpenAPI
- Ejemplos de requests/responses
- Guías de integración

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
