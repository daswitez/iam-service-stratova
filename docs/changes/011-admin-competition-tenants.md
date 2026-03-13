# Cambio 011 - Admin Competition Participant Tenants

## 1. Resumen

Este cambio implementa `P1.7 Gestion de tenants participantes`.

Se agrega administracion REST para:

- agregar tenant participante a una competencia
- listar tenants participantes de una competencia
- quitar tenant participante

## 2. Objetivo

Permitir que el `PLATFORM_ADMIN` habilite universidades y programas en una competencia antes de abrir enrollments multiuniversidad.

## 3. Endpoints nuevos

- `POST /api/v1/admin/competitions/{competitionId}/tenants`
- `GET /api/v1/admin/competitions/{competitionId}/tenants`
- `DELETE /api/v1/admin/competitions/{competitionId}/tenants/{tenantId}`

## 4. Reglas de negocio

- la competencia debe existir
- el tenant participante debe existir y estar activo
- no se permite duplicar la misma participacion
- la relacion se elimina fisicamente de `iam_competition_tenant`

## 5. Ejemplos curl

### Agregar tenant participante

```bash
curl -X POST http://localhost:8080/api/v1/admin/competitions/$COMPETITION_ID/tenants \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": "22222222-2222-2222-2222-222222222222"
  }'
```

### Listar tenants participantes

```bash
curl -X GET http://localhost:8080/api/v1/admin/competitions/$COMPETITION_ID/tenants \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

### Quitar tenant participante

```bash
curl -X DELETE http://localhost:8080/api/v1/admin/competitions/$COMPETITION_ID/tenants/22222222-2222-2222-2222-222222222222 \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

## 6. Archivos principales tocados

### Codigo

- `src/main/java/com/solveria/iamservice/api/rest/AdminCompetitionTenantController.java`
- `src/main/java/com/solveria/iamservice/application/service/AdminCompetitionTenantService.java`
- `src/main/java/com/solveria/iamservice/api/rest/dto/AdminCompetitionTenantCreateRequest.java`
- `src/main/java/com/solveria/iamservice/api/rest/dto/AdminCompetitionTenantResponse.java`
- `src/main/java/com/solveria/iamservice/multitenancy/persistence/repository/CompetitionTenantJpaRepository.java`
- `src/main/java/com/solveria/iamservice/multitenancy/persistence/entity/CompetitionTenantJpaEntity.java`

### Pruebas

- `src/test/java/com/solveria/iamservice/application/service/AdminCompetitionTenantServiceTest.java`
- `src/test/java/com/solveria/iamservice/api/rest/AdminCompetitionTenantControllerIT.java`

### Documentacion

- `docs/api/admin-competition-tenants.md`
- `docs/README.md`
- `docs/changes/README.md`

## 7. Verificacion ejecutada

Comando ejecutado:

```bash
cd iam-service
./mvnw -Dtest=AdminCompetitionTenantServiceTest,AdminCompetitionTenantControllerIT test
```

Resultado esperado:

- `BUILD SUCCESS`

## 8. Siguiente paso recomendado

El siguiente paso correcto es `P1.8 Busquedas y filtros`.
