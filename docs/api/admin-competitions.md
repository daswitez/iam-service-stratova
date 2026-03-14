# Admin Competitions API

## 1. Resumen

Este documento describe el CRUD administrativo de competencias expuesto en:

- `/api/v1/admin/competitions`

Reglas actuales:

- solo un usuario autenticado con rol `PLATFORM_ADMIN` puede usar estos endpoints
- la competencia requiere `hostTenantId` como tenant anfitrion
- el MVP fija `minTeamSize=4` y `maxTeamSize=6`
- `teamCreationMode` se persiste como `ADMIN_MANAGED`
- el `GET /api/v1/admin/competitions` acepta filtros por `status`, `hostTenantId` y `cycle`
- si existe un `iam_academic_cycle` del tenant host que contiene `startsAt` y `endsAt`, la competencia queda vinculada automaticamente a ese ciclo
- `DELETE` hace baja logica y deja `status=ARCHIVED`
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

### 3.1 Crear competencia

`POST /api/v1/admin/competitions`

```bash
curl -X POST http://localhost:8080/api/v1/admin/competitions \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "code": "biz-sim-2026",
    "name": "Business Simulation 2026",
    "description": "Competencia interuniversitaria",
    "scope": "CROSS_TENANT",
    "hostTenantId": "11111111-1111-1111-1111-111111111111",
    "productName": "Smart Retail",
    "industryCode": "retail-tech",
    "industryName": "Retail Technology",
    "initialCapitalAmount": 100000.00,
    "minTeamSize": 4,
    "maxTeamSize": 6,
    "roleAssignmentMethod": "ADMIN_ASSIGNMENT",
    "allowOptionalCoo": true,
    "startsAt": "2026-04-01T00:00:00Z",
    "endsAt": "2026-06-01T00:00:00Z"
  }'
```

Respuesta esperada:

```json
{
  "id": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
  "code": "biz-sim-2026",
  "name": "Business Simulation 2026",
  "scope": "CROSS_TENANT",
  "status": "DRAFT",
  "hostTenantCode": "umsa",
  "productName": "Smart Retail",
  "industryCode": "retail-tech",
  "industryName": "Retail Technology",
  "initialCapitalAmount": 100000.00,
  "currency": "USD",
  "minTeamSize": 4,
  "maxTeamSize": 6,
  "teamCreationMode": "ADMIN_MANAGED",
  "roleAssignmentMethod": "ADMIN_ASSIGNMENT",
  "allowOptionalCoo": true,
  "startsAt": "2026-04-01T00:00:00Z",
  "endsAt": "2026-06-01T00:00:00Z",
  "createdAt": "2026-03-12T23:00:00Z"
}
```

### 3.2 Listar competencias

`GET /api/v1/admin/competitions`

```bash
curl -X GET http://localhost:8080/api/v1/admin/competitions \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

Filtro opcional por estado:

```bash
curl -X GET "http://localhost:8080/api/v1/admin/competitions?status=DRAFT" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

Filtro opcional por tenant host:

```bash
curl -X GET "http://localhost:8080/api/v1/admin/competitions?hostTenantId=11111111-1111-1111-1111-111111111111" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

Filtro opcional por ciclo academico:

```bash
curl -X GET "http://localhost:8080/api/v1/admin/competitions?cycle=2026-S1" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

Filtros combinados:

```bash
curl -X GET "http://localhost:8080/api/v1/admin/competitions?status=ACTIVE&hostTenantId=11111111-1111-1111-1111-111111111111&cycle=2026-S1" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

Notas:

- `cycle` compara contra `iam_academic_cycle.code`
- la comparacion es case-insensitive
- si una competencia no tiene ciclo academico asociado, no aparece cuando se usa `cycle`

### 3.3 Obtener competencia por id

`GET /api/v1/admin/competitions/{id}`

```bash
curl -X GET http://localhost:8080/api/v1/admin/competitions/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

### 3.4 Actualizar competencia

`PUT /api/v1/admin/competitions/{id}`

```bash
curl -X PUT http://localhost:8080/api/v1/admin/competitions/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "code": "biz-sim-2026",
    "name": "Business Simulation 2026",
    "description": "Competencia actualizada",
    "scope": "CROSS_TENANT",
    "hostTenantId": "11111111-1111-1111-1111-111111111111",
    "productName": "Smart Retail",
    "industryCode": "retail-tech",
    "industryName": "Retail Technology",
    "initialCapitalAmount": 100000.00,
    "minTeamSize": 4,
    "maxTeamSize": 6,
    "roleAssignmentMethod": "ADMIN_ASSIGNMENT",
    "allowOptionalCoo": true,
    "status": "ACTIVE",
    "startsAt": "2026-04-01T00:00:00Z",
    "endsAt": "2026-06-01T00:00:00Z"
  }'
```

### 3.5 Archivar competencia

`DELETE /api/v1/admin/competitions/{id}`

```bash
curl -X DELETE http://localhost:8080/api/v1/admin/competitions/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

Respuesta esperada:

- `204 No Content`

## 4. Respuestas de seguridad esperadas

### Sin token

```bash
curl -X GET http://localhost:8080/api/v1/admin/competitions
```

```json
{
  "errorCode": "UNAUTHORIZED",
  "timestamp": "2026-03-12T23:00:00Z",
  "path": "/api/v1/admin/competitions",
  "details": null,
  "correlationId": null
}
```

### Con token sin `PLATFORM_ADMIN`

```json
{
  "errorCode": "FORBIDDEN",
  "timestamp": "2026-03-12T23:00:00Z",
  "path": "/api/v1/admin/competitions",
  "details": null,
  "correlationId": null
}
```

## 5. Reglas y limitaciones actuales

- el MVP no permite tamanos distintos de `4` y `6`
- `currency` queda fija en `USD`
- `teamCreationMode` queda fijo en `ADMIN_MANAGED`
- la vinculacion automatica de ciclo depende de que ya exista un registro en `iam_academic_cycle` para el tenant host
- si necesitas preparar ciclos reales para pruebas, usa la guia `docs/api/multi-tenant-real-data-testing.md`
- el siguiente paso natural es abrir enrollments multiuniversidad
