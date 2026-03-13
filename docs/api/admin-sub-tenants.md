# Admin Sub-Tenants API

## 1. Resumen

Este documento describe el CRUD administrativo de sub-tenants expuesto en:

- `POST /api/v1/admin/tenants/{parentId}/children`
- `GET /api/v1/admin/tenants/{parentId}/children`
- `GET /api/v1/admin/sub-tenants/{id}`
- `PUT /api/v1/admin/sub-tenants/{id}`
- `DELETE /api/v1/admin/sub-tenants/{id}`

Regla actual:

- solo un usuario autenticado con rol `PLATFORM_ADMIN` puede usar estos endpoints
- la jerarquia permitida es estricta:
  - `UNIVERSITY -> FACULTY`
  - `FACULTY -> PROGRAM`
- no se permite crear `PROGRAM` directo bajo `UNIVERSITY`
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

### 3.1 Crear facultad bajo una universidad

`POST /api/v1/admin/tenants/{parentId}/children`

```bash
curl -X POST http://localhost:8080/api/v1/admin/tenants/<UNIVERSITY_ID>/children \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "code": "facultad-ingenieria",
    "name": "Facultad de Ingenieria",
    "type": "FACULTY"
  }'
```

### 3.2 Crear programa bajo una facultad

```bash
curl -X POST http://localhost:8080/api/v1/admin/tenants/<FACULTY_ID>/children \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "code": "adm-sis-umsa",
    "name": "Administracion de Sistemas UMSA",
    "type": "PROGRAM"
  }'
```

Respuesta esperada:

```json
{
  "id": "22222222-2222-2222-2222-222222222222",
  "code": "adm-sis-umsa",
  "name": "Administracion de Sistemas UMSA",
  "type": "PROGRAM",
  "status": "ACTIVE",
  "parentTenantId": "<FACULTY_ID>",
  "parentTenantType": "FACULTY",
  "createdAt": "2026-03-12T23:00:00Z"
}
```

### 3.3 Listar hijos de un tenant

`GET /api/v1/admin/tenants/{parentId}/children`

```bash
curl -X GET http://localhost:8080/api/v1/admin/tenants/<UNIVERSITY_ID>/children \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

Filtros opcionales:

```bash
curl -X GET "http://localhost:8080/api/v1/admin/tenants/<UNIVERSITY_ID>/children?type=FACULTY&status=ACTIVE" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

### 3.4 Obtener sub-tenant por id

`GET /api/v1/admin/sub-tenants/{id}`

```bash
curl -X GET http://localhost:8080/api/v1/admin/sub-tenants/<SUB_TENANT_ID> \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

### 3.5 Actualizar sub-tenant

`PUT /api/v1/admin/sub-tenants/{id}`

```bash
curl -X PUT http://localhost:8080/api/v1/admin/sub-tenants/<SUB_TENANT_ID> \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "code": "adm-sis-umsa-central",
    "name": "Administracion de Sistemas UMSA"
  }'
```

### 3.6 Desactivar sub-tenant

`DELETE /api/v1/admin/sub-tenants/{id}`

```bash
curl -X DELETE http://localhost:8080/api/v1/admin/sub-tenants/<SUB_TENANT_ID> \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

Respuesta esperada:

- `204 No Content`

## 4. Reglas de jerarquia

Permitido:

- `UNIVERSITY -> FACULTY`
- `FACULTY -> PROGRAM`

No permitido:

- `UNIVERSITY -> PROGRAM`
- `PROGRAM -> PROGRAM`
- `PROGRAM -> FACULTY`
- cualquier tipo enterprise en este endpoint

## 5. Respuestas de error esperadas

### Jerarquia invalida

Si intentas crear un `PROGRAM` directo bajo una `UNIVERSITY`, el servicio devuelve una violacion de regla de negocio.

### Code duplicado

Si el `code` ya existe, el servicio devuelve una violacion de regla de negocio.

### Sin token

```json
{
  "errorCode": "UNAUTHORIZED",
  "timestamp": "2026-03-12T23:00:00Z",
  "path": "/api/v1/admin/tenants/<PARENT_ID>/children",
  "details": null,
  "correlationId": null
}
```

### Con token sin `PLATFORM_ADMIN`

```json
{
  "errorCode": "FORBIDDEN",
  "timestamp": "2026-03-12T23:00:00Z",
  "path": "/api/v1/admin/tenants/<PARENT_ID>/children",
  "details": null,
  "correlationId": null
}
```

## 6. Referencias

- `docs/changes/006-admin-sub-tenants-crud.md`
- `docs/changes/008-admin-module-authorization-and-error-normalization.md`
- `docs/api/admin-universities.md`
- `docs/api/admin-memberships.md`
