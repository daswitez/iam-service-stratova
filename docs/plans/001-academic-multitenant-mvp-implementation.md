# Plan de Implementacion — Academic Multi-Tenant MVP

## 1. Objetivo

Definir e implementar el flujo minimo funcional para el modo academico multi-tenant de Solveria, cubriendo:

- registro de universidades y su estructura academica
- registro de docentes, jurados e inversores potenciales de una o multiples universidades
- creacion de competencias por endpoint
- asignacion de universidades a una competencia
- inscripcion de estudiantes y staff a una competencia
- creacion de equipos dentro de una competencia
- union de estudiantes a equipos
- estructura ejecutiva basica de los equipos
- configuracion base del producto o industria a simular
- capitalizacion inicial comun para todos los equipos

## 2. Alcance del MVP

### Incluido

- modelo multi-tenant academico-first en IAM
- gestion de tenants organizacionales
- memberships organizacionales
- contexto de competencia
- contexto de equipo
- reglas de acceso y asignacion
- CRUDs administrativos completos
- reglas base de formacion de equipos de 4 a 6 integrantes

### No incluido todavia

- logica del motor de simulacion
- scoring/rankings avanzados
- rubricas academicas
- evaluacion financiera real
- reportes y dashboards
- pagos/suscripciones

## 3. Decision de modelado central

### 3.1 Tenant no es equipo ni competencia

`Tenant` sigue representando una organizacion o sub-organizacion academica:

- `UNIVERSITY`
- `FACULTY`
- `PROGRAM`

Mas adelante tambien:

- `HOLDING`
- `ENTERPRISE`
- `STARTUP`

### 3.2 Competition y Team son contexto operativo, no tenant

La competencia es el espacio donde se participa.
El equipo es el grupo concreto con el que se compite.

Esto evita romper el modelo cuando:

- un estudiante cambia de equipo
- un docente es jurado en una competencia y mentor en otra
- la competencia es entre varias universidades

### 3.3 Los docentes/jurados/inversores pueden venir de multiples universidades

Esta restriccion cambia una parte importante del modelo:

- un docente no debe quedar amarrado a una sola universidad en la logica de competencia
- un docente puede tener una o varias `TenantMembership`
- al inscribirlo en una competencia, debemos registrar desde que tenant participa en esa competencia
- la competencia puede seleccionar staff desde varias universidades habilitadas

Entonces:

- el usuario vive en IAM como identidad global
- sus universidades/programas viven en `TenantMembership`
- su participacion en competencia vive en `CompetitionEnrollment`
- el enrollment debe guardar el `originTenantId` usado en esa competencia

### 3.4 El estudiante debe estar inscrito en la competencia antes de estar en un equipo

Esta es la decision correcta para el MVP.

#### Por que

- separa elegibilidad de asignacion de equipo
- permite estudiantes sin equipo temporalmente
- permite validar cupos antes de asignar
- permite reubicar estudiantes entre equipos
- permite auditar claramente quien estuvo habilitado para competir

#### Entonces el flujo correcto es

1. usuario existe en IAM y pertenece a una universidad/programa
2. usuario es inscrito en una competencia
3. usuario es asignado a un equipo dentro de esa competencia

## 4. Modelo de actores

### Organizacionales

- `UNIVERSITY_ADMIN`
- `PROGRAM_ADMIN`
- `PROFESSOR`
- `STUDENT`

### De competencia

- `COMPETITION_OWNER`
- `COMPETITION_MANAGER`
- `JUDGE`
- `INVESTOR`
- `MENTOR`
- `COMPETITOR`

### De equipo

- `TEAM_CAPTAIN`
- `TEAM_MEMBER`

## 5. Regla clave de roles

No mezclar en un solo lugar:

- rol organizacional
- rol en competencia
- rol en equipo

Ejemplo:

- una persona puede ser `PROFESSOR` en la universidad
- `PROFESSOR` tambien en otra universidad si tiene otra membership
- `JUDGE` en competencia A
- `MENTOR` en competencia B

Por eso necesitamos tres capas:

1. `TenantMembership`
2. `CompetitionEnrollment`
3. `TeamMembership`

## 6. Entidades del MVP

### 6.1 Organization / IAM

- `Tenant`
  - `id`
  - `code`
  - `name`
  - `type`
  - `parentTenantId`
  - `status`

- `User`
  - `id`
  - `username`
  - `email`
  - `password`
  - `userCategory`
  - `primaryTenantId`
  - `active`

- `TenantMembership`
  - `id`
  - `userId`
  - `tenantId`
  - `membershipType`
  - `status`
  - `isPrimary`

### 6.2 Competition domain

- `Competition`
  - `id`
  - `code`
  - `name`
  - `description`
  - `hostTenantId`
  - `scope`
  - `status`
  - `academicCycleId`
  - `productName`
  - `industryCode`
  - `industryName`
  - `initialCapitalAmount`
  - `currency`
  - `minTeamSize`
  - `maxTeamSize`
  - `teamCreationMode`
  - `roleAssignmentMethod`
  - `allowOptionalCoo`
  - `startsAt`
  - `endsAt`

- `CompetitionTenant`
  - `id`
  - `competitionId`
  - `tenantId`

- `CompetitionEnrollment`
  - `id`
  - `competitionId`
  - `userId`
  - `originTenantId`
  - `participantType`
  - `status`
  - `invitedByUserId`
  - `approvedAt`

`originTenantId` no significa que el usuario solo pertenezca a esa universidad.
Significa desde que membership entra a esa competencia concreta.

- `CompetitionStaffAssignment`
  - opcion A: modelar como `CompetitionEnrollment` con `participantType`
  - opcion B: tabla separada

Para el MVP recomiendo **una sola tabla `CompetitionEnrollment`** con `participantType`, porque reduce complejidad.

- `Team`
  - `id`
  - `competitionId`
  - `originTenantId`
  - `code`
  - `name`
  - `status`

- `TeamMembership`
  - `id`
  - `teamId`
  - `competitionEnrollmentId`
  - `teamRole`
  - `executiveRole`
  - `secondaryExecutiveRole`
  - `status`

## 6.3 Estructura organizacional del equipo

Cada equipo debe modelarse como una organizacion ejecutiva simplificada.

### Roles ejecutivos base

- `CEO`
- `CPO`
- `CTO`
- `CMO`
- `CFO`

### Rol ejecutivo opcional

- `COO`

### Regla recomendada para el MVP

- equipos de `5` integrantes: un integrante por cada rol base
- equipos de `6` integrantes: se habilita adicionalmente `COO`
- equipos de `4` integrantes: un integrante puede asumir dos roles ejecutivos

### Restriccion importante

- un estudiante sigue teniendo una sola `TeamMembership`
- los roles ejecutivos se modelan sobre esa misma membership
- `executiveRole` es obligatorio
- `secondaryExecutiveRole` es opcional y solo se usa en equipos de 4 integrantes

## 7. Estados y reglas

### 7.1 Competition status

- `DRAFT`
- `PUBLISHED`
- `ENROLLMENT_OPEN`
- `ENROLLMENT_CLOSED`
- `ACTIVE`
- `FINISHED`
- `ARCHIVED`

### 7.2 Enrollment status

- `INVITED`
- `APPROVED`
- `REJECTED`
- `WAITLISTED`
- `WITHDRAWN`

### 7.3 Team status

- `FORMING`
- `READY`
- `LOCKED`
- `ACTIVE`
- `FINISHED`

### 7.4 Reglas de tamano de equipo

- tamano ideal: entre `4` y `6` integrantes
- menos de `4` genera sobrecarga
- mas de `6` genera coordinacion deficiente y menor responsabilidad individual

Para el MVP, la competencia debe operar con:

- `minTeamSize = 4`
- `maxTeamSize = 6`

No recomiendo abrir configuracion libre en esta primera version.

## 8. Flujo funcional minimo

### 8.1 Registrar universidad

1. un admin plataforma crea la universidad
2. opcionalmente crea sub-tenants:
   - facultad
   - programa/carrera

### 8.2 Registrar docentes, jurados, inversores, estudiantes

1. el usuario se registra o es creado por admin
2. se le asigna una o varias memberships a universidad/programa
3. sigue siendo un usuario organizacional, no de competencia todavia

### 8.3 Crear competencia

1. un `UNIVERSITY_ADMIN`, `PROGRAM_ADMIN` o `COMPETITION_OWNER` crea la competencia
2. define:
   - nombre
   - descripcion
   - ciclo academico
   - industria o producto a simular
   - capital inicial comun
   - tamano minimo/maximo de equipo
   - metodo de asignacion de roles ejecutivos
   - universidades habilitadas

### 8.4 Habilitar universidades participantes

1. la competencia registra sus tenants habilitados en `CompetitionTenant`
2. esto permite competencias:
   - intra-universidad
   - inter-programa
   - interuniversitaria

### 8.5 Inscribir staff a la competencia

Docentes, jurados o inversores:

1. ya existen como usuarios y tienen tenant membership
2. pueden venir de multiples universidades habilitadas en la competencia
3. se crean como `CompetitionEnrollment`
3. `participantType` sera:
   - `JUDGE`
   - `INVESTOR`
   - `MENTOR`
   - `MANAGER`

No pertenecen a equipos.

Regla:

- el request debe indicar explicitamente el `originTenantId`
- ese `originTenantId` debe existir como membership activa del usuario
- ese `originTenantId` debe estar entre los tenants habilitados de la competencia

### 8.6 Inscribir estudiantes a la competencia

1. el estudiante ya existe en IAM
2. debe pertenecer a uno de los tenants habilitados por la competencia
3. se crea `CompetitionEnrollment` con:
   - `participantType = COMPETITOR`
   - `originTenantId`
4. todavia no pertenece a un equipo

Regla:

- estudiantes tambien pueden tener multiples memberships en IAM
- para competir, se debe elegir una sola `originTenantId` por enrollment
- en una misma competencia, un estudiante no puede tener dos enrollments activos como competidor

### 8.7 Crear equipos

Opciones MVP:

- `ADMIN_MANAGED`
  - el admin crea equipos y define cupos
- `SELF_MANAGED`
  - los propios estudiantes crean equipos dentro de los limites

Para el MVP recomiendo soportar ambos, pero implementar primero `ADMIN_MANAGED`.

Reglas de composicion:

- un equipo no puede operar fuera del rango `4-6`
- todo equipo debe cubrir `CEO`, `CPO`, `CTO`, `CMO` y `CFO`
- `COO` es opcional
- en equipos de 4, una persona puede cubrir un rol ejecutivo secundario
- en equipos de 5, cada integrante cubre exactamente un rol base
- en equipos de 6, el sexto integrante puede cubrir `COO`

### 8.8 Unir estudiantes a equipos

1. solo estudiantes con `CompetitionEnrollment` aprobado pueden entrar a un equipo
2. el equipo debe pertenecer a la misma competencia
3. el equipo no puede exceder `maxTeamSize`
4. el estudiante no puede pertenecer a mas de un equipo activo en la misma competencia
5. la asignacion debe respetar la estructura ejecutiva del equipo

### 8.9 Asignacion de roles dentro del equipo

Metodo recomendado para el MVP:

- `ADMIN_ASSIGNMENT`
  - un admin de competencia o de universidad participante define los roles

Metodos futuros:

- `DEMOCRATIC_ELECTION`
- `SELF_DECLARED`

Recomendacion:

- registrar el metodo elegido en la competencia
- implementar solo `ADMIN_ASSIGNMENT` en la primera version

## 9. Endpoint design recomendado y CRUD completo

## 9.1 Tenants

- `POST /api/v1/tenants/universities`
- `GET /api/v1/tenants/universities`
- `POST /api/v1/tenants/{tenantId}/children`
- `GET /api/v1/tenants/{tenantId}`
- `GET /api/v1/tenants/{tenantId}/children`
- `PATCH /api/v1/tenants/{tenantId}`
- `DELETE /api/v1/tenants/{tenantId}`

## 9.2 Users and memberships

- `POST /api/v1/auth/register`
- `POST /api/v1/tenant-memberships`
- `GET /api/v1/tenant-memberships/{membershipId}`
- `GET /api/v1/users/{userId}/tenant-memberships`
- `PATCH /api/v1/tenant-memberships/{membershipId}`
- `DELETE /api/v1/tenant-memberships/{membershipId}`
- `GET /api/v1/tenants/{tenantId}/members`

## 9.3 Competitions

- `POST /api/v1/competitions`
- `GET /api/v1/competitions`
- `GET /api/v1/competitions/{competitionId}`
- `PATCH /api/v1/competitions/{competitionId}`
- `DELETE /api/v1/competitions/{competitionId}`
- `POST /api/v1/competitions/{competitionId}/tenants`
- `GET /api/v1/competitions/{competitionId}/tenants`
- `DELETE /api/v1/competitions/{competitionId}/tenants/{tenantId}`

La competencia debe persistir tambien:

- `minTeamSize`
- `maxTeamSize`
- `roleAssignmentMethod`
- `allowOptionalCoo`

## 9.4 Competition enrollments

- `POST /api/v1/competitions/{competitionId}/enrollments/staff`
- `POST /api/v1/competitions/{competitionId}/enrollments/students`
- `GET /api/v1/competitions/{competitionId}/enrollments`
- `GET /api/v1/competitions/{competitionId}/enrollments/{enrollmentId}`
- `PATCH /api/v1/competitions/{competitionId}/enrollments/{enrollmentId}/status`
- `PATCH /api/v1/competitions/{competitionId}/enrollments/{enrollmentId}`
- `DELETE /api/v1/competitions/{competitionId}/enrollments/{enrollmentId}`

Tambien hace falta un endpoint de seleccion para staff multiuniversidad:

- `GET /api/v1/competitions/{competitionId}/eligible-staff`

Debe permitir filtrar por:

- `participantType`
- `tenantId`
- `userCategory`
- `search`

## 9.5 Teams

- `POST /api/v1/competitions/{competitionId}/teams`
- `GET /api/v1/competitions/{competitionId}/teams`
- `GET /api/v1/competitions/{competitionId}/teams/{teamId}`
- `PATCH /api/v1/competitions/{competitionId}/teams/{teamId}`
- `DELETE /api/v1/competitions/{competitionId}/teams/{teamId}`
- `POST /api/v1/competitions/{competitionId}/teams/{teamId}/members`
- `GET /api/v1/competitions/{competitionId}/teams/{teamId}/members`
- `PATCH /api/v1/competitions/{competitionId}/teams/{teamId}/members/{teamMembershipId}`
- `DELETE /api/v1/competitions/{competitionId}/teams/{teamId}/members/{teamMembershipId}`
- `PATCH /api/v1/competitions/{competitionId}/teams/{teamId}/members/{teamMembershipId}/executive-role`

## 10. Request payload minimo por endpoint

### 10.1 Crear universidad

```json
{
  "code": "umsa",
  "name": "Universidad Mayor de San Andres"
}
```

### 10.2 Crear sub-tenant de programa

```json
{
  "code": "adm-sis-umsa",
  "name": "Administracion de Sistemas UMSA",
  "type": "PROGRAM"
}
```

### 10.3 Crear competencia

```json
{
  "code": "coresim-2026-s1",
  "name": "CoreSim 2026 Semestre 1",
  "description": "Competencia academica de simulacion empresarial",
  "hostTenantId": "tenant-uuid",
  "scope": "CROSS_TENANT",
  "academicCycleId": "cycle-uuid",
  "productName": "Bebida funcional",
  "industryCode": "food-beverage",
  "industryName": "Alimentos y bebidas",
  "initialCapitalAmount": 100000,
  "currency": "USD",
  "minTeamSize": 4,
  "maxTeamSize": 6,
  "teamCreationMode": "ADMIN_MANAGED",
  "roleAssignmentMethod": "ADMIN_ASSIGNMENT",
  "allowOptionalCoo": true,
  "startsAt": "2026-04-01T00:00:00Z",
  "endsAt": "2026-06-30T23:59:59Z"
}
```

### 10.4 Inscribir staff

```json
{
  "userId": 10,
  "originTenantId": "tenant-uuid",
  "participantType": "JUDGE"
}
```

`originTenantId` debe ser una universidad o programa al que el docente realmente pertenezca.
No se infiere automaticamente si el usuario tiene varias memberships.

### 10.5 Inscribir estudiante

```json
{
  "userId": 21,
  "originTenantId": "program-tenant-uuid",
  "participantType": "COMPETITOR"
}
```

### 10.6 Crear equipo

```json
{
  "originTenantId": "program-tenant-uuid",
  "code": "team-andes",
  "name": "Team Andes"
}
```

### 10.7 Anadir miembro a equipo

```json
{
  "competitionEnrollmentId": "enrollment-uuid",
  "teamRole": "TEAM_MEMBER",
  "executiveRole": "CTO"
}
```

### 10.8 Actualizar rol ejecutivo de miembro

```json
{
  "executiveRole": "CEO",
  "secondaryExecutiveRole": "CMO"
}
```

## 10.9 Listar staff elegible multiuniversidad

Ejemplo:

`GET /api/v1/competitions/{competitionId}/eligible-staff?participantType=JUDGE&tenantId=tenant-uuid&search=perez`

## 11. Validaciones obligatorias

### Tenant

- `code` unico
- jerarquia valida
- no ciclos en la jerarquia

### Competition

- `hostTenantId` debe existir
- `academicCycleId` debe existir
- `initialCapitalAmount > 0`
- `minTeamSize = 4`
- `maxTeamSize = 6`
- `roleAssignmentMethod` debe existir
- `allowOptionalCoo` define si el sexto integrante puede ocupar `COO`

### Enrollment

- el usuario debe existir
- el usuario debe tener membership en un tenant habilitado para esa competencia
- no duplicar enrollment activo por competencia
- el `originTenantId` debe coincidir con una membership activa del usuario
- un docente con multiples memberships debe elegir explicitamente con cual participa
- un mismo usuario puede tener distintos enrollments en distintas competencias
- en la misma competencia no puede tener enrollments activos incompatibles

Regla recomendada para el MVP:

- permitir un solo enrollment activo por usuario por competencia
- permitir cambiar `participantType` solo por endpoint administrativo de update

### Team

- el `originTenantId` debe estar habilitado en la competencia
- `code` unico dentro de la competencia
- el equipo no puede declararse listo si no cubre los roles ejecutivos requeridos

### TeamMembership

- enrollment debe pertenecer a la misma competencia del equipo
- enrollment debe ser `COMPETITOR`
- un competidor no puede estar en dos equipos activos de la misma competencia
- no exceder `maxTeamSize`
- `executiveRole` es obligatorio
- no puede haber dos `CEO` en el mismo equipo
- `secondaryExecutiveRole` solo se permite cuando el equipo tiene 4 integrantes
- si el equipo tiene 5 integrantes, deben cubrirse exactamente `CEO`, `CPO`, `CTO`, `CMO`, `CFO`
- si el equipo tiene 6 integrantes, el sexto rol permitido es `COO`

## 12. JWT y contexto IAM

El JWT no debe exponer solo `tenantId`.
Debe incluir:

- `primaryTenantId`
- `activeTenantId`
- `tenantIds`
- `competitionIds`
- `teamIds`
- `roles`
- `tenantMemberships`
- `competitionEnrollments`

Regla:

- permisos organizacionales se validan contra tenant membership
- permisos operativos se validan contra competition enrollment o team membership

## 13. Orden de implementacion recomendado

### Fase 1

- endpoint crear universidad
- endpoint listar/editar/eliminar universidad
- endpoint crear sub-tenant
- endpoint listar/editar/eliminar sub-tenant
- endpoint asignar membership organizacional
- endpoint listar/editar/eliminar membership

### Fase 2

- endpoint crear competencia
- endpoint listar/editar/eliminar competencia
- endpoint asignar tenants a competencia
- endpoint quitar tenant de competencia

### Fase 3

- endpoint inscribir staff
- endpoint listar staff elegible multiuniversidad
- endpoint inscribir estudiantes
- endpoint listar/editar/eliminar enrollments

### Fase 4

- endpoint crear equipos
- endpoint editar/eliminar equipos
- endpoint anadir miembros a equipo
- endpoint asignar/editar roles ejecutivos de miembros
- endpoint listar/editar/eliminar miembros de equipo
- endpoint listar competencia, inscriptos y equipos

### Fase 5

- contexto JWT mas rico
- autorizacion por competencia y equipo

## 14. Tabla de ownership por bounded context

### IAM Service

Debe ser duenio de:

- tenants
- users
- memberships
- competitions
- competition enrollments
- teams
- team memberships
- claims JWT

### Fuera de IAM por ahora

- estado de simulacion
- rondas o turnos de simulacion
- KPIs
- ranking
- scoring de inversion

## 15. Riesgos si modelamos mal esto

- si metemos equipo como tenant, se rompe el modelo
- si no existe enrollment de competencia, no se puede auditar participacion
- si jurados e inversores se modelan como miembros de equipo, se mezcla lectura con juego
- si un docente multiuniversidad no selecciona explicitamente su `originTenantId`, la auditoria queda ambigua
- si no hay CRUD completo, la operacion diaria queda bloqueada ante errores administrativos
- si no se modelan roles ejecutivos, no se podra evaluar responsabilidad real dentro del equipo
- si permitimos tamanos fuera de `4-6`, el formato academico pierde consistencia
- si la competencia no define industria/producto/capital al crearse, cada equipo arrancara con reglas distintas

## 16. Implementacion concreta recomendada

### Decision final para el MVP

- estudiantes: **si** deben estar inscritos directamente en la competencia
- luego se unen a equipos
- docentes/jurados/inversores: tambien deben estar inscritos en la competencia, pero como staff
- docentes/jurados/inversores pueden venir de multiples universidades habilitadas
- por eso el enrollment de staff debe guardar el `originTenantId` explicito
- los equipos existen dentro de la competencia
- el rango operativo del equipo para el MVP es `4-6`
- la estructura ejecutiva base del equipo es `CEO`, `CPO`, `CTO`, `CMO`, `CFO`
- `COO` es opcional para equipos de 6
- en equipos de 4 se permite un rol ejecutivo secundario en una sola persona
- la competencia define:
  - producto
  - industria
  - capital inicial comun
  - limites de equipo
  - metodo de asignacion de roles

## 17. Entregable tecnico esperado despues de este plan

Cuando implementemos hasta aqui, ya deberiamos poder hacer este flujo completo por API:

1. crear universidad
2. crear programa
3. registrar docente
4. registrar estudiante
5. crear competencia
6. habilitar tenants participantes
7. consultar staff elegible desde multiples universidades participantes
8. asignar jurados/inversores/docentes a la competencia
9. inscribir estudiantes a la competencia
10. crear equipos
11. anadir estudiantes a equipos
12. asignar roles ejecutivos del equipo
13. hacer login y recibir contexto correcto de tenant + competencia + equipo

Ese es el punto exacto donde el IAM multi-tenant queda funcional para el MVP academico, incluso antes de entrar al motor de simulacion.
