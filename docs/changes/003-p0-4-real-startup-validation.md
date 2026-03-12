# Cambio 003 - P0.4 Validacion real de arranque con Flyway

## 1. Resumen

Este cambio cierra `P0.4 Prueba de arranque real con migraciones`.

Se valido el arranque real de `iam-service` contra PostgreSQL limpio, con Flyway habilitado y aplicando el schema hasta `V5`.

Resultado:

- el contenedor de PostgreSQL levanto correctamente
- Flyway aplico `V1` a `V5` sin errores
- la aplicacion arranco en `dev`
- `POST /api/v1/auth/login` funciona con el `PLATFORM_ADMIN` bootstrap
- `GET /actuator/health` responde `200`

## 2. Objetivo

Confirmar que las migraciones nuevas no solo compilan o pasan en H2, sino que tambien son ejecutables en el entorno real de desarrollo.

Esto era necesario porque los tests actuales:

- usan H2
- no ejecutan Flyway
- no validan el schema PostgreSQL real

## 3. Flujo validado

### 3.1 Reinicio limpio de base de datos

```bash
cd iam-service
docker-compose down -v
docker-compose up -d
```

Resultado esperado:

- volumen eliminado
- contenedor `solveria-iam-postgres` recreado
- healthcheck del contenedor en estado `healthy`

### 3.2 Arranque del servicio

```bash
cd iam-service
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Resultado esperado:

- Flyway ejecuta migraciones
- Spring Boot levanta en `http://localhost:8080`

## 4. Evidencia de Flyway

Consulta ejecutada:

```bash
docker exec solveria-iam-postgres \
  psql -U iam_user -d iam_db \
  -c "SELECT installed_rank, version, description, success FROM flyway_schema_history ORDER BY installed_rank;"
```

Resultado validado:

```text
 installed_rank | version |            description             | success
----------------+---------+------------------------------------+---------
              1 | 1       | init iam schema                    | t
              2 | 2       | seed default actions               | t
              3 | 3       | CoreSim IAM Schema                 | t
              4 | 4       | multi tenant academic foundation   | t
              5 | 5       | academic mvp relational completion | t
```

Conclusion:

- `V5__academic_mvp_relational_completion.sql` se aplico correctamente en PostgreSQL real

## 5. Endpoints verificados

### 5.1 `GET /actuator/health`

```bash
curl http://localhost:8080/actuator/health
```

Respuesta validada:

```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP"
    }
  }
}
```

Interpretacion:

- el servicio esta arriba
- la conexion real a PostgreSQL esta sana

### 5.2 `POST /api/v1/auth/login`

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@solveria.local",
    "password": "Admin12345!"
  }'
```

Respuesta validada:

```json
{
  "token": "<JWT>",
  "user": {
    "id": 1,
    "username": "platform.admin",
    "email": "admin@solveria.local",
    "primaryTenantId": null,
    "userCategory": "ACADEMIC_ADMIN",
    "roles": ["ACADEMIC_ADMIN", "PLATFORM_ADMIN"]
  },
  "context": {
    "activeTenantId": null,
    "memberships": [],
    "teamCompetitions": []
  }
}
```

Interpretacion:

- el bootstrap del administrador sigue operativo despues de aplicar todas las migraciones
- el login todavia es consistente con el cambio `001`

## 6. Archivos involucrados

### Migraciones

- `src/main/resources/db/migration/V4__multi_tenant_academic_foundation.sql`
- `src/main/resources/db/migration/V5__academic_mvp_relational_completion.sql`

### Configuracion usada

- `docker-compose.yml`
- `src/main/resources/application-dev.yml`

## 7. Riesgos cerrados por este corte

- se confirma que `V5` no rompe el arranque real
- se confirma que `ddl-auto=validate` es compatible con el schema migrado
- se confirma que el bootstrap admin no depende de H2 ni de seeds manuales

## 8. Riesgos que siguen abiertos

- los tests automaticos todavia no cubren Flyway real
- aun no existen los CRUDs administrativos de usuarios
- la seguridad JWT local fina para futuros endpoints administrativos sigue pendiente

## 9. Siguiente paso recomendado

El siguiente paso correcto es abrir `P1.1.1 Administracion de usuarios`:

1. `POST /api/v1/admin/users`
2. detalle y edicion de usuarios
3. baja logica de usuarios
4. creacion de otros administradores
