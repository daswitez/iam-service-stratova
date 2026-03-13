# Cambio 010 - Admin Competitions CRUD

## 1. Resumen

Este cambio implementa `P1.6 CRUD de competencias`.

Se agrega administracion REST para:

- crear competencias con reglas base del MVP
- listar competencias
- ver detalle
- editar competencia
- archivar competencia

## 2. Objetivo

Permitir que el `PLATFORM_ADMIN` cree y mantenga competencias academicas con:

- tenant host
- producto e industria
- capital inicial comun
- reglas base de equipos del MVP

## 3. Endpoints nuevos

- `POST /api/v1/admin/competitions`
- `GET /api/v1/admin/competitions`
- `GET /api/v1/admin/competitions/{id}`
- `PUT /api/v1/admin/competitions/{id}`
- `DELETE /api/v1/admin/competitions/{id}`

## 4. Reglas de negocio

- `code` se normaliza a lowercase y debe ser unico
- `hostTenantId` debe existir y estar activo
- el MVP fija `minTeamSize=4` y `maxTeamSize=6`
- `currency` se fija en `USD`
- `teamCreationMode` se fija en `ADMIN_MANAGED`
- `DELETE` hace baja logica con `status=ARCHIVED`

## 5. Ejemplos curl

### Login de admin

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

### Crear competencia

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

### Listar competencias

```bash
curl -X GET http://localhost:8080/api/v1/admin/competitions \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

Filtro por estado:

```bash
curl -X GET "http://localhost:8080/api/v1/admin/competitions?status=DRAFT" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

### Ver detalle de competencia

```bash
curl -X GET http://localhost:8080/api/v1/admin/competitions/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

### Editar competencia

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

### Archivar competencia

```bash
curl -X DELETE http://localhost:8080/api/v1/admin/competitions/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

Nota:

- la documentacion REST detallada del modulo queda en `docs/api/admin-competitions.md`

## 6. Archivos principales tocados

### Codigo

- `src/main/java/com/solveria/iamservice/api/rest/AdminCompetitionController.java`
- `src/main/java/com/solveria/iamservice/application/service/AdminCompetitionService.java`
- `src/main/java/com/solveria/iamservice/api/rest/dto/AdminCompetitionCreateRequest.java`
- `src/main/java/com/solveria/iamservice/api/rest/dto/AdminCompetitionUpdateRequest.java`
- `src/main/java/com/solveria/iamservice/api/rest/dto/AdminCompetitionResponse.java`
- `src/main/java/com/solveria/iamservice/multitenancy/persistence/entity/CompetitionJpaEntity.java`
- `src/main/java/com/solveria/iamservice/multitenancy/persistence/entity/CompetitionStatus.java`
- `src/main/java/com/solveria/iamservice/multitenancy/persistence/entity/CompetitionRoleAssignmentMethod.java`
- `src/main/java/com/solveria/iamservice/multitenancy/persistence/entity/TeamCreationMode.java`
- `src/main/java/com/solveria/iamservice/multitenancy/persistence/repository/CompetitionJpaRepository.java`

### Pruebas

- `src/test/java/com/solveria/iamservice/application/service/AdminCompetitionServiceTest.java`
- `src/test/java/com/solveria/iamservice/api/rest/AdminCompetitionControllerIT.java`

### Documentacion

- `docs/api/admin-competitions.md`
- `docs/README.md`
- `docs/changes/README.md`

## 7. Verificacion ejecutada

Comando ejecutado:

```bash
cd iam-service
./mvnw -Dtest=AdminCompetitionServiceTest,AdminCompetitionControllerIT test
```

Resultado esperado:

- `BUILD SUCCESS`

## 8. Siguiente paso recomendado

El siguiente paso correcto es `P1.7 Gestion de tenants participantes`.
