# Competition Enrollments API

## 1. Resumen

Este documento describe los endpoints iniciales de enrollments de staff expuestos en:

- `/api/v1/competitions/{competitionId}/enrollments`

Reglas actuales:

- solo un usuario autenticado con rol `PLATFORM_ADMIN` puede usar estos endpoints
- el staff se inscribe como `JUDGE`, `INVESTOR`, `MENTOR` o `MANAGER`
- el request debe indicar `originTenantId` explicitamente
- el usuario debe tener membership activa en ese `originTenantId`
- el `originTenantId` debe estar habilitado para la competencia
- el tenant host tambien se considera valido como origen para la competencia
- `DELETE` hace baja logica y deja `status=WITHDRAWN`
- los errores del modulo responden con el contrato `ApiErrorResponse`

## 2. Obtener token de administrador

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@solveria.local",
    "password": "Admin12345!"
  }'
```

Guardar variables:

```bash
export ADMIN_TOKEN="<JWT>"
export COMPETITION_ID="<COMPETITION_UUID>"
export ENROLLMENT_ID="<ENROLLMENT_UUID>"
```

## 3. Endpoints

### 3.1 Inscribir staff

`POST /api/v1/competitions/{competitionId}/enrollments/staff`

```bash
curl -X POST http://localhost:8080/api/v1/competitions/$COMPETITION_ID/enrollments/staff \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 10,
    "originTenantId": "11111111-1111-1111-1111-111111111111",
    "participantType": "JUDGE"
  }'
```

Respuesta esperada:

```json
{
  "id": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
  "competitionId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
  "competitionCode": "biz-sim-2026",
  "userId": 10,
  "originTenantId": "11111111-1111-1111-1111-111111111111",
  "originTenantCode": "umsa",
  "originTenantName": "Universidad Mayor de San Andres",
  "participantType": "JUDGE",
  "status": "INVITED",
  "invitedByUserId": null,
  "approvedAt": null,
  "createdAt": "2026-03-13T23:00:00Z"
}
```

### 3.2 Ver detalle de enrollment

`GET /api/v1/competitions/{competitionId}/enrollments/{enrollmentId}`

```bash
curl -X GET http://localhost:8080/api/v1/competitions/$COMPETITION_ID/enrollments/$ENROLLMENT_ID \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

### 3.3 Actualizar estado de enrollment

`PATCH /api/v1/competitions/{competitionId}/enrollments/{enrollmentId}/status`

```bash
curl -X PATCH http://localhost:8080/api/v1/competitions/$COMPETITION_ID/enrollments/$ENROLLMENT_ID/status \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "APPROVED"
  }'
```

Notas:

- al pasar a `APPROVED`, el servicio completa `approvedAt`
- el endpoint acepta `INVITED`, `APPROVED`, `REJECTED`, `WAITLISTED` y `WITHDRAWN`

### 3.4 Retirar enrollment

`DELETE /api/v1/competitions/{competitionId}/enrollments/{enrollmentId}`

```bash
curl -X DELETE http://localhost:8080/api/v1/competitions/$COMPETITION_ID/enrollments/$ENROLLMENT_ID \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

Respuesta esperada:

- `204 No Content`

## 4. Respuestas de seguridad esperadas

### Sin token

```json
{
  "errorCode": "UNAUTHORIZED",
  "timestamp": "2026-03-13T23:00:00Z",
  "path": "/api/v1/competitions/<COMPETITION_ID>/enrollments/staff",
  "details": null,
  "correlationId": null
}
```

### Con token sin `PLATFORM_ADMIN`

```json
{
  "errorCode": "FORBIDDEN",
  "timestamp": "2026-03-13T23:00:00Z",
  "path": "/api/v1/competitions/<COMPETITION_ID>/enrollments/staff",
  "details": null,
  "correlationId": null
}
```

## 5. Reglas y limitaciones actuales

- esta primera entrega cubre solo enrollments de staff
- la lista general de enrollments y el alta de estudiantes quedan para el siguiente corte del modulo
- el sistema mantiene una sola fila por `competitionId + userId`, por lo que no se permite duplicar enrollments en la misma competencia
- `originTenantId` debe coincidir con una membership activa real del usuario
- si el tenant origen no es el host, debe existir como tenant participante de la competencia
- el siguiente paso natural es `P2.2 Endpoint de staff elegible`
