# Admin Memberships API

## 1. Resumen

Este documento describe el CRUD administrativo de memberships expuesto en:

- `POST /api/v1/admin/memberships`
- `GET /api/v1/admin/memberships/{id}`
- `GET /api/v1/admin/users/{userId}/memberships`
- `GET /api/v1/admin/tenants/{tenantId}/memberships`
- `PUT /api/v1/admin/memberships/{id}`
- `DELETE /api/v1/admin/memberships/{id}`

Regla actual:

- solo un usuario autenticado con rol `PLATFORM_ADMIN` puede usar estos endpoints
- un usuario puede tener multiples memberships
- solo una membership activa puede quedar como primaria
- la API expone `isPrimary`, pero la persistencia usa `membershipType`
- los errores del modulo administrativo responden con el contrato `ApiErrorResponse`

## 2. Obtener token de administrador

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@solveria.local",
    "password": "Admin12345!"
  }'
```

Guardar el token:

```bash
export ADMIN_TOKEN="<JWT>"
```

## 3. Endpoints

### 3.1 Crear membership

`POST /api/v1/admin/memberships`

```bash
curl -X POST http://localhost:8080/api/v1/admin/memberships \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 10,
    "tenantId": "11111111-1111-1111-1111-111111111111",
    "status": "ACTIVE",
    "isPrimary": true
  }'
```

Respuesta esperada:

```json
{
  "id": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
  "userId": 10,
  "tenantId": "11111111-1111-1111-1111-111111111111",
  "tenantCode": "umsa",
  "tenantName": "Universidad Mayor de San Andres",
  "tenantType": "UNIVERSITY",
  "membershipType": "PRIMARY",
  "isPrimary": true,
  "status": "ACTIVE",
  "createdAt": "2026-03-12T23:00:00Z"
}
```

### 3.2 Obtener membership por id

`GET /api/v1/admin/memberships/{id}`

```bash
curl -X GET http://localhost:8080/api/v1/admin/memberships/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

### 3.3 Listar memberships de un usuario

`GET /api/v1/admin/users/{userId}/memberships`

```bash
curl -X GET http://localhost:8080/api/v1/admin/users/10/memberships \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

Filtro opcional por estado:

```bash
curl -X GET "http://localhost:8080/api/v1/admin/users/10/memberships?status=ACTIVE" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

### 3.4 Listar memberships de un tenant

`GET /api/v1/admin/tenants/{tenantId}/memberships`

```bash
curl -X GET http://localhost:8080/api/v1/admin/tenants/11111111-1111-1111-1111-111111111111/memberships \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

Filtro opcional por estado:

```bash
curl -X GET "http://localhost:8080/api/v1/admin/tenants/11111111-1111-1111-1111-111111111111/memberships?status=ACTIVE" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

### 3.5 Actualizar membership

`PUT /api/v1/admin/memberships/{id}`

```bash
curl -X PUT http://localhost:8080/api/v1/admin/memberships/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "SUSPENDED",
    "isPrimary": false
  }'
```

Notas:

- si una membership deja de estar activa, deja de ser primaria
- si marcas una membership activa como primaria, el servicio degrada la primaria anterior
- si eliminas o cierras la primaria activa, el servicio promueve otra activa segun orden de creacion

### 3.6 Desactivar membership

`DELETE /api/v1/admin/memberships/{id}`

```bash
curl -X DELETE http://localhost:8080/api/v1/admin/memberships/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

Respuesta esperada:

- `204 No Content`

## 4. Reglas operativas

- no se puede repetir la combinacion `userId + tenantId`
- la primaria activa unica se reequilibra automaticamente
- `DELETE` hace baja logica con `status=LEFT`
- `iam_user.tenant_id` se sincroniza con la primaria activa por compatibilidad

## 5. Respuestas de seguridad esperadas

### Sin token

```json
{
  "errorCode": "UNAUTHORIZED",
  "timestamp": "2026-03-12T23:00:00Z",
  "path": "/api/v1/admin/memberships",
  "details": null,
  "correlationId": null
}
```

### Con token sin `PLATFORM_ADMIN`

```json
{
  "errorCode": "FORBIDDEN",
  "timestamp": "2026-03-12T23:00:00Z",
  "path": "/api/v1/admin/memberships",
  "details": null,
  "correlationId": null
}
```

### Payload invalido

```json
{
  "errorCode": "VALIDATION_ERROR",
  "timestamp": "2026-03-12T23:00:00Z",
  "path": "/api/v1/admin/memberships",
  "details": {
    "userId": "User id is required",
    "tenantId": "Tenant id is required",
    "status": "Status must be ACTIVE, INVITED, SUSPENDED, or LEFT"
  },
  "correlationId": null
}
```

## 6. Referencias

- `docs/changes/007-admin-memberships-crud.md`
- `docs/changes/008-admin-module-authorization-and-error-normalization.md`
- `docs/api/admin-users.md`
- `docs/api/admin-universities.md`
- `docs/api/admin-sub-tenants.md`
