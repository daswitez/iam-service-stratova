# Cambio 001 - Platform Admin Bootstrap y Auth Hardening

## 1. Resumen

Este cambio cierra el registro publico y define un operador inicial controlado para el sistema.

A partir de este punto:

- ya no se permite `register` publico
- el sistema bootstrapea un `PLATFORM_ADMIN` en `dev`
- `login` sigue siendo el punto de entrada para usuarios existentes
- el contexto auth ya incluye el rol persistido `PLATFORM_ADMIN`

## 2. Objetivo del cambio

Evitar que cualquier usuario externo cree cuentas, universidades o datos operativos sin control.

La logica nueva deja al sistema con este flujo:

1. arranca el servicio
2. se crea o asegura un `PLATFORM_ADMIN`
3. ese administrador inicia sesion
4. desde ahi se construiran los futuros CRUDs administrativos

## 3. Endpoints afectados

### 3.1 `POST /api/v1/auth/register`

Estado actual:

- deshabilitado para uso publico

Comportamiento:

- responde `403 FORBIDDEN`
- no crea usuarios
- no ejecuta logica de registro

### cURL

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "juan.perez",
    "email": "juan.perez@umsa.bo",
    "password": "SecurePass123",
    "userCategory": "STUDENT",
    "tenantId": "adm-sis-umsa"
  }'
```

### Respuesta esperada

```json
{
  "errorCode": "FORBIDDEN",
  "message": "Public registration is disabled. An authenticated administrator must create users."
}
```

### 3.2 `POST /api/v1/auth/login`

Estado actual:

- activo

Comportamiento:

- autentica usuarios existentes
- devuelve JWT
- devuelve contexto auth
- si el usuario tiene roles persistidos, tambien salen en `user.roles`

### cURL con admin bootstrap

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@solveria.local",
    "password": "Admin12345!"
  }'
```

### Respuesta esperada

```json
{
  "token": "<JWT>",
  "user": {
    "id": 1,
    "username": "platform.admin",
    "email": "admin@solveria.local",
    "primaryTenantId": null,
    "userCategory": "ACADEMIC_ADMIN",
    "roles": ["ACADEMIC_ADMIN", "PLATFORM_ADMIN"]
  },
  "context": {
    "activeTenantId": null,
    "memberships": [],
    "teamCompetitions": []
  }
}
```

## 4. Bootstrap del administrador

En `dev`, el servicio usa estas propiedades:

- `bootstrap.admin.enabled=true`
- `bootstrap.admin.username=platform.admin`
- `bootstrap.admin.email=admin@solveria.local`
- `bootstrap.admin.password=Admin12345!`

Regla:

- si el rol `PLATFORM_ADMIN` no existe, se crea
- si el usuario admin no existe, se crea
- si el usuario existe pero no tiene el rol, se corrige

## 5. Archivos principales tocados

### Codigo

- `src/main/java/com/solveria/iamservice/config/security/PlatformAdminBootstrapRunner.java`
- `src/main/java/com/solveria/iamservice/config/security/PlatformAdminBootstrapProperties.java`
- `src/main/java/com/solveria/iamservice/config/security/SecurityConstants.java`
- `src/main/java/com/solveria/iamservice/api/rest/AuthController.java`
- `src/main/java/com/solveria/iamservice/application/service/UserContextService.java`
- `src/main/java/com/solveria/iamservice/IamServiceApplication.java`
- `../core-plataform/core-platform/src/main/java/com/solveria/core/iam/infrastructure/persistence/repository/RoleJpaRepository.java`

### Configuracion

- `src/main/resources/application.yml`
- `src/main/resources/application-dev.yml`

### Pruebas

- `src/test/java/com/solveria/iamservice/api/rest/AuthControllerIT.java`
- `src/test/java/com/solveria/iamservice/config/security/PlatformAdminBootstrapRunnerTest.java`

## 6. Documentacion relacionada actualizada

- `docs/endpoints/iam/auth.md`
- `docs/api/multi-tenant-real-data-testing.md`
- `docs/setup-and-run.md`

## 7. Como probar rapidamente

1. levantar Postgres local
2. arrancar `iam-service` con perfil `dev`
3. hacer login con el admin bootstrap
4. verificar que `register` responde `403`

### Arranque local

```bash
cd iam-service
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### Verificacion de login

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@solveria.local",
    "password": "Admin12345!"
  }'
```

### Verificacion de bloqueo de register

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "demo.user",
    "email": "demo.user@example.com",
    "password": "SecurePass123",
    "userCategory": "STUDENT",
    "tenantId": "demo-tenant"
  }'
```

## 8. Lo que no existe todavia

Este cambio no incluye aun:

- `POST /api/v1/admin/users`
- CRUD administrativo de tenants
- CRUD administrativo de competencias
- proteccion fina por JWT local para endpoints administrativos futuros

## 9. Siguiente paso recomendado

El siguiente bloque tecnico correcto es:

1. crear CRUD administrativo de usuarios
2. proteger esos endpoints con el modelo de autenticacion del servicio
3. despues continuar con tenants y memberships
