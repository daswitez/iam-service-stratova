# Cambio 007 - Admin Memberships CRUD

## 1. Resumen

Este cambio implementa `P1.3 CRUD de memberships`.

Se agrega administracion REST para:

- asignar usuarios a tenants
- listar memberships por usuario
- listar memberships por tenant
- editar estado y primaria
- desactivar memberships

## 2. Objetivo

Permitir que el `PLATFORM_ADMIN` asigne usuarios a una o varias instituciones sin romper la regla de primaria unica activa.

## 3. Endpoints nuevos

- `POST /api/v1/admin/memberships`
- `GET /api/v1/admin/memberships/{id}`
- `GET /api/v1/admin/users/{userId}/memberships`
- `GET /api/v1/admin/tenants/{tenantId}/memberships`
- `PUT /api/v1/admin/memberships/{id}`
- `DELETE /api/v1/admin/memberships/{id}`

## 4. Reglas de negocio

- un usuario puede tener multiples memberships
- la API expone `isPrimary`, pero la persistencia sigue usando `membershipType`
- solo existe una membership `PRIMARY` activa por usuario
- si la primaria activa cambia o se desactiva, el servicio reequilibra la primaria activa restante
- `DELETE` hace baja logica con `status=LEFT`
- `iam_user.tenant_id` se sincroniza con la primaria activa para compatibilidad

## 5. Ejemplos curl

### Crear membership primaria

```bash
curl -X POST http://localhost:8080/api/v1/admin/memberships \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 10,
    "tenantId": "11111111-1111-1111-1111-111111111111",
    "status": "ACTIVE",
    "isPrimary": true
  }'
```

### Listar memberships de un usuario

```bash
curl -X GET http://localhost:8080/api/v1/admin/users/10/memberships \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

### Desactivar membership

```bash
curl -X DELETE http://localhost:8080/api/v1/admin/memberships/<MEMBERSHIP_ID> \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

## 6. Archivos principales tocados

### Codigo

- `src/main/java/com/solveria/iamservice/application/service/AdminMembershipService.java`
- `src/main/java/com/solveria/iamservice/api/rest/AdminMembershipController.java`
- `src/main/java/com/solveria/iamservice/api/rest/dto/AdminMembershipCreateRequest.java`
- `src/main/java/com/solveria/iamservice/api/rest/dto/AdminMembershipUpdateRequest.java`
- `src/main/java/com/solveria/iamservice/api/rest/dto/AdminMembershipResponse.java`
- `src/main/java/com/solveria/iamservice/multitenancy/persistence/repository/UserTenantMembershipJpaRepository.java`
- `src/main/java/com/solveria/iamservice/config/security/LocalJwtAuthenticationFilter.java`
- `src/main/java/com/solveria/iamservice/config/security/SecurityConfig.java`

### Pruebas

- `src/test/java/com/solveria/iamservice/application/service/AdminMembershipServiceTest.java`
- `src/test/java/com/solveria/iamservice/api/rest/AdminMembershipControllerIT.java`

## 7. Verificacion ejecutada

Comando ejecutado:

```bash
cd iam-service
./mvnw -Dtest=AdminMembershipServiceTest,AdminMembershipControllerIT test
```

Resultado:

- `BUILD SUCCESS`
- `6` tests pasando

## 8. Siguiente paso recomendado

El siguiente paso correcto es `P1.4 Validaciones y autorizacion del modulo`.
