# Admin Users API

## 1. Resumen

Este documento describe el CRUD administrativo de usuarios expuesto en:

- `/api/v1/admin/users`

Regla actual:

- solo un usuario autenticado con rol `PLATFORM_ADMIN` puede usar estos endpoints
- en `dev`, `iam-service` valida localmente el JWT emitido por `POST /api/v1/auth/login` para las rutas `/api/v1/admin/**`
- los errores del modulo administrativo responden con el contrato `ApiErrorResponse`

## 2. Obtener token de administrador

### Login del bootstrap admin

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@solveria.local",
    "password": "Admin12345!"
  }'
```

Guardar el `token` devuelto y reutilizarlo:

```bash
export ADMIN_TOKEN="<JWT>"
```

## 3. Endpoints

### 3.1 Crear usuario

`POST /api/v1/admin/users`

Notas:

- `primaryTenantId` es opcional
- `roleNames` hoy resuelve roles del tenant `system`
- hoy el rol persistido soportado de forma operativa es `PLATFORM_ADMIN`

```bash
curl -X POST http://localhost:8080/api/v1/admin/users \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "maria.admin",
    "email": "maria.admin@solveria.local",
    "password": "SecurePass123",
    "userCategory": "ACADEMIC_ADMIN",
    "primaryTenantId": null,
    "roleNames": ["PLATFORM_ADMIN"]
  }'
```

Respuesta esperada:

```json
{
  "id": 2,
  "username": "maria.admin",
  "email": "maria.admin@solveria.local",
  "userCategory": "ACADEMIC_ADMIN",
  "active": true,
  "primaryTenantId": null,
  "roleNames": ["PLATFORM_ADMIN"],
  "createdAt": "2026-03-12T19:00:00",
  "lastModifiedAt": null
}
```

### 3.2 Listar usuarios

`GET /api/v1/admin/users`

```bash
curl -X GET http://localhost:8080/api/v1/admin/users \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

Filtro opcional por estado:

```bash
curl -X GET "http://localhost:8080/api/v1/admin/users?active=true" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

### 3.3 Obtener usuario por id

`GET /api/v1/admin/users/{id}`

```bash
curl -X GET http://localhost:8080/api/v1/admin/users/2 \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

### 3.4 Actualizar usuario

`PUT /api/v1/admin/users/{id}`

Notas:

- `password` es opcional
- si se envía `active=false` para un `PLATFORM_ADMIN`, el servicio protege que no elimines al ultimo admin activo
- si se envía `primaryTenantId`, se sincroniza la membership primaria

```bash
curl -X PUT http://localhost:8080/api/v1/admin/users/2 \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "maria.admin",
    "email": "maria.admin@solveria.local",
    "password": "",
    "userCategory": "ACADEMIC_ADMIN",
    "active": true,
    "primaryTenantId": null,
    "roleNames": ["PLATFORM_ADMIN"]
  }'
```

### 3.5 Desactivar usuario

`DELETE /api/v1/admin/users/{id}`

Este endpoint hace baja logica:

- no borra la fila
- marca `active=false`

```bash
curl -X DELETE http://localhost:8080/api/v1/admin/users/2 \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

Respuesta esperada:

- `204 No Content`

## 4. Respuestas de seguridad esperadas

### Sin token

```bash
curl -X GET http://localhost:8080/api/v1/admin/users
```

Respuesta esperada:

```json
{
  "errorCode": "UNAUTHORIZED",
  "timestamp": "2026-03-12T23:00:00Z",
  "path": "/api/v1/admin/users",
  "details": null,
  "correlationId": null
}
```

### Con token sin `PLATFORM_ADMIN`

```json
{
  "errorCode": "FORBIDDEN",
  "timestamp": "2026-03-12T23:00:00Z",
  "path": "/api/v1/admin/users",
  "details": null,
  "correlationId": null
}
```

## 5. Limitaciones actuales

- los roles persistidos se resuelven contra el tenant `system`
- el cambio de `primaryTenantId` sigue siendo una via de compatibilidad; el CRUD completo vive en `docs/api/admin-memberships.md`
- los roles administrativos operativos siguen concentrados en `PLATFORM_ADMIN`
- los CRUDs de competencias, enrollments y equipos todavia no estan expuestos

## 6. Referencias

- `docs/changes/004-admin-users-crud-and-dev-jwt-protection.md`
- `docs/changes/008-admin-module-authorization-and-error-normalization.md`
- `docs/changes/001-platform-admin-bootstrap-and-auth-hardening.md`
- `docs/api/admin-memberships.md`
