# Scheduling & Concurrency — c4e-ingestion-service

## Estado actual

`FileProcessingPendingJob` y `FileProcessingRetryJob` ya no compiten en el scheduler por defecto.
Cada uno usa su propio `TaskScheduler` dedicado:

- `pendingJobScheduler` (thread prefix `sched-pending-`, pool size = 1)
- `retryJobScheduler` (thread prefix `sched-retry-`, pool size = 1)

Esto permite ejecución paralela real entre ambos jobs en la misma instancia, manteniendo aislamiento por hilo.

## Configuración actual de jobs

| Job | Intervalo | Status que procesa | Feature flag | Scheduler |
|---|---|---|---|---|
| `FileProcessingPendingJob` | 3000ms | `PENDING` | `file-processing-job` | `pendingJobScheduler` |
| `FileProcessingRetryJob` | 3000ms | `RETRY` | `file-retry-job` | `retryJobScheduler` |

Ambos comparten el intervalo vía `file.processing.interval-ms`.

## Protecciones de concurrencia existentes

### DB — Pessimistic Write Lock
Las queries de claim usan `@Lock(LockModeType.PESSIMISTIC_WRITE)` para evitar doble claim concurrente del mismo `FileRecord`.

### Ownership — `locked` + `lockedBy`
```
FileRecord.locked   = true
FileRecord.lockedBy = UUID único por instancia (InstanceIdentifier)
```
- `saveIfOwnedBy()` valida ownership antes de persistir.
- `releaseLockIfOwnedBy()` valida ownership antes de liberar.
- Si ownership se pierde: `LockOwnershipException`.

### AsyncConfig
`ThreadPoolTaskExecutor` (`proc-`, core=4, max=8) sigue disponible para flujos `@Async`; no sustituye schedulers `@Scheduled`.

## Observabilidad de jobs

Se agregó instrumentación central en `FileProcessingExecutor`:

- Logs de ciclo: inicio, deshabilitado por flag, completado (claimed, duración, thread) y fallo.
- Métricas Micrometer:
  - `c4e.scheduler.job.skipped`
  - `c4e.scheduler.job.claimed`
  - `c4e.scheduler.job.failed`
  - `c4e.scheduler.job.duration` (tag `outcome=success|error`)

Endpoints expuestos en Actuator:

- `/actuator/health`
- `/actuator/info`
- `/actuator/metrics`
- `/actuator/scheduledtasks`

## Estado de preparación

La app queda preparada para mayor throughput en procesamiento de `PENDING` y `RETRY` dentro de la misma instancia, con aislamiento de scheduler, lock de BD y señales de observabilidad operables.

