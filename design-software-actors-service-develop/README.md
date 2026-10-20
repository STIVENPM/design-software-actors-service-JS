# design-software-actors-service (Java/Spring Boot)

Esqueleto hexagonal Maven multi-modulo (ADR-006): core (dominio+aplicacion puros) / adapters / apps.
Modulos: actors-core actors-adapters actors-api. Build: ./mvnw package (requiere Maven).

## Arranque local (smoke test)

```powershell
# 1. Compilar el jar ejecutable
mvn package -DskipTests

# 2. DB develop + migraciones (desde design-software-docker-infra)
docker compose --env-file .env.develop up postgres -d
docker compose --env-file .env.develop --profile tooling run --rm liquibase-actors update

# 3. Env de BD (cada terminal nueva)
$env:ACTORS_DB_URL="jdbc:postgresql://localhost:15432/design-software-develop"
$env:ACTORS_DB_USER="design_software_user"
$env:ACTORS_DB_PASSWORD="change-me"
$env:ACADEMIC_MANAGEMENT_BASE_URL="http://<academic-management-host>:<port>"

# 4. Levantar (perfil dev + tracing apagado para no requerir el collector OTLP)
java -jar actors-api\target\actors-api-0.1.0-exec.jar --spring.profiles.active=dev --management.tracing.enabled=false

# 5. Probar que responde (otra terminal)
curl.exe -s http://localhost:8080/actuator/health
curl.exe -s -i http://localhost:8080/api/v1/instructors
```

Esperado: `{"status":"UP"}` y `401 MISSING_TOKEN` en el API (ese 401 prueba
seguridad + envelope). Sin IAM real no hay JWT firmable a mano para endpoints
autenticados; esos se cubren con ITs (JWT mockeado).

## Tests

```powershell
# Unitarios Surefire (no requieren BD; los *IT no corren aqui)
mvn test

# Integracion Failsafe: ITs *IT contra Postgres real (requiere ACTORS_DB_* + liquibase al dia)
# El POM fija api.version=1.44 para Testcontainers/Docker Desktop. Si el entorno
# lo sobreescribe, usar explicitamente: mvn -Dapi.version=1.44 verify
mvn verify

# Solo una clase / un metodo (iteracion rapida)
mvn "-Dtest=InstructorIT" "-Dsurefire.failIfNoSpecifiedTests=false" "-pl=actors-api" "-am" test
mvn "-Dtest=InstructorAreaContractIT#secondCurrentContractClosesPrevious" "-Dsurefire.failIfNoSpecifiedTests=false" "-pl=actors-api" "-am" test
```

Reportes: `actors-api/target/surefire-reports` y `actors-api/target/failsafe-reports`.

## Contrato HU-ACT-007: inscripcion de aprendiz en ficha

`POST /api/v1/learners/{id}/ficha-enrollments` requiere `ACT_LEARNER_MANAGE` y recibe JSON snake_case:

```json
{
  "ficha_id": "00000000-0000-0000-0000-000000000000",
  "enrollment_status_id": "00000000-0000-0000-0000-000000000000",
  "stage": "LECTURE",
  "enrollment_date": "2026-09-17",
  "transfer_reason": "Cambio de ficha"
}
```

Campos obligatorios: `ficha_id`, `enrollment_status_id`, `stage`, `enrollment_date`.
`transfer_reason` es opcional y se conserva cuando aplica traslado. Valores de
`stage`: `INDUCTION`, `LECTURE`, `PRODUCTIVE`.

`GET /api/v1/learners/{id}/ficha-enrollments` requiere `ACT_LEARNER_VIEW` y devuelve el historial.

## Contrato interno HU-ACT-012: aprendices activos por ficha

`GET /internal/fichas/{fichaId}/active-learners/count` requiere Bearer JWT y el
feature provisional `ACT_INTERNAL_FICHA_LEARNER_COUNT` en `features[]`. El nombre
y la concesion a `academic-management` deben confirmarse con IAM.
Actors consulta `GET /api/v1/fichas/{id}` en Academic usando el mismo JWT recibido.
Ese JWT tambien necesita `ACADEMIC_FICHA_VIEW:GLOBAL`: Academic puede ocultar
fichas fuera de scope con `404`, por lo que un scope menor no permite distinguir
inexistencia. Configure `ACADEMIC_MANAGEMENT_BASE_URL` con la URL base de Academic.

Respuesta 200:

```json
{
  "ficha_id": "00000000-0000-0000-0000-000000000000",
  "active_learners": 0
}
```

Cuenta inscripciones `is_current=true`, activas, no eliminadas, con estado
`ACTIVE` de `LEARNER_ENROLLMENT_STATUS` y aprendiz activo no eliminado.
Una ficha existente sin inscripciones activas devuelve `0`. Una ficha inexistente
segun Academic devuelve `404` con el envelope de error. Si Academic no responde o
rechaza la consulta, Actors devuelve `502` y no interpreta el fallo como ficha
inexistente.
