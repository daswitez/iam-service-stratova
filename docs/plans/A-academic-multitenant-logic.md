# Logica Operativa - Academic Multi-Tenant MVP

## 1. Proposito

Este documento explica la logica operativa del MVP academico multi-tenant en terminos de negocio y de arquitectura.
No reemplaza el plan principal.
Su funcion es dejar claro como pensar el modelo antes de implementar endpoints, tablas o reglas de autorizacion.

Documento base relacionado:

- `001-academic-multitenant-mvp-implementation.md`

## 2. Principio rector

El sistema debe separar tres niveles que no deben mezclarse:

1. organizacion
2. competencia
3. equipo

La organizacion responde: "a que institucion pertenece este usuario".
La competencia responde: "en que espacio competitivo participa".
El equipo responde: "con que grupo ejecuta la simulacion".

Si estos tres niveles se mezclan, el modelo se vuelve rigido y deja de servir para competencias interuniversitarias.

## 3. Que significa tenant en este MVP

`Tenant` representa una entidad organizacional real.

Para el MVP academico:

- `UNIVERSITY`
- `FACULTY`
- `PROGRAM`

No debe representar:

- equipos
- competencias
- rondas
- simulaciones

Eso permite que una competencia involucre multiples universidades sin destruir el aislamiento organizacional.

## 4. Identidad global y pertenencia multiple

El usuario vive como identidad global en IAM.
La pertenencia a instituciones se modela con `TenantMembership`.

Eso implica:

- un estudiante puede tener una membership principal y otras secundarias
- un docente puede pertenecer a varias universidades o programas
- un jurado o inversor no se ata a una sola universidad en el contexto competitivo

La identidad es una.
Las pertenencias son multiples.
La competencia debe elegir explicitamente desde cual membership participa una persona.

## 5. Staff multiuniversidad

Los docentes que luego actuan como jurados, mentores o inversores potenciales no deben modelarse como usuarios especiales por competencia.

La logica correcta es esta:

1. el docente existe en IAM
2. el docente tiene una o varias `TenantMembership`
3. la competencia habilita ciertos tenants participantes
4. el docente se inscribe en la competencia con un `CompetitionEnrollment`
5. ese enrollment guarda un `originTenantId`

El `originTenantId` no dice "esta persona solo pertenece a esa universidad".
Dice "en esta competencia participa representando o entrando desde esa universidad o programa".

Eso deja trazabilidad, evita ambiguedad y soporta staff interuniversitario real.

## 6. Logica de estudiantes

Los estudiantes no deben entrar directo al equipo.
Primero deben entrar a la competencia y luego al equipo.

Flujo correcto:

1. usuario registrado en IAM
2. membership valida con universidad o programa
3. enrollment en competencia como `COMPETITOR`
4. asignacion posterior a un equipo

Ventajas:

- permite validar elegibilidad antes del armado de equipos
- permite estudiantes sin equipo en una etapa inicial
- permite mover estudiantes entre equipos sin romper el historial
- permite auditar quien estuvo habilitado para competir aunque no haya quedado en equipo

## 7. Logica de equipos

El equipo es una unidad operativa dentro de una competencia.
No es un tenant.
No es un usuario.
No es una empresa legal real.

Cada equipo:

- pertenece a una sola competencia
- tiene un `originTenantId` para trazabilidad academica
- recibe estudiantes ya aprobados en la competencia
- opera bajo reglas de tamano y estructura ejecutiva

## 8. Tamano ideal del equipo

Para el MVP academico, el rango operativo sera:

- minimo `4`
- maximo `6`

Razon:

- menos de 4 sobrecarga a los miembros
- mas de 6 empeora coordinacion y responsabilidad individual

La competencia debe nacer con esa restriccion.
No conviene permitir configuracion libre en esta primera etapa.

## 9. Estructura ejecutiva del equipo

Cada equipo debe modelarse como una organizacion ejecutiva simplificada.

Roles base:

- `CEO`
- `CPO`
- `CTO`
- `CMO`
- `CFO`

Rol opcional:

- `COO`

Reglas:

- equipo de 5: cubre exactamente los 5 roles base
- equipo de 6: agrega `COO`
- equipo de 4: una persona puede cubrir un rol secundario

Eso implica que la membresia del equipo debe guardar:

- `executiveRole`
- `secondaryExecutiveRole` opcional

## 10. Asignacion de roles

Para el MVP, el metodo recomendado es:

- `ADMIN_ASSIGNMENT`

Es decir, la asignacion la hace un administrador autorizado de la competencia o de una universidad participante.

Metodos futuros:

- `DEMOCRATIC_ELECTION`
- `SELF_DECLARED`

No conviene implementar esos metodos en la primera fase porque agregan complejidad de workflow, aprobaciones y conflictos.

## 11. Limites del IAM en este MVP

IAM debe ser dueno de:

- users
- tenants
- tenant memberships
- competitions
- competition tenants
- competition enrollments
- teams
- team memberships
- claims y contexto JWT

IAM no debe ser dueno todavia de:

- motor de simulacion
- turnos
- KPIs economicos
- scoring
- ranking

## 12. Modulos funcionales

### Modulo 1 - Organizacion

Responsable de tenants y memberships.

### Modulo 2 - Competencia

Responsable de crear competencias, definir industria, producto, capital inicial y tenants habilitados.

### Modulo 3 - Participacion

Responsable de enrollments de estudiantes y staff.

### Modulo 4 - Equipos

Responsable de crear equipos, agregar miembros y asignar roles ejecutivos.

### Modulo 5 - Acceso y contexto

Responsable de JWT, permisos, contexto activo y validaciones cruzadas.

## 13. Regla de escalabilidad

Para que esto escale bien, cada modulo debe depender del anterior pero no mezclar responsabilidades.

Orden correcto:

1. organizacion
2. competencia
3. participacion
4. equipos
5. contexto y autorizacion

Tambien hay reglas de diseno que ayudan a escalar:

- no usar `tenantId` como comodin para cualquier cosa
- no mezclar roles organizacionales con roles competitivos
- no meter reglas de simulacion dentro del IAM
- no permitir escrituras ambiguas sin `originTenantId`
- no modelar equipos como tenants

## 14. Contexto JWT recomendado

El token no debe quedar reducido a un solo `tenantId`.
Debe representar contexto compuesto.

Claims minimos recomendados:

- `primaryTenantId`
- `activeTenantId`
- `tenantMemberships`
- `competitionEnrollments`
- `teamIds`
- `roles`

Esto hace posible que otros servicios validen acceso sin reinventar la logica de negocio.

## 15. Flujo canonico del MVP

1. crear universidad
2. crear programa
3. registrar docente y estudiante
4. asignar memberships
5. crear competencia con industria, producto y capital inicial
6. habilitar tenants participantes
7. consultar staff elegible multiuniversidad
8. inscribir staff a la competencia
9. inscribir estudiantes a la competencia
10. crear equipos
11. agregar estudiantes a equipos
12. asignar roles ejecutivos
13. hacer login con contexto correcto

## 16. Criterio de calidad

La implementacion va en buen camino si cumple estas condiciones:

- un docente puede actuar en competencias de multiples universidades sin duplicar identidad
- un estudiante no puede entrar a un equipo sin haber sido enrolado en la competencia
- no hay equipos fuera del rango `4-6`
- todo equipo listo cubre la estructura ejecutiva requerida
- toda accion relevante deja trazabilidad de tenant, competencia y equipo
