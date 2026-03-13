# Backlog Priorizado - Academic Multi-Tenant MVP

## 1. Objetivo

Este backlog ordena el trabajo de implementacion en la secuencia correcta.
La prioridad sigue dependencias reales entre modulos, no conveniencia aislada.

## 2. Convencion de prioridad

- `P0` bloqueante de arquitectura
- `P1` funcionalidad base obligatoria
- `P2` funcionalidad operativa del MVP
- `P3` endurecimiento, escalabilidad y calidad

## 3. Orden maestro

1. cerrar el modelo de dominio
2. construir persistencia y migraciones
3. exponer CRUD de organizacion
4. exponer CRUD de competencia
5. exponer enrollments
6. exponer equipos y roles ejecutivos
7. endurecer JWT y autorizacion
8. cerrar documentacion y pruebas

## 4. P0 - Fundacion de dominio y datos

### P0.0 Modelo de acceso administrativo

Objetivo:

- cerrar la seguridad base antes de abrir CRUDs

Tareas:

- definir rol `PLATFORM_ADMIN`
- definir estrategia de bootstrap inicial
- decidir si el bootstrap sera por migracion, seed o variable segura
- deshabilitar registro publico abierto
- definir endpoints administrativos de gestion de usuarios

Salida esperada:

- modelo de acceso administrativo cerrado y sin alta publica

### P0.1 Revisar esquema actual vs esquema objetivo

Objetivo:

- confirmar que el modelo actual implementado en `iam-service` sigue alineado con el plan academico

Tareas:

- auditar entidades nuevas de multitenancy
- verificar nombres de tablas y columnas
- detectar huecos: enrollments, teams, executive roles, competition settings
- decidir si `TeamMembership` necesita nuevos campos o tabla auxiliar

Salida esperada:

- lista final de entidades y enums del MVP

### P0.2 Cerrar modelo relacional final del MVP

Objetivo:

- dejar definido el contrato de base de datos antes de abrir CRUDs

Tareas:

- definir tablas faltantes
- definir constraints unicos
- definir foreign keys
- definir indices por busqueda y joins criticos
- definir enums o columnas de estado

Salida esperada:

- especificacion lista para migracion
- documento de referencia: `D-relational-model-contract-mvp.md`

### P0.3 Crear migraciones complementarias

Objetivo:

- completar la base del modelo academico multi-tenant

Tareas:

- agregar tablas faltantes
- agregar columnas de roles ejecutivos
- agregar columnas de configuracion de competencia
- agregar indices para:
  - `competition_id`
  - `origin_tenant_id`
  - `user_id`
  - `status`

Salida esperada:

- nuevas migraciones Flyway ejecutables en Postgres real

### P0.4 Prueba de arranque real con migraciones

Objetivo:

- validar que el servicio arranca con el esquema real

Tareas:

- levantar Postgres local
- correr la aplicacion con perfil `dev`
- validar Flyway
- validar que no hay conflictos con H2 en tests

Salida esperada:

- evidencia de arranque limpio con schema real
- evidencia documentada en `docs/changes/003-p0-4-real-startup-validation.md`

## 5. P1 - Modulo Organizacion

### P1.1 CRUD de universidades

Objetivo:

- crear, listar, editar y desactivar universidades

Tareas:

- DTOs request/response
- controller REST
- servicio de aplicacion
- validaciones de `code` unico
- pruebas unitarias y de integracion

Dependencias:

- P0 completo

### P1.1.1 Administracion de usuarios

Objetivo:

- permitir que solo administradores autenticados creen cuentas

Tareas:

- endpoint `POST /api/v1/admin/users`
- detalle de usuario
- update de usuario
- baja logica de usuario
- creacion de otros administradores

Reglas:

- no exponer `auth/register` publico
- solo roles administrativos autorizados pueden crear cuentas
- el `PLATFORM_ADMIN` puede crear otros administradores

### P1.2 CRUD de sub-tenants

Objetivo:

- soportar facultades y programas

Tareas:

- crear endpoint hijo por tenant
- validar jerarquia permitida
- listar hijos
- editar y desactivar hijos

Reglas:

- `UNIVERSITY -> FACULTY -> PROGRAM`
- no permitir jerarquias invalidas

### P1.3 CRUD de memberships

Objetivo:

- asignar usuarios a una o varias instituciones

Tareas:

- crear membership
- listar memberships por usuario
- listar miembros por tenant
- editar tipo o estado
- eliminar o desactivar membership
- definir `isPrimary`

Reglas:

- un usuario puede tener multiples memberships
- solo una debe ser primaria

### P1.4 Validaciones y autorizacion del modulo

Objetivo:

- evitar mutaciones no autorizadas

Tareas:

- proteger endpoints de tenant
- proteger endpoints administrativos de usuarios
- validar permisos administrativos
- normalizar errores

### P1.5 Documentacion del modulo

Objetivo:

- dejar el modulo listo para uso por terceros

Tareas:

- documentar endpoints
- actualizar ejemplos de prueba
- actualizar setup si cambia el flujo local

## 6. P1 - Modulo Competencia

### P1.6 CRUD de competencias

Objetivo:

- crear competencias con reglas base de simulacion

Tareas:

- crear endpoint de alta
- listar competencias
- ver detalle
- editar
- desactivar o archivar

Campos obligatorios:

- nombre
- codigo
- host tenant
- producto
- industria
- capital inicial
- `minTeamSize = 4`
- `maxTeamSize = 6`
- `roleAssignmentMethod`
- `allowOptionalCoo`

### P1.7 Gestion de tenants participantes

Objetivo:

- habilitar universidades y programas en una competencia

Tareas:

- agregar tenant participante
- listar tenants participantes
- quitar tenant participante

Reglas:

- el tenant debe existir
- no duplicar participacion

### P1.8 Busquedas y filtros

Objetivo:

- dejar la API utilizable por frontend y operaciones

Tareas:

- filtros por estado
- filtros por tenant host
- filtros por ciclo

## 7. P2 - Modulo Participacion

### P2.1 Enrollment de staff multiuniversidad

Objetivo:

- inscribir jurados, mentores, inversores y managers

Tareas:

- endpoint de alta
- endpoint de detalle
- update de estado
- baja logica
- validacion de `originTenantId`

Reglas:

- el usuario debe tener membership activa en el `originTenantId`
- el `originTenantId` debe estar habilitado en la competencia
- no puede haber enrollment ambiguo

### P2.2 Endpoint de staff elegible

Objetivo:

- permitir seleccionar staff desde multiples universidades

Tareas:

- query paginada
- filtros por `tenantId`
- filtros por `participantType`
- filtros por `userCategory`
- busqueda por texto

### P2.3 Enrollment de estudiantes

Objetivo:

- habilitar competidores antes del armado de equipos

Tareas:

- alta
- detalle
- update de estado
- baja logica
- listado filtrado

Reglas:

- un estudiante no entra a equipo sin enrollment aprobado
- un estudiante no debe tener dos enrollments activos como competidor en la misma competencia

### P2.4 Auditoria del modulo

Objetivo:

- asegurar trazabilidad institucional y competitiva

Tareas:

- incluir `originTenantId` en respuestas
- logs estructurados por enrollment
- codigos de error claros

## 8. P2 - Modulo Equipos

### P2.5 CRUD de equipos

Objetivo:

- crear y administrar equipos dentro de la competencia

Tareas:

- crear equipo
- listar equipos
- ver detalle
- editar
- eliminar o desactivar

Reglas:

- cada equipo pertenece a una sola competencia
- `code` unico dentro de la competencia

### P2.6 Membresia de equipo

Objetivo:

- unir estudiantes aprobados a equipos

Tareas:

- agregar miembro
- listar miembros
- editar estado
- remover miembro

Reglas:

- el enrollment debe ser `COMPETITOR`
- no exceder `maxTeamSize`
- un competidor no puede estar en dos equipos activos de la misma competencia

### P2.7 Roles ejecutivos

Objetivo:

- modelar responsabilidad real dentro del equipo

Tareas:

- endpoint para asignar rol ejecutivo
- endpoint para editar rol ejecutivo
- validacion de cobertura de roles
- soporte de `secondaryExecutiveRole`

Reglas:

- equipos de 5 cubren `CEO`, `CPO`, `CTO`, `CMO`, `CFO`
- equipos de 6 pueden agregar `COO`
- equipos de 4 permiten rol secundario
- no puede haber dos `CEO`

### P2.8 Estado del equipo

Objetivo:

- distinguir equipos incompletos de equipos listos

Tareas:

- derivar o actualizar estado `FORMING` y `READY`
- bloquear `READY` si faltan roles
- bloquear `READY` si falta tamano minimo

## 9. P3 - Modulo Acceso y Contexto

### P3.1 JWT enriquecido

Objetivo:

- exponer contexto real de pertenencia y participacion

Tareas:

- agregar claims de memberships
- agregar claims de enrollments
- agregar equipos activos
- agregar `actorType` o rol administrativo efectivo
- definir tamano maximo del token

### P3.2 Context switch

Objetivo:

- permitir al usuario activar el contexto correcto

Tareas:

- seleccionar tenant activo
- seleccionar competencia activa
- seleccionar equipo activo

### P3.3 Autorizacion cruzada

Objetivo:

- separar permisos organizacionales de permisos operativos

Tareas:

- policy checks por tenant
- policy checks por competition enrollment
- policy checks por team membership

## 10. P3 - Escalabilidad tecnica

### P3.4 Diseno para crecimiento

Objetivo:

- evitar reescrituras tempranas

Tareas:

- usar paginacion en listados
- indexar consultas por competencia y tenant
- desacoplar DTOs publicos del modelo de persistencia
- centralizar validaciones de negocio
- evitar logica duplicada entre controllers

### P3.5 Observabilidad

Objetivo:

- facilitar soporte y diagnostico

Tareas:

- logs estructurados por tenant, competition y team
- metricas de enrollments y equipos
- errores con `errorCode`

## 11. P3 - Pruebas

### P3.6 Testing por modulo

Objetivo:

- cubrir la base del MVP sin depender solo de H2

Tareas:

- unit tests de servicios
- integration tests de controllers
- pruebas con Postgres real o Testcontainers
- pruebas de migraciones Flyway

## 12. P3 - Documentacion y entrega

### P3.7 Documentacion viva

Objetivo:

- evitar que la implementacion se despegue de la documentacion

Tareas:

- actualizar docs de endpoints por modulo
- actualizar `setup-and-run.md`
- actualizar guia de prueba real
- actualizar plan principal si cambian decisiones

## 13. Orden recomendado por sprints

### Sprint 1

- P0.0
- P0 completo
- P1.1
- P1.1.1
- P1.2
- P1.3

### Sprint 2

- P1.4
- P1.5
- P1.6
- P1.7

### Sprint 3

- P1.8
- P2.1
- P2.2
- P2.3

### Sprint 4

- P2.4
- P2.5
- P2.6
- P2.7
- P2.8

### Sprint 5

- P3.1
- P3.2
- P3.3
- P3.4
- P3.5
- P3.6
- P3.7

## 14. Que sigue inmediatamente

El siguiente paso correcto es:

1. cerrar bootstrap de `PLATFORM_ADMIN` y deshabilitar registro publico
2. revisar si faltan migraciones para `CompetitionEnrollment`, `TeamMembership` y configuracion de competencia
3. implementar administracion de usuarios
4. implementar CRUD de `Tenant`
5. implementar CRUD de `TenantMembership`
6. implementar CRUD de `Competition`

No recomiendo empezar por equipos ni JWT antes de tener organizacion y competencia consolidadas.
