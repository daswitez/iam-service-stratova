# Admin Competition Tenants API

## 1. Resumen

Este documento describe la gestion administrativa de tenants participantes expuesta en:

- `/api/v1/admin/competitions/{competitionId}/tenants`

Reglas actuales:

- solo un usuario autenticado con rol `PLATFORM_ADMIN` puede usar estos endpoints
- el tenant participante debe existir y estar activo
- no se permite duplicar la misma participacion en una competencia
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
export COMPETITION_ID="<COMPETITION_UUID>"
```

## 3. Endpoints

### 3.1 Agregar tenant participante

`POST /api/v1/admin/competitions/{competitionId}/tenants`

```bash
curl -X POST http://localhost:8080/api/v1/admin/competitions/$COMPETITION_ID/tenants \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": "22222222-2222-2222-2222-222222222222"
  }'
```

### 3.2 Listar tenants participantes

`GET /api/v1/admin/competitions/{competitionId}/tenants`

```bash
curl -X GET http://localhost:8080/api/v1/admin/competitions/$COMPETITION_ID/tenants \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

### 3.3 Quitar tenant participante

`DELETE /api/v1/admin/competitions/{competitionId}/tenants/{tenantId}`

```bash
curl -X DELETE http://localhost:8080/api/v1/admin/competitions/$COMPETITION_ID/tenants/22222222-2222-2222-2222-222222222222 \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

## 4. Respuestas de seguridad esperadas

### Sin token

```json
{
  "errorCode": "UNAUTHORIZED",
  "timestamp": "2026-03-13T01:00:00Z",
  "path": "/api/v1/admin/competitions/<COMPETITION_ID>/tenants",
  "details": null,
  "correlationId": null
}
```

### Con token sin `PLATFORM_ADMIN`

```json
{
  "errorCode": "FORBIDDEN",
  "timestamp": "2026-03-13T01:00:00Z",
  "path": "/api/v1/admin/competitions/<COMPETITION_ID>/tenants",
  "details": null,
  "correlationId": null
}
```

## 5. Reglas y limitaciones actuales

- el endpoint no agrega automaticamente el tenant host
- la participacion se modela como relacion explicita en `iam_competition_tenant`
- el siguiente paso natural es abrir enrollments multiuniversidad
