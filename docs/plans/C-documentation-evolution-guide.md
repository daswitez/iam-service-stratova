# Guia de Evolucion Documental - Topologia Alfabetica

## 1. Proposito

Este documento define como se debe actualizar la documentacion a medida que avance la implementacion del MVP academico multi-tenant.

La idea es simple:

- que la documentacion no quede atras del codigo
- que un tercero sepa exactamente que archivo tocar
- que la carpeta `plans/` tenga una topologia alfabetica estable

## 2. Topologia alfabetica de `plans/`

La carpeta `plans/` queda organizada asi:

- `001-academic-multitenant-mvp-implementation.md`
  - plan principal y decisiones del MVP
- `A-academic-multitenant-logic.md`
  - logica operativa y reglas del modelo
- `B-prioritized-implementation-backlog.md`
  - backlog priorizado y orden de ejecucion
- `C-documentation-evolution-guide.md`
  - reglas para mantener la documentacion actualizada
- `D-relational-model-contract-mvp.md`
  - contrato relacional final del MVP antes de migraciones

Regla:

- `001-...` conserva el plan maestro
- `A-...`, `B-...`, `C-...` y siguientes sirven como documentos operativos complementarios

Si luego se necesita agregar mas documentos, seguir este orden:

- `E-...`
- `F-...`
- `G-...`

## 3. Principio de documentacion viva

Toda funcionalidad nueva del MVP debe cerrar con actualizacion documental.
No debe existir "lo implementamos y luego documentamos".

Definicion de hecho:

- si cambia el comportamiento publico, cambia la documentacion publica
- si cambia el arranque local, cambia la documentacion operativa
- si cambia el modelo de dominio, cambia la documentacion de planes o arquitectura

## 4. Mapa de documentos por tipo de cambio

### 4.1 Si cambia el modelo multi-tenant

Actualizar:

- `iam-service/docs/plans/001-academic-multitenant-mvp-implementation.md`
- `iam-service/docs/plans/A-academic-multitenant-logic.md`

Ejemplos:

- nuevos tipos de tenant
- cambios en enrollments
- cambios en estructura de equipos

### 4.2 Si cambia el orden de trabajo o prioridades

Actualizar:

- `iam-service/docs/plans/B-prioritized-implementation-backlog.md`

Ejemplos:

- cambio de sprint
- modulo bloqueado
- nueva dependencia tecnica

### 4.3 Si cambia un endpoint o payload

Actualizar:

- `docs/endpoints/iam/auth.md` si afecta auth
- documentacion administrativa de usuarios si cambia el flujo de alta
- documentacion de endpoints especifica que se cree para tenants, competitions, enrollments o teams
- `iam-service/docs/api/multi-tenant-real-data-testing.md` si cambia la manera de probar

### 4.4 Si cambia el flujo local de desarrollo

Actualizar:

- `iam-service/docs/setup-and-run.md`
- `iam-service/docs/README.md`

### 4.5 Si cambia una decision arquitectonica fuerte

Actualizar:

- `iam-service/docs/plans/001-academic-multitenant-mvp-implementation.md`
- `iam-service/docs/plans/A-academic-multitenant-logic.md`
- crear o actualizar un ADR si la decision lo amerita

## 5. Checklist documental por modulo

### Modulo Organizacion

Cuando se implemente:

- crear documentacion de endpoints administrativos de usuarios
- crear documentacion de endpoints de tenants
- crear documentacion de endpoints de memberships
- agregar ejemplos reales de alta y consulta
- actualizar guia de pruebas

### Modulo Competencia

Cuando se implemente:

- crear documentacion de CRUD de competencias
- documentar tenants participantes
- documentar reglas de `minTeamSize`, `maxTeamSize`, `roleAssignmentMethod`

### Modulo Participacion

Cuando se implemente:

- documentar enrollments de staff y estudiantes
- documentar `originTenantId`
- documentar filtros de staff elegible multiuniversidad

### Modulo Equipos

Cuando se implemente:

- documentar CRUD de equipos
- documentar membresias de equipo
- documentar roles ejecutivos
- documentar restricciones `4-6`

### Modulo Acceso y Contexto

Cuando se implemente:

- actualizar `auth.md`
- documentar claims JWT
- documentar contexto activo

## 6. Orden recomendado para actualizar docs

Cada vez que se cierre una feature, seguir este orden:

1. actualizar doc de endpoint o modulo afectado
2. actualizar guia de prueba real
3. actualizar `setup-and-run.md` si cambia algo operativo
4. actualizar `README.md` si aparece una nueva seccion relevante
5. actualizar `001` o `A` si la decision cambia la logica base
6. actualizar `B` si el backlog o prioridad cambia

## 7. Formato recomendado para nuevas docs de endpoints

Cada nuevo archivo de endpoint deberia incluir:

1. objetivo
2. endpoint
3. request
4. response
5. validaciones
6. errores esperados
7. ejemplo `curl`
8. notas de negocio

## 8. Regla de nombrado

Para nuevos archivos dentro de `plans/`:

- usar prefijo alfabetico en mayuscula
- usar nombres cortos y estables
- evitar mezclar numeracion nueva con letras sin motivo

Ejemplos buenos:

- `D-authorization-model.md`
- `E-testing-strategy.md`

Ejemplos malos:

- `nuevo-plan.md`
- `plan-final-bueno.md`
- `002-otra-cosa.md`

## 9. Definicion de terminado para documentacion

Una tarea de implementacion queda realmente cerrada solo si:

- el codigo compila
- las pruebas relevantes pasan
- la documentacion publica fue ajustada
- la guia de prueba refleja el comportamiento actual
- el backlog o plan fue corregido si la implementacion cambio la estrategia

## 10. Que documento tocar primero a partir de ahora

Segun el siguiente paso recomendado del backlog:

1. si arrancamos bootstrap administrativo, actualizar `auth.md` y crear doc de usuarios administrativos
2. si arrancamos CRUD de tenants, crear doc de endpoints de tenants
3. luego crear doc de endpoints de memberships
4. despues doc de competencias
5. despues doc de enrollments
6. despues doc de equipos y roles ejecutivos

Eso mantiene la documentacion creciendo en el mismo orden que el producto.
