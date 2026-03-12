# Cambio 002 - Academic MVP Relational Migration V5

## 1. Resumen

Este cambio implementa `P0.3 Crear migraciones complementarias`.

Se agrega una nueva migracion Flyway para completar el esquema relacional del MVP academico multi-tenant segun el contrato definido en `D-relational-model-contract-mvp.md`.

Archivo principal:

- `src/main/resources/db/migration/V5__academic_mvp_relational_completion.sql`

## 2. Objetivo

Cerrar los huecos estructurales que todavia existian despues de `V4`, sin romper el codigo actual del servicio.

Por eso la estrategia de `V5` es:

- agregar tablas faltantes
- ampliar tablas existentes
- agregar indices y constraints
- mantener compatibilidad temporal con el modelo JPA actual

## 3. Cambios aplicados en schema

### 3.1 `iam_competition`

Se agregan columnas para:

- `product_name`
- `industry_code`
- `industry_name`
- `initial_capital_amount`
- `currency`
- `min_team_size`
- `max_team_size`
- `team_creation_mode`
- `role_assignment_method`
- `allow_optional_coo`

Tambien se agregan:

- defaults operativos para filas existentes
- checks de capital y rango `4-6`
- indices por owner, ciclo, estado y scope

### 3.2 `iam_competition_enrollment`

Se crea la tabla faltante del MVP para:

- staff multiuniversidad
- estudiantes competidores
- trazabilidad con `origin_tenant_id`

### 3.3 `iam_team_member`

Se amplía la tabla para acercarla al modelo objetivo:

- `competition_enrollment_id`
- `team_role`
- `executive_role`
- `secondary_executive_role`

Importante:

- no se elimina `user_id` todavia
- no se elimina `member_role` todavia
- esto queda en modo compatible mientras se refactoriza el codigo

### 3.4 `iam_academic_cycle`

Se corrige la unicidad:

- deja de ser global por `code`
- pasa a ser unica por `owner_tenant_id + code`

### 3.5 Indices operativos

Se agregan indices para:

- tenants por tipo y estado
- memberships primarias activas
- ciclos por owner
- competencias por owner, ciclo, estado y scope
- enrollments por competition, user, origin tenant y status
- teams por origin tenant y status
- team members por team, enrollment, status y executive role

## 4. Compatibilidad

`V5` no pretende cerrar todo el refactor de entidades.
Solo deja la base lista para los siguientes pasos.

Eso significa:

- la tabla nueva `iam_competition_enrollment` ya existe
- varias columnas del modelo objetivo ya existen
- el servicio actual puede seguir arrancando mientras se adapta el codigo

## 5. Endpoint impact

Este cambio no agrega nuevos endpoints REST.

Impacta indirectamente lo que vendra despues:

- CRUD de users admin
- CRUD de competitions
- enrollments
- teams
- roles ejecutivos

## 6. Como validar

### Aplicar migraciones en dev

```bash
cd iam-service
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### Revisar en PostgreSQL

```sql
\d iam_competition
\d iam_competition_enrollment
\d iam_team_member
\d iam_academic_cycle
```

### Consultas utiles

```sql
SELECT column_name, data_type
FROM information_schema.columns
WHERE table_name = 'iam_competition'
ORDER BY ordinal_position;
```

```sql
SELECT indexname, indexdef
FROM pg_indexes
WHERE tablename IN ('iam_competition', 'iam_competition_enrollment', 'iam_team_member');
```

## 7. Siguiente paso recomendado

Con `V5` aplicado, el siguiente paso correcto es adaptar el codigo y los CRUDs a este schema:

1. administracion de usuarios
2. CRUD de competitions
3. enrollments
4. refactor de `iam_team_member` para usar `competition_enrollment_id`
