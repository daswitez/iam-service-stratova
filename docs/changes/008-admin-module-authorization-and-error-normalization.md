# Cambio 008 - Admin Module Authorization And Error Normalization

## 1. Resumen

Este cambio implementa `P1.4 Validaciones y autorizacion del modulo`.

Se endurece el modulo organizacional en dos capas:

- autorizacion declarativa sobre endpoints administrativos
- manejo de errores REST unificado con `ApiErrorResponse`

## 2. Objetivo

Evitar mutaciones no autorizadas y asegurar que el modulo responda errores consistentes en validacion, negocio, autenticacion y autorizacion.

## 3. Cambios principales

### 3.1 Autorizacion administrativa

- se habilita method security
- se agrega la anotacion `@PlatformAdminOnly`
- los controllers administrativos del modulo organizacional quedan protegidos de forma explicita
- `SecurityConfig` deja `/api/v1/admin/**` en `authenticated()` y la validacion fina vive en method security

Resultado:

- sin token: `401 UNAUTHORIZED`
- con token autenticado sin `PLATFORM_ADMIN`: `403 FORBIDDEN`
- con `PLATFORM_ADMIN`: acceso permitido

### 3.2 Normalizacion de errores

- se elimina el handler REST duplicado
- el modulo usa un unico contrato de error: `ApiErrorResponse`
- `AccessDeniedException` de Spring Security ahora se traduce correctamente a `FORBIDDEN`

Formato efectivo:

- `errorCode`
- `timestamp`
- `path`
- `details` cuando aplica validacion

## 4. Archivos principales tocados

### Codigo

- `src/main/java/com/solveria/iamservice/config/security/PlatformAdminOnly.java`
- `src/main/java/com/solveria/iamservice/config/security/SecurityConfig.java`
- `src/main/java/com/solveria/iamservice/api/exception/GlobalExceptionHandler.java`
- `src/main/java/com/solveria/iamservice/api/rest/AdminUserController.java`
- `src/main/java/com/solveria/iamservice/api/rest/AdminUniversityController.java`
- `src/main/java/com/solveria/iamservice/api/rest/AdminSubTenantController.java`
- `src/main/java/com/solveria/iamservice/api/rest/AdminMembershipController.java`

### Codigo removido

- `src/main/java/com/solveria/iamservice/api/rest/GlobalRestExceptionHandler.java`
- `src/main/java/com/solveria/iamservice/api/rest/dto/ErrorResponse.java`

### Pruebas

- `src/test/java/com/solveria/iamservice/api/rest/AdminUserControllerIT.java`
- `src/test/java/com/solveria/iamservice/api/rest/AdminUniversityControllerIT.java`
- `src/test/java/com/solveria/iamservice/api/rest/AdminSubTenantControllerIT.java`
- `src/test/java/com/solveria/iamservice/api/rest/AdminMembershipControllerIT.java`
- `src/test/java/com/solveria/iamservice/contract/AssignPermissionsToRoleContractTest.java`

## 5. Verificacion ejecutada

Comando ejecutado:

```bash
cd iam-service
./mvnw -Dtest=AdminUserControllerIT,AdminUniversityControllerIT,AdminSubTenantControllerIT,AdminMembershipControllerIT,AssignPermissionsToRoleContractTest test
```

Resultado:

- `BUILD SUCCESS`
- `23` tests pasando

## 6. Siguiente paso recomendado

El siguiente paso correcto es `P1.5 Documentacion del modulo`.
