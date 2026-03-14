# Cambio 013 - Competition Staff Enrollments

## 1. Resumen

Este cambio implementa `P2.1 Enrollment de staff multiuniversidad`.

Se agregan endpoints REST para:

- inscribir staff en una competencia
- ver detalle de un enrollment
- actualizar el estado del enrollment
- retirar logicamente un enrollment

## 2. Objetivo

Permitir que el sistema inscriba jurados, mentores, inversores y managers desde multiples universidades o programas, dejando trazabilidad explicita con `originTenantId`.

## 3. Endpoints nuevos

- `POST /api/v1/competitions/{competitionId}/enrollments/staff`
- `GET /api/v1/competitions/{competitionId}/enrollments/{enrollmentId}`
- `PATCH /api/v1/competitions/{competitionId}/enrollments/{enrollmentId}/status`
- `DELETE /api/v1/competitions/{competitionId}/enrollments/{enrollmentId}`

## 4. Reglas de negocio

- el usuario debe existir
- el `originTenantId` debe existir
- el usuario debe tener membership activa en el `originTenantId`
- el `originTenantId` debe estar habilitado para la competencia
- el tenant host tambien se considera habilitado como origen
- no se permite duplicar `competitionId + userId`
- `participantType` permitido para este endpoint: `JUDGE`, `INVESTOR`, `MENTOR`, `MANAGER`
- `DELETE` hace baja logica con `status=WITHDRAWN`
- al mover el enrollment a `APPROVED`, se completa `approvedAt`

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

Guardar variables:

```bash
export ADMIN_TOKEN="<JWT>"
export COMPETITION_ID="<COMPETITION_UUID>"
export ENROLLMENT_ID="<ENROLLMENT_UUID>"
```

### Inscribir jurado

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

### Ver enrollment

```bash
curl -X GET http://localhost:8080/api/v1/competitions/$COMPETITION_ID/enrollments/$ENROLLMENT_ID \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

### Aprobar enrollment

```bash
curl -X PATCH http://localhost:8080/api/v1/competitions/$COMPETITION_ID/enrollments/$ENROLLMENT_ID/status \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "APPROVED"
  }'
```

### Retirar enrollment

```bash
curl -X DELETE http://localhost:8080/api/v1/competitions/$COMPETITION_ID/enrollments/$ENROLLMENT_ID \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

## 6. Archivos principales tocados

### Codigo

- `src/main/java/com/solveria/iamservice/api/rest/CompetitionEnrollmentController.java`
- `src/main/java/com/solveria/iamservice/application/service/CompetitionEnrollmentService.java`
- `src/main/java/com/solveria/iamservice/api/rest/dto/CompetitionStaffEnrollmentCreateRequest.java`
- `src/main/java/com/solveria/iamservice/api/rest/dto/CompetitionEnrollmentStatusUpdateRequest.java`
- `src/main/java/com/solveria/iamservice/api/rest/dto/CompetitionEnrollmentResponse.java`
- `src/main/java/com/solveria/iamservice/multitenancy/persistence/entity/CompetitionEnrollmentJpaEntity.java`
- `src/main/java/com/solveria/iamservice/multitenancy/persistence/entity/CompetitionParticipantType.java`
- `src/main/java/com/solveria/iamservice/multitenancy/persistence/entity/CompetitionEnrollmentStatus.java`
- `src/main/java/com/solveria/iamservice/multitenancy/persistence/repository/CompetitionEnrollmentJpaRepository.java`
- `src/main/java/com/solveria/iamservice/config/security/LocalJwtAuthenticationFilter.java`
- `src/main/java/com/solveria/iamservice/config/security/SecurityConfig.java`

### Pruebas

- `src/test/java/com/solveria/iamservice/application/service/CompetitionEnrollmentServiceTest.java`
- `src/test/java/com/solveria/iamservice/api/rest/CompetitionEnrollmentControllerIT.java`

### Documentacion

- `docs/api/competition-enrollments.md`
- `docs/README.md`
- `docs/changes/README.md`

## 7. Verificacion ejecutada

Comando ejecutado:

```bash
cd iam-service
./mvnw -Dtest=CompetitionEnrollmentServiceTest,CompetitionEnrollmentControllerIT test
```

Resultado esperado:

- `BUILD SUCCESS`

## 8. Siguiente paso recomendado

El siguiente paso correcto es `P2.2 Endpoint de staff elegible`.
