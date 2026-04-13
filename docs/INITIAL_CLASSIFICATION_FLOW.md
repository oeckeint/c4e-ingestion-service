# Flujo de Clasificacion Inicial de Archivos (API y JOB)

## Alcance
Este documento cubre **solo** la clasificacion inicial del archivo como objeto de entrada.

- Incluye: validaciones de archivo, deduplicacion, destino de carpeta, persistencia inicial y outbox.
- Excluye: parseo de contenido de medidas y persistencia de lineas de medida.

## Entradas
- **API**: `POST /files` -> `FileUploadController` -> `FileUploadOrchestratorService.processFiles(..., FileOrigin.API)`
- **JOB**: `FileScannerJob` -> `FileScannerService.scanAndRegisterFiles()` -> `FileUploadOrchestratorService.processFiles(..., FileOrigin.JOB)`

> Nota: API y JOB usan la **misma cadena de validaciones** para clasificar el archivo.

## Validaciones de archivo (sin parseo de contenido)
Ordenadas por `@Order`:

1. `NullFileValidator`
2. `EmptyFileValidator`
3. `FileSizeValidator`
4. `FilenameValidator`
5. `FileExtensionValidator`
6. `ContentTypeValidator`
7. `DuplicatedOriginalFilenameValidator`
8. `DuplicatedContentByHashValidator`

## Matriz de clasificacion inicial
| Origen | Caso | Resultado tecnico | Carpeta destino | file_records | Outbox |
|---|---|---|---|---|---|
| API | Archivo valido | `VALID` -> `PENDING` | `pending` | Si (`saveNew`) | No |
| API | Archivo invalido (no duplicado) | `VALIDATION_FAILED` / `CRITICAL_VALIDATION_FAILED` | `rejected` | No (en esta fase) | Si (`FILE_REJECTED`) |
| API | Archivo duplicado | `DUPLICATED_ORIGINAL_FILENAME` o `DUPLICATED_CONTENT` | `duplicates` | No (en esta fase) | Si (`FILE_ALREADY_EXISTS`) |
| JOB | Archivo valido | `VALID` -> `PENDING` | `scanner-lock` (temporal) -> `pending` | Si (`saveNew`, `origin=JOB`) | No |
| JOB | Archivo invalido (no duplicado) | `VALIDATION_FAILED` / `CRITICAL_VALIDATION_FAILED` | `rejected` | No (en esta fase) | Si (`FILE_REJECTED`) |
| JOB | Archivo duplicado | `DUPLICATED_ORIGINAL_FILENAME` o `DUPLICATED_CONTENT` | `duplicates` | No (en esta fase) | Si (`FILE_ALREADY_EXISTS`) |

## Campos iniciales relevantes en file_records para archivos validos
Cuando el archivo pasa clasificacion inicial y entra a `pending`, se crea `FileRecord` con:

- `status = PENDING`
- `origin = API` o `JOB`
- `type` inicial por extension/nombre de archivo
- `quality_status = NOT_EVALUATED`
- `business_result = NOT_PROCESSED`

## Que pasa despues
La fase posterior (job de procesamiento) toma archivos `PENDING/RETRY` y recien ahi:

- parsea contenido de medidas,
- aplica validaciones por linea,
- persiste medidas,
- actualiza `quality_status` y `business_result` con el resultado real de negocio.

