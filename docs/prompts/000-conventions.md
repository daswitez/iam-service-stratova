# Convenciones del Proyecto IAM Service

Este documento define las convenciones obligatorias que deben seguirse en todo el desarrollo del microservicio.

## 📦 Naming Conventions

### Paquetes
```
com.solveria.iamservice
├── api/                    # Capa de presentación (REST controllers, DTOs, exception handlers)
│   ├── rest/              # Controllers REST
│   ├── exception/         # Exception handlers y DTOs de error
│   └── dto/               # DTOs de API (Request/Response)
├── application/           # Capa de aplicación (orchestration, DTOs internos)
│   ├── orchestration/     # Orchestrators (coordinan use cases)
│   ├── dto/               # DTOs internos (Request/Response de aplicación)
│   └── exception/         # Excepciones específicas del servicio
├── config/                # Configuraciones Spring (@Configuration)
└── IamServiceApplication  # Clase principal Spring Boot
```

### Clases

**Controllers:**
- Sufijo: `*Controller`
- Ejemplo: `RoleController`, `AssignPermissionsToRoleController`
- Ubicación: `api.rest.*`

**Orchestrators:**
- Sufijo: `*Orchestrator`
- Ejemplo: `CreateRoleOrchestrator`, `AssignPermissionsToRoleOrchestrator`
- Ubicación: `application.orchestration.*`

**DTOs:**
- Request: `*Request` (ej: `CreateRoleRequest`)
- Response: `*Response` (ej: `CreateRoleResponse`)
- Ubicación: `application.dto.*` (internos) o `api.rest.dto.*` (API)

**Exception Handlers:**
- Nombre: `GlobalExceptionHandler` o `GlobalRestExceptionHandler`
- Ubicación: `api.exception.*` o `api.rest.*`

**Configurations:**
- Sufijo: `*Config`
- Ejemplo: `UseCaseConfig`, `I18nConfig`, `OpenApiConfig`
- Ubicación: `config.*`

### Endpoints REST

**Patrón:**
- Base: `/api/v1/{resource}`
- Métodos HTTP: `GET`, `POST`, `PUT`, `DELETE`, `PATCH`
- Nombres en plural: `/api/v1/roles`, `/api/v1/permissions`

**Ejemplos:**
```
POST   /api/v1/roles
GET    /api/v1/roles/{id}
PUT    /api/v1/roles/{id}/permissions
DELETE /api/v1/roles/{id}
```

## 📝 Logging Conventions

### Formato Estructurado Obligatorio

**Patrón:** `event=EVENT_NAME key1=value1 key2=value2`

### Niveles de Log

- **INFO**: Operaciones exitosas, inicio/fin de casos de uso
- **WARN**: Validaciones fallidas, situaciones recuperables
- **ERROR**: Excepciones, errores no recuperables
- **DEBUG**: Información detallada para debugging (solo en desarrollo)

### Ejemplos Concretos

**Inicio de operación:**
```java
log.info("event=IAM_ROLE_CREATE_REQUEST_RECEIVED name={} description={}", 
        request.name(), request.description());
```

**Operación exitosa:**
```java
log.info("event=IAM_ROLE_CREATE_SUCCESS roleId={} name={}", 
        role.getId(), role.getName());
```

**Error con errorCode:**
```java
log.error("event=IAM_ROLE_CREATE_ERROR errorCode={} name={}", 
        e.getCode(), request.name(), e);
```

**Error genérico:**
```java
log.error("event=IAM_ROLE_CREATE_ERROR name={}", request.name(), e);
```

**Validación fallida:**
```java
log.warn("event=IAM_ROLE_CREATE_VALIDATION_FAILED field={} reason={}", 
        "name", "required");
```

### Convenciones de Nombres de Eventos

- Prefijo: `IAM_` (identificador del servicio)
- Formato: `IAM_{OPERATION}_{STATUS}`
- Estados: `REQUEST_RECEIVED`, `SUCCESS`, `ERROR`, `VALIDATION_FAILED`

**Ejemplos:**
- `IAM_ROLE_CREATE_REQUEST_RECEIVED`
- `IAM_ROLE_CREATE_SUCCESS`
- `IAM_ROLE_CREATE_ERROR`
- `IAM_ASSIGN_PERMISSIONS_REQUEST_RECEIVED`
- `IAM_ASSIGN_PERMISSIONS_SUCCESS`

## 🌍 i18n Conventions

### Error Codes

**Formato:** `UPPER_SNAKE_CASE` con prefijo contextual

**Estructura:**
- Prefijo: `IAM_` (servicio) o genérico (`VALIDATION_ERROR`, `UNEXPECTED_ERROR`)
- Contexto: `{RESOURCE}_{OPERATION}_{STATUS}`
- Ejemplos:
  - `IAM_ROLE_CREATE_FAILED`
  - `IAM_ROLE_NOT_FOUND`
  - `IAM_ASSIGN_PERMISSIONS_FAILED`
  - `VALIDATION_ERROR`
  - `UNEXPECTED_ERROR`

**Ubicación:**
- Constantes: `api.exception.ErrorCodes`
- Mensajes: `resources/i18n/messages_{locale}.properties`

### Validation Keys

**Formato:** `validation.{resource}.{field}.{rule}`

**Ejemplos:**
```
validation.role.name.required=El nombre del rol es obligatorio
validation.role.name.size=El nombre del rol debe tener entre {min} y {max} caracteres
validation.role.description.size=La descripción no debe exceder {max} caracteres
validation.permission.ids.required=Al menos un permiso es requerido
```

### Mensajes de Error

**Formato en properties:**
```
{ERROR_CODE}={Mensaje traducible}
{ERROR_CODE}={Mensaje con {parametro}}
```

**Ejemplos:**
```
IAM_ROLE_CREATE_FAILED=Error al crear el rol
IAM_ROLE_NOT_FOUND=No se encontró el rol con identificador {id}
error.entity.not_found=No se encontró {entity} con identificador {id}
```

**Regla crítica:** NUNCA hardcodear mensajes en código. Siempre usar errorCode y resolver con `MessageSource`.

## 🏗️ Reglas de Arquitectura

### Separación de Capas

**iam-service (Microservicio):**
- ✅ `api.*`: Controllers REST, DTOs de API, Exception Handlers
- ✅ `application.orchestration.*`: Orchestrators que coordinan use cases
- ✅ `application.dto.*`: DTOs internos (Request/Response de aplicación)
- ✅ `config.*`: Configuraciones Spring (beans, i18n, OpenAPI)
- ❌ NO contiene lógica de negocio
- ❌ NO contiene entidades de dominio
- ❌ NO contiene repositorios JPA

**core-platform (Librería):**
- ✅ `domain.model.*`: Entidades de dominio
- ✅ `application.usecase.*`: Casos de uso (lógica de negocio)
- ✅ `application.port.*`: Interfaces de puertos (repositories, external services)
- ✅ `infrastructure.persistence.*`: Adapters JPA, Repositories
- ❌ NO contiene controllers REST
- ❌ NO contiene DTOs de API

### Flujo de Datos

```
HTTP Request
    ↓
Controller (api.rest.*)
    ↓
Orchestrator (application.orchestration.*)
    ↓
UseCase (core-platform)
    ↓
Domain Model (core-platform)
    ↓
Repository Port (core-platform)
    ↓
Repository Adapter (core-platform)
    ↓
Database
```

### DTOs

**DTOs de API (`api.rest.dto.*`):**
- Expuestos en la API REST
- Pueden tener validaciones JSR-303
- Mapean a/desde DTOs de aplicación

**DTOs de Aplicación (`application.dto.*`):**
- Internos al servicio
- Mapean a/desde Commands del core-platform
- No expuestos directamente en API

**Commands (`core-platform`):**
- Objetos inmutables (records)
- Ubicados en `core.iam.application.command.*`
- Usados por UseCases

### Exception Handling

**Jerarquía:**
1. `SolverException` (core-platform): Excepciones de dominio/negocio
2. `IamServiceException` (iam-service): Excepciones específicas del servicio
3. `GlobalRestExceptionHandler`: Maneja todas las excepciones, resuelve i18n

**Regla:** Siempre propagar `SolverException` del core sin envolver, solo loguear.

## ✅ Definition of Done (DoD)

Para cada caso de uso, se debe cumplir:

### 1. Implementación
- [ ] Controller REST con validaciones JSR-303
- [ ] Orchestrator que coordina el UseCase
- [ ] DTOs de Request/Response (API y aplicación)
- [ ] Mapeo correcto entre capas

### 2. Manejo de Errores
- [ ] ErrorCodes definidos en `ErrorCodes.java`
- [ ] Mensajes i18n en los 3 idiomas (es, en, pt)
- [ ] Logs estructurados con formato `event=...`
- [ ] Exception handler maneja todos los casos

### 3. Testing
- [ ] Tests unitarios del Orchestrator
- [ ] Tests de integración con MockMvc
- [ ] Tests de contrato (Contract Testing)
- [ ] Cobertura mínima: 80%

### 4. Documentación
- [ ] OpenAPI/Swagger documentado
- [ ] Ejemplos de request/response
- [ ] Códigos de error documentados

### 5. Validación
- [ ] Compila sin errores
- [ ] Tests pasan: `mvn test`
- [ ] Aplicación arranca: `mvn spring-boot:run`
- [ ] Endpoint funciona: curl/Postman
- [ ] Logs en formato correcto
- [ ] Mensajes i18n funcionan

## 🚫 Pitfalls Comunes

1. **Hardcodear mensajes**: ❌ `throw new Exception("Error al crear rol")` → ✅ `throw new SolverException("IAM_ROLE_CREATE_FAILED")`
2. **Logs sin estructura**: ❌ `log.info("Creating role")` → ✅ `log.info("event=IAM_ROLE_CREATE_REQUEST_RECEIVED name={}", name)`
3. **Lógica de negocio en Controller/Orchestrator**: ❌ Validar reglas de negocio aquí → ✅ Delegar a UseCase
4. **DTOs de API exponiendo entidades de dominio**: ❌ Retornar `Role` directamente → ✅ Retornar `RoleResponse` DTO
5. **Exception handling sin i18n**: ❌ Mensaje hardcodeado en exception → ✅ Usar errorCode y resolver con MessageSource
