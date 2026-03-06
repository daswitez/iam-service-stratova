# Dev Checklist - IAM Service

Checklist diario para desarrollo. Ejecutar antes de cada commit.

## 🔍 Pre-Commit Checklist

### Compilación
- [ ] `mvn clean compile` ejecuta sin errores
- [ ] No hay warnings críticos
- [ ] Todas las dependencias resueltas

### Tests
- [ ] `mvn test` pasa todos los tests
- [ ] Cobertura mínima: 80%
- [ ] Tests nuevos tienen assertions claras
- [ ] Tests son determinísticos (mismo resultado cada vez)

### Código
- [ ] No hay código comentado innecesario
- [ ] No hay imports no usados
- [ ] No hay variables no usadas
- [ ] Nombres de variables/métodos son descriptivos

### Convenciones
- [ ] Logs en formato estructurado: `event=... key=value`
- [ ] No hay mensajes hardcodeados (usar errorCode)
- [ ] ErrorCodes definidos en `ErrorCodes.java`
- [ ] Mensajes i18n en los 3 idiomas (es, en, pt)

### Arquitectura
- [ ] Separación de capas respetada:
  - API/Orchestration en iam-service
  - Negocio en core-platform
- [ ] No hay lógica de negocio en Controllers/Orchestrators
- [ ] DTOs correctos (API vs Aplicación)

### Endpoints
- [ ] Endpoints documentados con OpenAPI
- [ ] Códigos de respuesta documentados
- [ ] Ejemplos en Swagger UI funcionan

### Exception Handling
- [ ] Excepciones manejadas por GlobalExceptionHandler
- [ ] Respuestas de error tienen estructura consistente
- [ ] Mensajes i18n funcionan

## 🧪 Testing Checklist

### Tests Unitarios
- [ ] Orchestrators tienen tests
- [ ] Tests mockean dependencias correctamente
- [ ] Tests validan casos exitosos y de error

### Tests de Integración
- [ ] Controllers tienen tests con MockMvc
- [ ] Tests validan request/response completos
- [ ] Tests validan códigos HTTP

### Contract Tests
- [ ] Contract tests con MockMvc pasan
- [ ] Pact provider tests pasan (si aplica)

## 📝 Documentación Checklist

- [ ] OpenAPI/Swagger actualizado
- [ ] Ejemplos de request/response correctos
- [ ] Códigos de error documentados
- [ ] README actualizado (si hay cambios significativos)

## 🚀 Pre-Push Checklist

### Git
- [ ] `git status` muestra solo archivos relevantes
- [ ] Commit message descriptivo y claro
- [ ] No hay archivos temporales o de configuración local

### Validación Final
```bash
# Ejecutar antes de push
mvn clean test
git status
git log --oneline -5
```

- [ ] Todos los tests pasan
- [ ] Solo cambios relevantes en git status
- [ ] Commit messages siguen convenciones (feat:, fix:, etc.)

## 🔧 Comandos Útiles

### Compilación y Tests
```bash
# Compilar
mvn clean compile

# Ejecutar tests
mvn test

# Ejecutar tests específicos
mvn test -Dtest=RoleControllerTest

# Ver cobertura
mvn test jacoco:report
```

### Arranque y Validación
```bash
# Arrancar servicio
mvn spring-boot:run

# Verificar health
curl http://localhost:8080/actuator/health

# Verificar OpenAPI
curl http://localhost:8080/v3/api-docs
```

### Git
```bash
# Ver cambios
git status
git diff

# Ver commits recientes
git log --oneline -10

# Crear commit
git add .
git commit -m "feat: descripción del cambio"
```

## ⚠️ Errores Comunes

### Compilación
- **Error:** Dependencias no resueltas
  - **Solución:** `mvn clean install` o verificar que core-platform esté instalado

- **Error:** Versiones incompatibles
  - **Solución:** Verificar `pom.xml` y versiones de Spring Boot

### Tests
- **Error:** Tests fallan intermitentemente
  - **Solución:** Verificar que tests sean determinísticos (no dependan de orden, tiempo, etc.)

- **Error:** MockMvc no encuentra endpoints
  - **Solución:** Verificar `@WebMvcTest` y `@AutoConfigureMockMvc`

### Runtime
- **Error:** Beans no se inyectan
  - **Solución:** Verificar `@Component`, `@Service`, `@Configuration` y `scanBasePackages`

- **Error:** Entidades JPA no se reconocen
  - **Solución:** Verificar `@EntityScan` en `IamServiceApplication`

## 📚 Referencias Rápidas

- Convenciones: `docs/prompts/000-conventions.md`
- Master Runbook: `docs/runbooks/MASTER-RUNBOOK.md`
- Release Checklist: `docs/runbooks/RELEASE-CHECKLIST.md`
