# IAM Multi-Tenant Testing With Real Data

Esta guía permite probar el modelo multi-tenant académico real ya implementado en `iam-service`, incluso antes de tener endpoints CRUD administrativos para tenants, ciclos, competencias y equipos.

## Qué ya está implementado

- Registro y login con contexto multi-tenant.
- Resolución de `tenantId` por UUID o por `code`.
- Creación automática de tenant si el `tenantId` enviado no existe.
- Respuesta de auth con:
  - memberships activas
  - tenant activo
  - contexto de competencias/equipos si el usuario ya pertenece a alguno

## Qué todavía no existe por API

- CRUD REST para `Tenant`
- CRUD REST para `AcademicCycle`
- CRUD REST para `Competition`
- CRUD REST para `Team`
- Context switch explícito por endpoint

Mientras esos endpoints no existan, la forma correcta de probar el flujo completo es:

1. levantar PostgreSQL
2. sembrar datos reales en la DB
3. registrar o loguear usuarios
4. verificar el `context` devuelto por auth

## 1. Levantar base y API

Desde `iam-service/`:

```bash
docker-compose up -d
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

## 2. Entrar a PostgreSQL del contenedor

```bash
docker exec -it solveria-iam-postgres psql -U iam_user -d iam_db
```

## 3. Sembrar tenants académicos reales

Este ejemplo crea dos universidades, sus programas y una competencia interuniversitaria de un ciclo académico.

```sql
INSERT INTO iam_tenant (id, code, name, type, status, created_at)
VALUES
  ('10000000-0000-0000-0000-000000000001', 'umsa', 'Universidad Mayor de San Andres', 'UNIVERSITY', 'ACTIVE', CURRENT_TIMESTAMP),
  ('10000000-0000-0000-0000-000000000002', 'ucb', 'Universidad Catolica Boliviana', 'UNIVERSITY', 'ACTIVE', CURRENT_TIMESTAMP);

INSERT INTO iam_tenant (id, code, name, type, parent_tenant_id, status, created_at)
VALUES
  ('11000000-0000-0000-0000-000000000001', 'adm-sis-umsa', 'Administracion de Sistemas UMSA', 'PROGRAM', '10000000-0000-0000-0000-000000000001', 'ACTIVE', CURRENT_TIMESTAMP),
  ('11000000-0000-0000-0000-000000000002', 'ing-comercial-ucb', 'Ingenieria Comercial UCB', 'PROGRAM', '10000000-0000-0000-0000-000000000002', 'ACTIVE', CURRENT_TIMESTAMP);

INSERT INTO iam_academic_cycle (id, code, name, owner_tenant_id, start_date, end_date, created_at)
VALUES
  ('20000000-0000-0000-0000-000000000001', '2026-S1', 'Semestre 1 2026', '10000000-0000-0000-0000-000000000001', '2026-02-01', '2026-06-30', CURRENT_TIMESTAMP);

INSERT INTO iam_competition (id, code, name, description, scope, status, owner_tenant_id, academic_cycle_id, starts_at, ends_at, created_at)
VALUES
  ('30000000-0000-0000-0000-000000000001', 'coresim-2026-s1', 'CoreSim 2026 Semestre 1', 'Competencia academica interuniversitaria', 'CROSS_TENANT', 'ACTIVE', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '90 day', CURRENT_TIMESTAMP);

INSERT INTO iam_competition_tenant (id, competition_id, tenant_id, created_at)
VALUES
  ('40000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001', '11000000-0000-0000-0000-000000000001', CURRENT_TIMESTAMP),
  ('40000000-0000-0000-0000-000000000002', '30000000-0000-0000-0000-000000000001', '11000000-0000-0000-0000-000000000002', CURRENT_TIMESTAMP);

INSERT INTO iam_team (id, competition_id, origin_tenant_id, code, name, status, created_at)
VALUES
  ('50000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001', '11000000-0000-0000-0000-000000000001', 'team-andes', 'Team Andes', 'ACTIVE', CURRENT_TIMESTAMP),
  ('50000000-0000-0000-0000-000000000002', '30000000-0000-0000-0000-000000000001', '11000000-0000-0000-0000-000000000002', 'team-condor', 'Team Condor', 'ACTIVE', CURRENT_TIMESTAMP);
```

## 4. Registrar usuarios contra tenants reales

### Estudiante UMSA

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "juan.perez",
    "email": "juan.perez@umsa.bo",
    "password": "SecurePass123",
    "userCategory": "STUDENT",
    "tenantId": "adm-sis-umsa"
  }'
```

### Estudiante UCB

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "maria.lopez",
    "email": "maria.lopez@ucb.edu.bo",
    "password": "SecurePass123",
    "userCategory": "STUDENT",
    "tenantId": "ing-comercial-ucb"
  }'
```

## 5. Obtener IDs reales de usuarios creados

Dentro de `psql`:

```sql
SELECT id, username, email, tenant_id
FROM iam_user
ORDER BY id;
```

## 6. Vincular usuarios a equipos para que aparezca el contexto competitivo

Si `juan.perez` obtuvo `id = 1` y `maria.lopez` obtuvo `id = 2`:

```sql
INSERT INTO iam_team_member (id, team_id, user_id, member_role, status, created_at)
VALUES
  ('60000000-0000-0000-0000-000000000001', '50000000-0000-0000-0000-000000000001', 1, 'CAPTAIN', 'ACTIVE', CURRENT_TIMESTAMP),
  ('60000000-0000-0000-0000-000000000002', '50000000-0000-0000-0000-000000000002', 2, 'CAPTAIN', 'ACTIVE', CURRENT_TIMESTAMP);
```

## 7. Login y validación del contexto

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "juan.perez@umsa.bo",
    "password": "SecurePass123"
  }'
```

Debes recibir:

- `user.primaryTenantId`
- `context.activeTenantId`
- `context.memberships`
- `context.teamCompetitions`

## 8. Qué validar manualmente

- El usuario registrado queda ligado al tenant académico correcto.
- `memberships` devuelve el tenant real y no un `tenantId` inventado.
- Si el usuario pertenece a un equipo, `teamCompetitions` devuelve:
  - equipo
  - competencia
  - alcance `CROSS_TENANT` o `INTRA_TENANT`
  - ciclo académico
  - tenant de origen del equipo

## 9. Consultas útiles

### Ver tenants

```sql
SELECT id, code, name, type, parent_tenant_id, status
FROM iam_tenant
ORDER BY code;
```

### Ver memberships

```sql
SELECT id, user_id, tenant_id, membership_type, status
FROM iam_user_tenant_membership
ORDER BY user_id, created_at;
```

### Ver equipos y competencia

```sql
SELECT
  t.code AS team_code,
  t.name AS team_name,
  c.code AS competition_code,
  c.scope,
  tenant.code AS origin_tenant_code
FROM iam_team t
JOIN iam_competition c ON c.id = t.competition_id
JOIN iam_tenant tenant ON tenant.id = t.origin_tenant_id
ORDER BY t.code;
```

### Ver miembros de equipo

```sql
SELECT
  tm.user_id,
  u.username,
  u.email,
  team.code AS team_code,
  tm.member_role,
  tm.status
FROM iam_team_member tm
JOIN iam_user u ON u.id = tm.user_id
JOIN iam_team team ON team.id = tm.team_id
ORDER BY team.code, tm.user_id;
```

## 10. Estado actual del MVP

Hoy el modelo correcto ya existe en DB y auth:

- tenant organizacional
- memberships
- ciclo académico
- competencia
- equipo
- membresía a equipo

Lo siguiente que falta construir es la capa REST administrativa para no depender de SQL manual.
