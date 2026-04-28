# Dev Notes - Implementación actual de ingesta

> Documento interno para equipo técnico. No orientado a cliente.

## 1. Objetivo de estas notas

Dejar descrito, de forma compacta, cómo está funcionando hoy el flujo de ingesta en `c4e-ingestion-service`, qué piezas intervienen, qué se considera ya consolidado y qué queda pendiente en la siguiente iteración.

---

## 2. Resumen técnico del flujo actual

### Entrada principal
- `POST /files` en `FileUploadController`
- Orquestación en `FileUploadOrchestratorService`
- Entrada alternativa por carpetas monitorizadas mediante `FileScannerService` + `FileScannerJob`

### Flujo alto nivel
1. Se recibe uno o varios archivos.
2. Se construye `ValidationContext` por fichero.
3. Se ejecuta la cadena de validadores (`FileValidator`).
4. Según resultado:
   - inválido -> se guarda en `rejected` y se genera evento outbox,
   - duplicado -> se guarda en `duplicates` y se genera evento outbox,
   - válido -> se guarda en `pending` y se registra `FileRecord`.
5. El archivo pendiente puede procesarse por:
   - `FileProcessingJob`, o
   - `FileProcessingListener` si entra por Rabbit.
6. `FileProcessingServiceImpl` reclama lock, mueve a `processing`, ejecuta el procesador por tipo y resuelve estado final.
7. Resultado final:
   - `SUCCEEDED` -> mueve a `processed`
   - `FAILED` -> mueve a `failed`
   - `REJECTED` -> mueve a `rejected`
   - error inesperado retryable -> vuelve a `pending` con estado `RETRY`

---

## 3. Componentes principales

## 3.1. API y orquestación de subida
### `FileUploadController`
- Endpoint `POST /files`
- Devuelve respuesta agregada por lote

### `FileUploadOrchestratorService`
Responsabilidades:
- construir `FileContext` por fichero,
- ejecutar validaciones en orden,
- clasificar el resultado de cada archivo,
- persistir/reubicar en disco según resultado.

Métodos clave:
- `processFiles(...)`
- `getFileContexts(...)`
- `processRejectFile(...)`
- `processNewFile(...)`
- `processDuplicatedFile(...)`

---

## 3.2. Cadena de validación
Existe un chain-of-responsibility vía `FileValidator`.

Validaciones observadas en el código:
- `NullFileValidator`
- `EmptyFileValidator`
- `FilenameValidator`
- `FileExtensionValidator`
- `ContentTypeValidator`
- `FileSizeValidator`
- `DuplicatedOriginalFilenameValidator`
- `DuplicatedContentByHashValidator`

Notas:
- Hay validadores fail-fast.
- La deduplicación por hash está pensada para ejecutarse tarde por coste I/O + acceso BD.
- La deduplicación cubre tanto nombre original como contenido.

---

## 3.3. Persistencia de seguimiento
### `FileRecord`
Entidad principal de trazabilidad del fichero.

Campos relevantes:
- identidad: `id`
- naming/ruta: `originalFilename`, `finalFilename`, `finalPath`
- clasificación: `type`, `origin`
- seguimiento: `status`, `comment`, `failureReason`, `retryCount`
- locking: `locked`, `lockedBy`, `lockedAt`
- timestamps: `uploadedAt`, `processedAt`, `failedAt`, `lastAttemptAt`
- deduplicación: `hash`

### `FileRecordService`
Responsabilidades:
- alta de nuevos registros `PENDING`,
- búsquedas por `id`, nombre o hash,
- persistencia de cambios de estado,
- claim/release de locks,
- reclamación por lote para jobs,
- reclamación puntual para Rabbit.

### `FileRecordRepository`
Puntos importantes:
- claim por lote con `PESSIMISTIC_WRITE`
- claim por `id` con `PESSIMISTIC_WRITE`
- filtro por estados y por tipos soportados

Esto ayuda a evitar procesamiento concurrente del mismo fichero.

---

## 3.4. Procesamiento por tipo
### `FileTypeProcessorRegistry`
- registra procesadores por `FileType`
- garantiza un único procesador por tipo
- expone `supportedTypes()` para jobs y listener

### `FileProcessingServiceImpl`
Es el núcleo del procesamiento.

Responsabilidades principales:
- reclamar el archivo si no viene ya bloqueado por la misma instancia,
- mover de ubicación actual a `processing`,
- actualizar `FileRecord` a `PROCESSING`,
- localizar el procesador correspondiente,
- aplicar el resultado (`SUCCEEDED`, `FAILED`, `REJECTED`),
- remediar errores inesperados con retry/fallo final,
- liberar el lock al terminar.

Puntos importantes del comportamiento actual:
- si el tipo no tiene procesador registrado, el archivo se omite,
- el lock se libera al final en `finally`,
- los errores inesperados notifican a `IncidentNotificationService`,
- el número de reintentos depende de `processor.features.max-retries`.

### `MeasureFileTypeProcessor`
Soporta hoy:
- `MEDIDA_QH_F5`
- `MEDIDA_QH_P5`
- `MEDIDA_QH_P1`
- `MEDIDA_QH_P2`

Comportamiento actual:
- parsea el fichero con `MeasureFileParserService`,
- si no hay registros válidos -> `FAILED` con `INVALID_FILE_FORMAT`,
- si hay líneas con error -> `FAILED` con comentario descriptivo,
- si el parse es correcto -> `SUCCEEDED`.

Importante:
- hoy el parse sirve para **validar y aceptar/rechazar el fichero**, no para persistir todavía el dato de negocio extraído.

Actualización reciente (medidas):
- se añadió cadena de validación por registro en modo tolerante (`MeasureRecordValidationChain`),
- se acumulan incidencias por línea y se persisten solo registros válidos,
- se genera reporte de defectos mediante `MeasureDefectReportService` en:
  - `<original>.sge_defect.jsonl`
  - `<original>.sge_defect.csv`
- ubicación de reportes: `failed/defects`.

El `comment` de fallo en `FileRecord` incluye la ruta al reporte para trazabilidad operativa.

---

## 3.5. Parser de medidas
### `MeasureFileParserService`
Lo que hace hoy:
- valida formato de nombre,
- clasifica por prefijo de fichero,
- parsea líneas según tipo (`P1`, `P2`, `F5`, `P5`),
- detecta errores por línea con detalle,
- devuelve `MeasureParseResult` con `records` + `errors`.

Valor técnico actual:
- hay validación sintáctica real del contenido,
- el resultado es reutilizable para la futura persistencia funcional.

Gap actual:
- el resultado del parse no se persiste todavía en entidades de dominio ni en tablas de negocio.

---

## 3.6. Jobs y ejecución asíncrona
### `FileProcessingJob`
- procesa lotes `PENDING`
- usa `claimFilesForProcessing(...)`
- batch size configurable

### `FileRetryJob`
- procesa lotes `RETRY`
- actualmente controlado por flag

### `FileProcessingListener`
- consume mensajes Rabbit
- reclama por `id`
- sólo procesa si el tipo está soportado

### `FileScannerJob` / `FileScannerService`
- escanean carpetas configuradas
- hacen claim físico moviendo a `.scanner-lock`
- convierten el `Path` a `MultipartFile`
- reutilizan `FileUploadOrchestratorService`

Observación:
- el scanner actual ya funciona con bloqueo por carpeta lock, pero sigue siendo un área a revisar y endurecer según crecimiento operativo.

---

## 3.7. Outbox e incidencias
### `OutboxService`
Se usa para generar eventos para:
- duplicados,
- rechazados.

Notas importantes:
- existe modelo de outbox con estados (`PENDING`, `PROCESSING`, `PROCESSED`, `FAILED`),
- la pieza ya sirve como base para desacoplar eventos de integración,
- conviene revisar el diseño de payload/identidad en eventos rechazados dentro de una siguiente iteración de consolidación.

### `IncidentNotificationService`
- se dispara ante errores inesperados durante el procesamiento.

---

## 4. Estados y ciclo de vida

Estados observados más relevantes para el flujo actual:
- `PENDING`
- `PROCESSING`
- `SUCCEEDED`
- `FAILED`
- `RETRY`
- `REJECTED`
- `DUPLICATED_ORIGINAL_FILENAME`
- `DUPLICATED_CONTENT`

Ciclo nominal:
`upload/scan -> validate -> pending -> processing -> succeeded/failed/rejected`

Ciclo con incidencia inesperada:
`processing -> retry -> pending -> processing -> ... -> failed`

---

## 5. Estructura de carpetas operativas

Configuradas desde `c4e.upload.*`:
- `automatic`
- `pending`
- `processing`
- `processed`
- `duplicates`
- `failed`
- `rejected`
- `archive`

Soporte scanner:
- `.scanner-lock`

Reportes de defectos de medidas:
- `failed/defects` (no se escanea como entrada)

Valor operativo:
- lectura rápida del estado real del fichero,
- soporte manual más simple,
- inspección y recuperación más sencillas.

---

## 6. Feature flags relevantes

Según `application.yml`, hoy son relevantes:
- `persist-data`
- `send-messages`
- `receive-messages`
- `file-scanner-job`
- `file-processing-job`
- `file-retry-job`
- `scanner-lock-maintenance-job`
- `notify-on-error`

Esto permite activar/desactivar piezas del pipeline sin rediseñar el servicio.

---

## 7. Qué consideramos ya conseguido

- API de recepción funcional.
- Cadena de validación extensible.
- Persistencia de metadatos y trazabilidad del fichero.
- Claim/lock para evitar dobles procesados.
- Procesamiento asíncrono por job y por Rabbit.
- Gestión física del ciclo de vida del archivo.
- Retry ante error inesperado.
- Base modular para añadir nuevos tipos de procesador.
- Cobertura de tests sobre `FileProcessingServiceImpl` para éxito, fallo funcional, retry y fallo de remediación.

---

## 8. Limitaciones / gaps actuales

### Funcionales
- El procesamiento real automatizado está centrado hoy en ficheros de medidas.
- Tipos como `AWAITING_CLASSIFICATION`, `FACTURA`, `DOCUMENTO_PDF`, etc. no tienen todavía procesador conectado al pipeline.

### Persistencia funcional
- Se persiste el seguimiento del fichero, pero no el dato de negocio parseado.
- Próxima fase: persistencia end-to-end de medidas/datos extraídos.

### Scanner / operación
- El scanner ya está implementado, pero conviene endurecer observabilidad, mantenimiento de locks y comportamiento ante errores prolongados.

### Integración / producto
- Falta terminar de consolidar la parte más “presentable” de la aplicación: observabilidad, mensajes, documentación operativa y experiencia global de explotación.

---

## 9. Next steps técnicos recomendados

### 9.1. Persistencia funcional completa
Objetivo:
- persistir el resultado de `MeasureParseResult` en modelo/tablas de negocio,
- enlazar cada dato persistido con su `FileRecord`,
- reforzar trazabilidad end-to-end.

Esto debería incluir:
- diseño de entidades/repositorios de dominio,
- idempotencia por fichero/hash,
- estrategia transaccional,
- tratamiento de parciales si aplica.

### 9.2. Pulido / hardening de aplicación
Objetivo:
- mejorar robustez y claridad operativa antes de una fase de adopción más amplia.

Líneas sugeridas:
- revisar mensajes funcionales y técnicos,
- añadir más métricas y logging orientado a operación,
- documentar runbooks/configuración por entorno,
- reforzar tests de integración end-to-end,
- revisar detalles de outbox y consistencia de eventos,
- cerrar flecos de scanner y mantenimiento de locks.

### 9.3. Extensión de tipologías
Después de consolidar persistencia:
- añadir clasificación/procesamiento para XML,
- incorporar otros documentos previstos por `FileType`,
- mantener el patrón `FileTypeProcessor` como punto de extensión principal.

---

## 10. Referencias de código útiles

Archivos clave para entender la implementación actual:
- `src/main/java/com/com4energy/processor/controller/FileUploadController.java`
- `src/main/java/com/com4energy/processor/service/FileUploadOrchestratorService.java`
- `src/main/java/com/com4energy/processor/service/impl/FileProcessingServiceImpl.java`
- `src/main/java/com/com4energy/processor/service/FileRecordService.java`
- `src/main/java/com/com4energy/processor/repository/FileRecordRepository.java`
- `src/main/java/com/com4energy/processor/service/FileScannerService.java`
- `src/main/java/com/com4energy/processor/job/FileProcessingJob.java`
- `src/main/java/com/com4energy/processor/job/FileRetryJob.java`
- `src/main/java/com/com4energy/processor/messaging/FileProcessingListener.java`
- `src/main/java/com/com4energy/processor/service/processing/MeasureFileTypeProcessor.java`
- `src/main/java/com/com4energy/processor/service/measure/MeasureFileParserService.java`
- `src/test/java/com/com4energy/processor/service/impl/FileProcessingServiceImplTest.java`

---

## 11. Mensaje corto para alinear al equipo

La base técnica de ingesta ya está bien encaminada: recepción, validación, trazabilidad, locking, procesamiento y manejo de error existen y son reutilizables.

La siguiente iteración no debería rehacer el flujo, sino **capitalizar lo construido** en dos frentes:
1. **persistencia funcional del dato**,
2. **pulido/hardening de la aplicación**.

