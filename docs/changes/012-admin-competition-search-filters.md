# Cambio 012 - Admin Competition Search Filters

## 1. Resumen

Este cambio implementa `P1.8 Busquedas y filtros`.

Se amplian los filtros administrativos del listado de competencias para soportar:

- filtro por estado
- filtro por tenant host
- filtro por ciclo academico
- combinacion de filtros en una sola consulta

## 2. Objetivo

Dejar el listado administrativo de competencias utilizable para frontend y operaciones sin obligar a descargar todo el catalogo y filtrar del lado cliente.

## 3. Endpoint afectado

- `GET /api/v1/admin/competitions`

Nuevos query params soportados:

- `status`
- `hostTenantId`
- `cycle`

## 4. Reglas de negocio

- `status` sigue validando contra `CompetitionStatus`
- `hostTenantId` debe ser un UUID valido cuando se envia
- `cycle` compara contra `iam_academic_cycle.code` de forma case-insensitive
- si una competencia no tiene `academic_cycle_id`, no aparece cuando se filtra por `cycle`
- al crear o actualizar una competencia, el servicio intenta vincular automaticamente un `iam_academic_cycle` del tenant host cuando el rango `startsAt` / `endsAt` queda contenido en ese ciclo

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

### Crear competencia que cae dentro de un ciclo existente

Precondicion:

- ya existe un `iam_academic_cycle` para el tenant host, por ejemplo `2026-S1`

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

### Listar por estado

```bash
curl -X GET "http://localhost:8080/api/v1/admin/competitions?status=ACTIVE" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

### Listar por tenant host

```bash
curl -X GET "http://localhost:8080/api/v1/admin/competitions?hostTenantId=11111111-1111-1111-1111-111111111111" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

### Listar por ciclo academico

```bash
curl -X GET "http://localhost:8080/api/v1/admin/competitions?cycle=2026-S1" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

### Listar con filtros combinados

```bash
curl -X GET "http://localhost:8080/api/v1/admin/competitions?status=ACTIVE&hostTenantId=11111111-1111-1111-1111-111111111111&cycle=2026-S1" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

## 6. Archivos principales tocados

### Codigo

- `src/main/java/com/solveria/iamservice/api/rest/AdminCompetitionController.java`
- `src/main/java/com/solveria/iamservice/application/service/AdminCompetitionService.java`
- `src/main/java/com/solveria/iamservice/multitenancy/persistence/repository/AcademicCycleJpaRepository.java`
- `src/main/java/com/solveria/iamservice/multitenancy/persistence/entity/AcademicCycleJpaEntity.java`

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

El siguiente paso correcto es `P2.1 Enrollment de staff multiuniversidad`.
