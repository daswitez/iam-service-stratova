# Cambio 006 - Admin Sub-Tenants CRUD

## 1. Resumen

Este cambio implementa `P1.2 CRUD de sub-tenants`.

Se agrega administracion REST para:

- facultades
- programas

usando el mismo `iam_tenant` del modelo multi-tenant.

## 2. Objetivo

Permitir que el `PLATFORM_ADMIN` modele la estructura academica real antes de pasar a memberships, competencias y equipos.

## 3. Endpoints nuevos

- `POST /api/v1/admin/tenants/{parentId}/children`
- `GET /api/v1/admin/tenants/{parentId}/children`
- `GET /api/v1/admin/sub-tenants/{id}`
- `PUT /api/v1/admin/sub-tenants/{id}`
- `DELETE /api/v1/admin/sub-tenants/{id}`

## 4. Reglas de negocio

- solo se permiten tipos `FACULTY` y `PROGRAM`
- jerarquia valida:
  - `UNIVERSITY -> FACULTY`
  - `FACULTY -> PROGRAM`
- no se permite `PROGRAM` directo bajo `UNIVERSITY`
- `code` es unico y se normaliza a lowercase
- `DELETE` hace baja logica con `status=INACTIVE`

## 5. Ejemplos curl

### Crear facultad

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

### Crear programa

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

### Listar hijos

```bash
curl -X GET http://localhost:8080/api/v1/admin/tenants/<PARENT_ID>/children \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

## 6. Archivos principales tocados

### Codigo

- `src/main/java/com/solveria/iamservice/application/service/AdminSubTenantService.java`
- `src/main/java/com/solveria/iamservice/api/rest/AdminSubTenantController.java`
- `src/main/java/com/solveria/iamservice/api/rest/dto/AdminSubTenantCreateRequest.java`
- `src/main/java/com/solveria/iamservice/api/rest/dto/AdminSubTenantUpdateRequest.java`
- `src/main/java/com/solveria/iamservice/api/rest/dto/AdminSubTenantResponse.java`
- `src/main/java/com/solveria/iamservice/multitenancy/persistence/entity/TenantJpaEntity.java`
- `src/main/java/com/solveria/iamservice/multitenancy/persistence/repository/TenantJpaRepository.java`

### Pruebas

- `src/test/java/com/solveria/iamservice/application/service/AdminSubTenantServiceTest.java`
- `src/test/java/com/solveria/iamservice/api/rest/AdminSubTenantControllerIT.java`

## 7. Verificacion ejecutada

Comando ejecutado:

```bash
cd iam-service
./mvnw test
```

Resultado:

- `BUILD SUCCESS`
- `24` tests pasando

## 8. Documentacion relacionada

- `docs/api/admin-sub-tenants.md`
- `docs/api/admin-universities.md`

## 9. Siguiente paso recomendado

El siguiente paso correcto es `P1.3 CRUD de memberships`.
