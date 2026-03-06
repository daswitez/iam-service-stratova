# IAM Service - Guía de Ejecución Local

Esta guía describe cómo levantar y ejecutar el microservicio **IAM Service** de Solveria CoreSim AI en un entorno local, ideal para desarrollo y pruebas.

## Requisitos Previos

Asegúrate de tener instalado en tu sistema:
1. **Docker y Docker Compose**: Para levantar la base de datos PostgreSQL aislada.
2. **Java JDK 21**: La versión requerida por el proyecto Spring Boot.
3. **Maven** (opcional, el proyecto incluye `mvnw` (Maven Wrapper)).

---

## 1. Levantar la Base de Datos con Docker

El servicio IAM requiere una base de datos PostgreSQL. Hemos configurado un archivo `docker-compose.yml` en la raíz de `iam-service` que contiene todo lo necesario.

1. Abre una terminal y navega al directorio del servicio:
   ```bash
   cd /ruta/hacia/ProyectoAI/iam-service
   ```

2. Ejecuta el siguiente comando para descargar y levantar el contenedor en segundo plano (`-d`):
   ```bash
   docker-compose up -d
   ```

3. **Verificación:** Puedes comprobar que el contenedor está corriendo con:
   ```bash
   docker ps
   ```
   Deberías ver un contenedor llamado `solveria-iam-postgres` mapeado en el puerto `5432`.

### ¿Qué ocurre internamente?
- Se levanta una instancia de PostgreSQL 16.
- Se configura automáticamente un usuario (`iam_user`), contraseña (`iam_password`) y la base de datos que la aplicación espera (`iam_db`).
- Se utiliza un volumen de Docker (`iam-postgres-data`) para que los datos persistan incluso si apagas el contenedor.

---

## 2. Ejecutar la API (Spring Boot)

Una vez que la base de datos está corriendo, puedes iniciar el servicio IAM.

1. En la misma terminal de `iam-service`, ejecuta el proyecto utilizando el Maven Wrapper con el perfil de desarrollo (`dev`):
   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
   ```

2. **¿Qué ocurre durante el arranque?**
   - **Compilación**: Maven descarga las dependencias y compila el proyecto y su librería compartida (`core-platform`).
   - **Spotless**: Verifica el formato del código (si hay errores de formato, ejecuta `./mvnw spotless:apply` antes).
   - **Flyway Migrations**: Al arrancar, Spring Boot detecta las migraciones SQL en `src/main/resources/db/migration` y **automáticamente crea las tablas y la estructura completa de base de datos**. No necesitas correr scripts manuales.
   - **Seed Data**: El script `V2` (y los siguientes) también insertará datos semilla necesarios (por ejemplo, acciones por defecto como CREATE, READ, UPDATE).

3. **Verificación visual:** Sabrás que todo levantó correctamente cuando veas este mensaje al final de la consola:
   ```text
   Tomcat started on port 8080 (http) with context path '/'
   Started IamServiceApplication in X.XXX seconds
   ```

---

## 3. Comprobar que el Sistema Funciona (Healthcheck)

Para asegurarnos de que la API y la base de datos se conectan sin problemas, consulta los endpoints de sistema generados por Actuator. Puedes usar un navegador, cURL o Postman.

Ejecuta el siguiente comando en una nueva terminal:
```bash
curl http://localhost:8080/actuator/health
```

**Respuesta esperada:**
```json
{"status":"UP"}
```
*(Y dado que configuramos que el detalle de health se exponga bajo ciertas condiciones, podrías ver el estado `UP` del nodo de base de datos).*

### Documentación de la API
Gracias a SpringDoc, una vez levantada la API, podrás interactuar visualmente con los endpoints a través de Swagger UI:
- **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **JSON OpenAPI Doc**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

---

## Solución de Problemas Frecuentes

1. **El puerto 5432 ya está en uso:**  
   Si ya tienes otra instancia de PostgreSQL instalada en tu equipo corriendo nativamente, Docker fallará. Debes detener la local (`sudo systemctl stop postgresql`) o cambiar el mapeo de puertos en el `docker-compose.yml`.

2. **Errores de Hibernate o "Schema-validation":**  
   Si detienes el contenedor, borras los datos y vuelves a arrancar, a veces es necesario borrar la tabla `flyway_schema_history` o directamente hacer `docker-compose down -v` (el `-v` borra el volumen persistente) para levantar un entorno 100% fresco y permitir que los scripts de Flyway se apliquen desde 0.

3. **Errores de Estilo (Spotless):**  
   Si el build de Maven falla indicando "format violations", simplemente corre:
   ```bash
   ./mvnw spotless:apply
   ```
   Y luego vuelve a correr el comando de arranque.
