# Scheduling & Concurrency — c4e-ingestion-service

## Estado actual

Spring Boot usa **1 solo thread** para todos los `@Scheduled` por defecto.
Con dos jobs activos (`FileProcessingPendingJob` + `FileProcessingRetryJob`) ambos se ejecutan **secuencialmente** en el mismo hilo.

## Configuración actual de jobs

| Job | Intervalo | Status que procesa | Feature flag |
|---|---|---|---|
| `FileProcessingPendingJob` | 3000ms | `PENDING` | `file-processing-job` |
| `FileProcessingRetryJob` | 3000ms | `RETRY` | `file-retry-job` |

Ambos comparten el mismo intervalo vía `FileProcessingJobProperties` (`file.processing.interval-ms`).

## Protecciones de concurrencia existentes

### DB — Pessimistic Write Lock
Todas las queries de claim usan `@Lock(LockModeType.PESSIMISTIC_WRITE)`, impidiendo que dos instancias reclamen el mismo `FileRecord` simultáneamente.

### Ownership — `locked` + `lockedBy`
```
FileRecord.locked   = true
FileRecord.lockedBy = UUID único por instancia (InstanceIdentifier)
```
- `saveIfOwnedBy()` valida ownership antes de persistir
- `releaseLockIfOwnedBy()` valida ownership antes de liberar
- Si ownership se pierde → `LockOwnershipException`

### AsyncConfig
Existe un `ThreadPoolTaskExecutor` (`proc-`, core=4, max=8) pero **no lo usan los jobs** ya que no están anotados con `@Async`. Está disponible para otros flujos.

## Estado de preparación para concurrencia

**La aplicación ya está lista para ejecutar los jobs en paralelo.** Todas las capas de protección necesarias están implementadas:

| Capa | Estado | Detalle |
|---|---|---|
| DB Pessimistic Write Lock | ✅ Implementado | Ningún hilo puede reclamar el mismo `FileRecord` que otro |
| Ownership (`locked` + `lockedBy`) | ✅ Implementado | UUID por instancia, validado antes de cada write |
| `saveIfOwnedBy` | ✅ Implementado | No persiste si perdió el lock |
| `releaseLockIfOwnedBy` | ✅ Implementado | No libera si no es el dueño |
| `ThreadPoolTaskExecutor` (proc-, 4–8 threads) | ✅ Implementado | Disponible para `@Async` workflows |

No hay deuda técnica pendiente para soportar concurrencia. El sistema es seguro frente a condiciones de carrera tanto en single-instance como en entornos multi-instancia.

## Activación futura: scheduler pool dedicado (1 línea)

Cuando el volumen justifique ejecutar ambos jobs en paralelo, basta con añadir en `application.yml`:

```yaml
spring:
  task:
    scheduling:
      pool:
        size: 2   # 1 hilo por job activo
```

> No se requiere ningún cambio de código. La infraestructura de concurrencia ya está en producción.

