# Future Enhancements Backlog

## Contexto
Este documento captura mejoras **fuera del alcance** de la iteración actual.

En esta iteración ya se cerró:
- Optimizaciones P1/P2 de índices y consultas.
- Optimización de lookup de cliente por CUPS.
- Flujo de cuarentena conectado con `failedRecords` en persistencia de medidas.

Lo siguiente queda planificado para una próxima fase.

## Prioridad P3 (arquitectura y resiliencia)

### 1) Reclaim de eventos outbox en estado `PROCESSING` huérfanos
**Módulo:** `c4e-outbox-worker`

**Problema actual**
Si un worker cae después de marcar `PROCESSING`, el evento puede quedar atascado.

**Propuesta**
- Definir TTL de lock (`locked_at`) para outbox.
- Crear proceso de reclaim (job periódico o fase previa al polling).
- Regresar eventos expirados a `PENDING` o moverlos a estado dedicado según política.

**Impacto**
Alta mejora de robustez operativa.

**Esfuerzo estimado**
Medio/Alto.

**Definition of Done**
- Eventos en `PROCESSING` expirados se recuperan automáticamente.
- No hay doble publicación bajo concurrencia.
- Tests de integración con caída simulada de worker.

---

### 2) Política de retry real para outbox (`FAILED`)
**Módulo:** `c4e-outbox-worker`

**Problema actual**
`FAILED` incrementa `retries`, pero no existe reingreso automático al pipeline.

**Propuesta**
- Definir `max-retries` y estrategia de backoff.
- Introducir transición controlada de `FAILED -> PENDING` para reintento.
- Definir estado terminal (`DEAD` / `FAILED_FINAL`) para no ciclar indefinidamente.

**Impacto**
Alta mejora en entrega eventual.

**Esfuerzo estimado**
Alto.

**Definition of Done**
- Retry automático parametrizable.
- Límite de reintentos enforceable.
- Métricas y logs claros por intento y estado final.

---

### 3) Gobernanza del esquema `outbox_event` (ownership de migraciones)
**Módulos:** `c4e-ingestion-service`, `c4e-outbox-worker`

**Problema actual**
Productor y consumidor usan la misma tabla; conviene definir ownership único de DDL.

**Propuesta**
- Establecer oficialmente qué módulo versiona `outbox_event`.
- Documentar dependencia entre servicios y orden de despliegue.
- Evitar drift de esquema entre repos.

**Impacto**
Alto en mantenibilidad y despliegues.

**Esfuerzo estimado**
Medio.

**Definition of Done**
- Responsabilidad de esquema documentada y aplicada.
- Pipeline de despliegue sin ambigüedad de migraciones.

## Prioridad P4 (cleanup y calidad técnica)

### 4) Limpieza de APIs/repositorios residuales
**Módulo:** `c4e-ingestion-service`

**Candidatos**
- `MeasureRecordRepository`
- `MeasureRecordEntity`
- `existsByOrigen(...)` en repos de medidas si no se usan
- Métodos de repositorio no invocados

**Impacto**
Medio (reduce ruido y deuda técnica).

**Esfuerzo estimado**
Bajo/Medio.

**Definition of Done**
- Sin código muerto en paths de persistencia de medidas.
- `mvn test` en verde tras cleanup.

---

### 5) Endurecimiento de observabilidad para procesamiento y outbox
**Módulos:** ambos

**Propuesta**
- Métricas de lock wait, throughput, retries, dead letters.
- Dashboards/alertas para backlog de `PENDING`, `PROCESSING`, `FAILED`.

**Impacto**
Medio/Alto en operación.

**Esfuerzo estimado**
Medio.

## Notas de implementación
- Mantener cambios arquitecturales bajo feature flags cuando sea posible.
- Añadir pruebas de concurrencia y fallos antes de activar políticas de reclaim/retry en producción.
- Validar siempre con:
  - `c4e-ingestion-service`: `mvn test`
  - `c4e-outbox-worker`: `mvn test`

## Estado
- Documento creado como referencia para próxima iteración.
- No implica cambios funcionales inmediatos en runtime.

