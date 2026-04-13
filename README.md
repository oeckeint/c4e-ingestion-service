# c4e-ingestion-service

Microservicio Spring Boot para ingesta asincrona de ficheros.

## Estado actual (abril 2026)

Verificado en local sobre el estado actual del repo:

- `./mvnw test` pasa.
- `./mvnw verify` pasa (incluyendo checkstyle).
- Hay cambios locales sin consolidar en `git status` (algunos parecen prototipos no conectados al flujo principal).

## Documentacion recomendada

- Vista cliente: `docs/CLIENT_IMPLEMENTATION_OVERVIEW.md`
- Notas tecnicas internas: `docs/DEV_NOTES_INGESTION_IMPLEMENTATION.md`

## Reporte de defectos de medidas

Cuando un archivo de medidas falla por parseo/validacion/persistencia, se generan reportes en:

- `failed/defects/<archivo_original>.sge_defect.jsonl`
- `failed/defects/<archivo_original>.sge_defect.csv`

El CSV esta orientado a lectura operativa/cliente y el JSONL a trazabilidad tecnica.

## Que funciona hoy

- **Integracion de librerias compartidas**: usa `c4e-event-publisher` para publicar incidentes y `c4e-i18n-core` como base de i18n comun.
- **Validacion de almacenamiento al arranque**: crea/valida directorios configurados (`c4e.upload.*` y `scanner.paths[]`) y falla rapido si no hay permisos.
- **API de subida**: `POST /files` recibe multipart y procesa lote.
- **Persistencia inicial**: registra `FileRecord` con estado `PENDING` (si `app.feature.enabled.persist-data=true`).
- **Encolado RabbitMQ**: publica mensaje con `id` y `originPath` (si `app.feature.enabled.send-messages=true`).
- **Consumidor RabbitMQ**: escucha cola y lanza procesamiento asincrono (si `app.feature.enabled.receive-messages=true`).
- **Pipeline de ficheros**:
  - mueve de `automatic` -> `processing`
  - mueve de `processing` -> `processed`
  - actualiza estados en BD (`PROCESSING`, `PROCESSED`, `RETRYING`, `FAILED`).
- **Retry job**: reprocesa registros `RETRYING` si esta activo `app.feature.enabled.file-retry-job=true`.

## Limitaciones / trabajo pendiente

- `FileScannerService#scanAndRegisterFiles` esta incompleto (deja un log de `needs a new implementation...`).
- Cobertura de tests funcionales muy baja (test de contexto comentado).
- Existen clases de prototipo/no productivas en `src/main/java` que conviene limpiar o mover a tests.

## Requisitos

- Java 17 (SDKMAN recomendado: `17.0.17-tem`).
- Maven 3.9.12 (si usas SDKMAN auto-env).
- MySQL accesible con variables:
  - `DB_URL_SGE`
  - `DB_USER_SGE`
  - `DB_PASSWORD_SGE`
- RabbitMQ accesible con propiedades Spring AMQP por defecto o variables de entorno equivalentes.

## Configuracion clave

Archivo: `src/main/resources/application.yml`

- Variable de entorno opcional para base path:
  - `C4E_HOST_STORAGE_ROOT`.
  - Si no se define, usa `/Users/jesus/Downloads/com4energy`.
- Paths de trabajo:
  - `c4e.upload.base-path`
  - `c4e.upload.pending-path`
  - `c4e.upload.processing-path`
  - `c4e.upload.processed-path`
  - `c4e.upload.automatic-path`
- Scanner:
  - `scanner.paths[]`
  - `scanner.scan-interval-ms`
- Retry:
  - `file.retry-interval-ms`
- Feature flags:
  - `app.feature.enabled.persist-data`
  - `app.feature.enabled.send-messages`
  - `app.feature.enabled.receive-messages`
  - `app.feature.enabled.file-scanner-job`
  - `app.feature.enabled.file-retry-job`
- Incidentes (publisher):
  - `c4e.incidents.enabled`
  - `c4e.incidents.types.validation.exchange`
  - `c4e.incidents.types.integration.exchange`
  - `c4e.incidents.types.system.exchange`

## Arranque rapido local

```bash
cd /Users/jesus/Development/Com4Energy/c4e-ingestion-service
sdk env
./mvnw clean test
./mvnw spring-boot:run
```

Override opcional de ruta base:

```bash
export C4E_HOST_STORAGE_ROOT="$HOME/Downloads/com4energy"
./mvnw spring-boot:run
```

## Probar subida de archivos

```bash
curl -X POST "http://localhost:8080/files" \
  -F "files=@/ruta/al/archivo1.csv" \
  -F "files=@/ruta/al/archivo2.csv"
```

## Flujo resumido

1. `FileUploadController` guarda en carpeta `automatic` y registra en BD.
2. `MessageProducer` publica en `c4e.exchange` con routing `c4e.routing`.
3. `FileProcessingListener` recibe mensaje de `c4e.queue`.
4. `FileProcessingServiceImpl` mueve fichero por carpetas y marca estado final.
5. `FileRetryJob` reintenta registros en `RETRYING`.

## Siguiente iteracion recomendada

1. Completar implementacion de `FileScannerService`.
2. Crear tests de integracion para `POST /files` y pipeline Rabbit.
3. Limpiar clases de prototipo en `src/main/java`.
4. Documentar ejemplos de configuracion por entorno (`local`, `dev`).
