# Logs i18n reference (c4e-ingestion-service)

## Objetivo
Dejar una referencia unica para definir y usar mensajes de log con i18n en ingestion.

## Alcance
Este documento aplica a:
- `src/main/java/com/com4energy/processor/common/LogsCommonMessageKey.java`
- `src/main/resources/logs-messages.properties`
- Uso en codigo via `Messages.format(...)`

## Regla principal
No hardcodear mensajes de log en clases de servicio/job.

Siempre usar:
1. Enum en `LogsCommonMessageKey`
2. Texto en `logs-messages.properties`
3. Llamada en codigo con `Messages.format(...)`

## Patron de uso
```java
log.info(Messages.format(LogsCommonMessageKey.FILE_FOUND, filesFound, pathStr));
log.debug(Messages.format(LogsCommonMessageKey.FILE_ALREADY_CLAIMED, targetFile));
throw new FileScannerException(Messages.format(LogsCommonMessageKey.SCANNER_DIRECTORY_SCAN_ERROR), e);
```

## Flujo para agregar una nueva clave
1. Agregar la constante en `LogsCommonMessageKey`.
2. Agregar la entrada equivalente en `logs-messages.properties`.
3. Usar la constante desde codigo con `Messages.format(...)`.
4. Compilar modulo para validar que no haya claves faltantes.

Comando recomendado:
```bash
cd /Users/jesus/Development/Com4Energy/c4e-ingestion-service
mvn -q -DskipTests compile
```

## Convenciones
- Prefijo de propiedades: `log.`
- Nombre enum: MAYUSCULAS con `_`
- Orden de parametros: respetar `{0}`, `{1}`, `{2}` del properties
- Mensajes tecnicos de log: mantener consistencia en ingles

## Claves actuales
| Enum | Property key |
|---|---|
| `FILE_ALREADY_CLAIMED` | `log.file.already.claimed` |
| `FILE_COULD_NOT_CLAIM` | `log.file.could.not.claim` |
| `FILE_FOUND` | `log.file.found` |
| `SCANNER_DIRECTORY_SCAN_ERROR` | `log.scanner.directory.scan.error` |
| `FILE_DELETE_FAILED` | `log.file.delete.failed` |
| `FILE_CLASSIFICATION_ERROR` | `log.file.classification.error` |
| `FILE_CLASSIFIED` | `log.file.classified` |
| `SCANNER_CLASSIFIED_FILE` | `log.scanner.classified.file` |
| `COULD_NOT_DELETE_CLAIMED_FILE` | `log.could.not.delete.claimed.file` |
| `ERROR_CLASSIFYING_CLAIMED_FILE` | `log.error.classifying.claimed.file` |
| `FILE_PENDING_JOB_CLAIMED` | `log.file.processing.job.pending.claimed` |
| `FILE_RETRY_JOB_CLAIMED` | `log.file.processing.job.retry.claimed` |
| `FILE_PROCESSING_JOB_STARTED` | `log.file.processing.job.started` |
| `FILE_PROCESSING_JOB_COMPLETED` | `log.file.processing.job.completed` |
| `FILE_PROCESSING_JOB_DISABLED` | `log.file.processing.job.disabled` |
| `FILE_PROCESSING_JOB_FAILED` | `log.file.processing.job.failed` |
| `MEASURE_FILE_PROCESSED` | `log.measure.file.processed` |

## Anti-patrones
No usar:
```java
log.info("{} files found in {}", filesFound, pathStr);
log.error("Error scanning directory or creating lock directory", e);
```

## Referencia rapida
Checklist antes de merge:
- [ ] Existe constante en `LogsCommonMessageKey`
- [ ] Existe clave en `logs-messages.properties`
- [ ] El codigo usa `Messages.format(...)`
- [ ] El orden de parametros coincide con placeholders
- [ ] `mvn -q -DskipTests compile` en verde

