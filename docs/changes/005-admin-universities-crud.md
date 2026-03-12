# Cambio 005 - Admin Universities CRUD

## 1. Resumen

Este cambio implementa `P1.1 CRUD de universidades`.

Se agrega administracion REST para universidades sobre el modelo multi-tenant ya existente:

- crear universidad
- listar universidades
- obtener detalle
- editar `code` y `name`
- desactivar universidad

## 2. Objetivo

Permitir que el `PLATFORM_ADMIN` cree y administre universidades reales antes de avanzar a facultades, programas, competencias y memberships.

## 3. Endpoint nuevo

Base path:

- `/api/v1/admin/universities`

Operaciones:

- `POST /api/v1/admin/universities`
- `GET /api/v1/admin/universities`
- `GET /api/v1/admin/universities/{id}`
- `PUT /api/v1/admin/universities/{id}`
- `DELETE /api/v1/admin/universities/{id}`

## 4. Reglas de negocio

- cada registro usa `TenantType.UNIVERSITY`
- `code` es unico y se normaliza a lowercase
- las universidades se crean con `status=ACTIVE`
- `DELETE` no borra fisicamente; cambia a `INACTIVE`
- solo `PLATFORM_ADMIN` puede operar estos endpoints

## 5. Ejemplos curl

### Crear

```bash
curl -X POST http://localhost:8080/api/v1/admin/universities \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "code": "umsa",
    "name": "Universidad Mayor de San Andres"
  }'
```

### Listar

```bash
curl -X GET http://localhost:8080/api/v1/admin/universities \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

### Actualizar

```bash
curl -X PUT http://localhost:8080/api/v1/admin/universities/6dfece42-f772-4f2a-a084-af4f11223344 \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "code": "umsa-central",
    "name": "Universidad Mayor de San Andres"
  }'
```

### Desactivar

```bash
curl -X DELETE http://localhost:8080/api/v1/admin/universities/6dfece42-f772-4f2a-a084-af4f11223344 \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

## 6. Archivos principales tocados

### Codigo

- `src/main/java/com/solveria/iamservice/application/service/AdminUniversityService.java`
- `src/main/java/com/solveria/iamservice/api/rest/AdminUniversityController.java`
- `src/main/java/com/solveria/iamservice/api/rest/dto/AdminUniversityCreateRequest.java`
- `src/main/java/com/solveria/iamservice/api/rest/dto/AdminUniversityUpdateRequest.java`
- `src/main/java/com/solveria/iamservice/api/rest/dto/AdminUniversityResponse.java`
- `src/main/java/com/solveria/iamservice/multitenancy/persistence/entity/TenantJpaEntity.java`
- `src/main/java/com/solveria/iamservice/multitenancy/persistence/repository/TenantJpaRepository.java`

### Pruebas

- `src/test/java/com/solveria/iamservice/application/service/AdminUniversityServiceTest.java`
- `src/test/java/com/solveria/iamservice/api/rest/AdminUniversityControllerIT.java`

## 7. Verificacion ejecutada

Comando ejecutado:

```bash
cd iam-service
./mvnw test
```

Resultado:

- `BUILD SUCCESS`
- `22` tests pasando

## 8. Documentacion relacionada

- `docs/api/admin-universities.md`
- `docs/api/admin-users.md`

## 9. Siguiente paso recomendado

El siguiente paso correcto es `P1.2 CRUD de sub-tenants`, con foco en:

1. facultades
2. programas
3. validacion jerarquica `UNIVERSITY -> FACULTY -> PROGRAM`

## 10. Nota operativa

Si al probar `POST /api/v1/admin/universities` aparece:

```json
{
  "errorCode": "UNEXPECTED_ERROR",
  "timestamp": "2026-03-12T23:23:52.983674147Z",
  "path": "/api/v1/admin/universities",
  "details": null,
  "correlationId": null
}
```

la primera verificacion correcta es reiniciar `iam-service` y volver a hacer login para usar un token nuevo.

```bash
cd /home/daswit/Downloads/ProyectoAI/iam-service
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Luego:

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@solveria.local",
    "password": "Admin12345!"
  }'
```

Si el error persiste despues del reinicio, entonces hace falta revisar el stacktrace del backend.

Si el error cambia a:

```json
{
  "errorCode": "error.business.rule.violation",
  "timestamp": "2026-03-12T23:29:21.296379603Z",
  "path": "/api/v1/admin/universities",
  "details": null,
  "correlationId": null
}
```

entonces el problema ya no es runtime sino de regla de negocio: el `code` de la universidad ya existe.
