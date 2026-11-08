# Backend · API y worker

```bash
./gradlew bootRun
./gradlew test
./gradlew bootJar
SPRING_PROFILES_ACTIVE=worker ./gradlew bootRun
SPRING_PROFILES_ACTIVE=tls TLS_CERTIFICATE=file:cert.pem TLS_PRIVATE_KEY=file:key.pem ./gradlew bootRun
```

En orden: API en `:8080` (requiere la base de `infra/` levantada), pruebas unitarias con reporte JaCoCo en
`build/reports/jacoco`, jar ejecutable en `build/libs`, solo tareas programadas sin servidor web, y API con
HTTPS en `:8443` usando un certificado PEM (por ejemplo, uno autofirmado generado con `openssl`).

- Versiones: `gradle/libs.versions.toml` (Spring Boot fija el resto mediante su BOM).
- Configuración: `src/main/resources/application.yml` (perfiles `tls` y `worker`); en producción todo por
  variables de `infra/.env`.
- Esquema: migraciones Flyway en `src/main/resources/db/migration` (nunca editar una ya aplicada; crear `V2__...`).
- Errores: las excepciones de `shared.error` se traducen a RFC 9457 en `ErrorHandler`.
