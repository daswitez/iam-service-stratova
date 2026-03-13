# Admin Universities API

## 1. Resumen

Este documento describe el CRUD administrativo de universidades expuesto en:

- `/api/v1/admin/universities`

Regla actual:

- solo un usuario autenticado con rol `PLATFORM_ADMIN` puede usar estos endpoints
- una universidad se persiste como `TenantType.UNIVERSITY`
- `DELETE` hace baja logica y deja `status=INACTIVE`
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

### 3.1 Crear universidad

`POST /api/v1/admin/universities`

```bash
curl -X POST http://localhost:8080/api/v1/admin/universities \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "code": "umsa",
    "name": "Universidad Mayor de San Andres"
  }'
```

Respuesta esperada:

```json
{
  "id": "6dfece42-f772-4f2a-a084-af4f11223344",
  "code": "umsa",
  "name": "Universidad Mayor de San Andres",
  "type": "UNIVERSITY",
  "status": "ACTIVE",
  "createdAt": "2026-03-12T23:00:00Z"
}
```

### 3.2 Listar universidades

`GET /api/v1/admin/universities`

```bash
curl -X GET http://localhost:8080/api/v1/admin/universities \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

Filtro opcional por estado:

```bash
curl -X GET "http://localhost:8080/api/v1/admin/universities?status=ACTIVE" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

### 3.3 Obtener universidad por id

`GET /api/v1/admin/universities/{id}`

```bash
curl -X GET http://localhost:8080/api/v1/admin/universities/6dfece42-f772-4f2a-a084-af4f11223344 \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

### 3.4 Actualizar universidad

`PUT /api/v1/admin/universities/{id}`

```bash
curl -X PUT http://localhost:8080/api/v1/admin/universities/6dfece42-f772-4f2a-a084-af4f11223344 \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "code": "umsa-central",
    "name": "Universidad Mayor de San Andres"
  }'
```

Notas:

- `code` se normaliza a lowercase
- el `code` debe ser unico

### 3.5 Desactivar universidad

`DELETE /api/v1/admin/universities/{id}`

```bash
curl -X DELETE http://localhost:8080/api/v1/admin/universities/6dfece42-f772-4f2a-a084-af4f11223344 \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

Respuesta esperada:

- `204 No Content`

## 4. Respuestas de seguridad esperadas

### Sin token

```bash
curl -X GET http://localhost:8080/api/v1/admin/universities
```

```json
{
  "errorCode": "UNAUTHORIZED",
  "timestamp": "2026-03-12T23:00:00Z",
  "path": "/api/v1/admin/universities",
  "details": null,
  "correlationId": null
}
```

### Con token sin `PLATFORM_ADMIN`

```json
{
  "errorCode": "FORBIDDEN",
  "timestamp": "2026-03-12T23:00:00Z",
  "path": "/api/v1/admin/universities",
  "details": null,
  "correlationId": null
}
```

## 5. Limitaciones actuales

- este endpoint solo administra universidades
- facultades y programas se documentan en `docs/api/admin-sub-tenants.md`
- memberships se documentan en `docs/api/admin-memberships.md`

## 6. Troubleshooting

### 6.1 `UNEXPECTED_ERROR` al crear una universidad

Si recibes una respuesta como esta:

```json
{
  "errorCode": "UNEXPECTED_ERROR",
  "timestamp": "2026-03-12T23:23:52.983674147Z",
  "path": "/api/v1/admin/universities",
  "details": null,
  "correlationId": null
}
```

la causa mas probable es que `iam-service` sigue corriendo con una instancia vieja, anterior a este cambio.

Haz esto:

```bash
cd /home/daswit/Downloads/ProyectoAI/iam-service
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Luego vuelve a hacer login para obtener un token nuevo:

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@solveria.local",
    "password": "Admin12345!"
  }'
```

Y repite el request de alta:

```bash
curl -X POST http://localhost:8080/api/v1/admin/universities \
  -H "Authorization: Bearer <TOKEN_NUEVO>" \
  -H "Content-Type: application/json" \
  -d '{
    "code": "umsa",
    "name": "Universidad Mayor de San Andres"
  }'
```

Si despues de reiniciar el servicio el error sigue apareciendo, hay que revisar el stacktrace del backend, no solo la respuesta JSON.

### 6.2 `error.business.rule.violation` al crear una universidad

Si recibes:

```json
{
  "errorCode": "error.business.rule.violation",
  "timestamp": "2026-03-12T23:29:21.296379603Z",
  "path": "/api/v1/admin/universities",
  "details": null,
  "correlationId": null
}
```

en `POST /api/v1/admin/universities`, la causa mas probable es que el `code` ya existe.

Ejemplo verificado:

- `umsa` ya estaba registrado como universidad activa

Puedes comprobarlo listando universidades:

```bash
curl -X GET http://localhost:8080/api/v1/admin/universities \
  -H "Authorization: Bearer <TOKEN_NUEVO>"
```

Opciones correctas:

1. usar otro `code`
2. actualizar la universidad existente con `PUT /api/v1/admin/universities/{id}`

Ejemplo con otro `code`:

```bash
curl -X POST http://localhost:8080/api/v1/admin/universities \
  -H "Authorization: Bearer <TOKEN_NUEVO>" \
  -H "Content-Type: application/json" \
  -d '{
    "code": "umsa-central",
    "name": "Universidad Mayor de San Andres"
  }'
```

## 7. Referencias

- `docs/changes/005-admin-universities-crud.md`
- `docs/changes/008-admin-module-authorization-and-error-normalization.md`
- `docs/api/admin-users.md`
- `docs/api/admin-sub-tenants.md`
- `docs/api/admin-memberships.md`
