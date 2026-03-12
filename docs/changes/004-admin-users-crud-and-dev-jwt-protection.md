# Cambio 004 - Admin Users CRUD y proteccion JWT local en DEV

## 1. Resumen

Este cambio abre `P1.1.1 Administracion de usuarios`.

Se implementaron dos piezas en conjunto:

- CRUD administrativo de usuarios en `/api/v1/admin/users`
- proteccion de `/api/v1/admin/**` con JWT local en `dev`

Con esto, los endpoints administrativos nuevos ya no quedan expuestos por el `permitAll` del modo de desarrollo.

## 2. Objetivo

Habilitar que el `PLATFORM_ADMIN` cree y gestione cuentas de usuarios y administradores, manteniendo control de acceso real desde ahora.

## 3. Cambios tecnicos

### 3.1 Seguridad

Se agrego validacion local del token emitido por `iam-service` para rutas administrativas:

- `LocalJwtAuthenticationFilter`
- `JwtAuthorityExtractor`
- `JwtService.parseClaims(...)`

Regla actual:

- `/api/v1/admin/**` requiere `ROLE_PLATFORM_ADMIN`
- en `dev`, el resto de `/api/**` sigue abierto temporalmente
- en modo JWT externo, tambien se mapea el claim `roles` a autoridades `ROLE_*`

### 3.2 CRUD administrativo

Se agregaron endpoints:

- `POST /api/v1/admin/users`
- `GET /api/v1/admin/users`
- `GET /api/v1/admin/users/{id}`
- `PUT /api/v1/admin/users/{id}`
- `DELETE /api/v1/admin/users/{id}`

El borrado es logico:

- no elimina la fila
- cambia `active=false`

### 3.3 Reglas de negocio aplicadas

- email unico a nivel aplicacion
- username unico a nivel aplicacion
- solo `ACADEMIC_ADMIN` puede recibir rol persistido `PLATFORM_ADMIN`
- no se puede desactivar ni degradar al ultimo `PLATFORM_ADMIN` activo
- si se informa `primaryTenantId`, se sincroniza la membership primaria

## 4. Endpoints afectados

### 4.1 `POST /api/v1/auth/login`

Se mantiene igual funcionalmente, pero ahora su token tambien sirve para administrar usuarios en `dev`.

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@solveria.local",
    "password": "Admin12345!"
  }'
```

### 4.2 `POST /api/v1/admin/users`

```bash
curl -X POST http://localhost:8080/api/v1/admin/users \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "maria.admin",
    "email": "maria.admin@solveria.local",
    "password": "SecurePass123",
    "userCategory": "ACADEMIC_ADMIN",
    "primaryTenantId": null,
    "roleNames": ["PLATFORM_ADMIN"]
  }'
```

### 4.3 `GET /api/v1/admin/users`

```bash
curl -X GET http://localhost:8080/api/v1/admin/users \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

### 4.4 `PUT /api/v1/admin/users/{id}`

```bash
curl -X PUT http://localhost:8080/api/v1/admin/users/2 \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "maria.admin",
    "email": "maria.admin@solveria.local",
    "password": "",
    "userCategory": "ACADEMIC_ADMIN",
    "active": true,
    "primaryTenantId": null,
    "roleNames": ["PLATFORM_ADMIN"]
  }'
```

### 4.5 `DELETE /api/v1/admin/users/{id}`

```bash
curl -X DELETE http://localhost:8080/api/v1/admin/users/2 \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

## 5. Archivos principales tocados

### Codigo

- `src/main/java/com/solveria/iamservice/config/security/LocalJwtAuthenticationFilter.java`
- `src/main/java/com/solveria/iamservice/config/security/JwtAuthorityExtractor.java`
- `src/main/java/com/solveria/iamservice/config/security/JwtService.java`
- `src/main/java/com/solveria/iamservice/config/security/SecurityConfig.java`
- `src/main/java/com/solveria/iamservice/application/service/AdminUserService.java`
- `src/main/java/com/solveria/iamservice/api/rest/AdminUserController.java`
- `src/main/java/com/solveria/iamservice/api/rest/dto/AdminUserCreateRequest.java`
- `src/main/java/com/solveria/iamservice/api/rest/dto/AdminUserUpdateRequest.java`
- `src/main/java/com/solveria/iamservice/api/rest/dto/AdminUserResponse.java`

### Repositorios

- `../core-plataform/core-platform/src/main/java/com/solveria/core/iam/infrastructure/persistence/repository/UserJpaRepository.java`
- `src/main/java/com/solveria/iamservice/multitenancy/persistence/repository/UserTenantMembershipJpaRepository.java`
- `src/main/java/com/solveria/iamservice/multitenancy/persistence/entity/UserTenantMembershipJpaEntity.java`

### Pruebas

- `src/test/java/com/solveria/iamservice/api/rest/AdminUserControllerIT.java`
- `src/test/java/com/solveria/iamservice/application/service/AdminUserServiceTest.java`

## 6. Verificacion ejecutada

Comandos ejecutados:

```bash
cd core-plataform
./mvnw -pl core-platform -am install -DskipTests
```

```bash
cd iam-service
./mvnw test
```

Resultado:

- `BUILD SUCCESS`
- `20` tests pasando

## 7. Documentacion relacionada

- `docs/api/admin-users.md`
- `docs/changes/001-platform-admin-bootstrap-and-auth-hardening.md`

## 8. Lo que sigue

El siguiente paso correcto es `P1.1 CRUD de universidades` y despues `P1.2 CRUD de sub-tenants`.
