# Ingestion Enhancements - Roadmap para Cliente

## Objetivo
Documentar mejoras recomendadas para la ingesta de archivos, con foco en riesgo real, costo de implementacion e impacto operativo.

## Resumen Ejecutivo
- Ya existe base de seguridad para XML (parser seguro y validacion XSD en utilidades).
- Siguiente mejora prioritaria: validador basico de antivirus en la cadena de validacion.
- Punto critico para negocio: latencia del escaneo puede afectar throughput y formar cola.
- Recomendacion tecnica: modelar escaneo/validacion pesada como procesos asincronos.

## Riesgo real vs costo
| Riesgo | Probabilidad | Impacto | Mitigacion recomendada |
|---|---|---|---|
| Malware en XML u otros adjuntos | Bajo-Medio | Alto | AntivirusValidator + cuarentena |
| XML malicioso (XXE, entidades externas) | Medio | Alto | Parser XML seguro (ya preparado) |
| Datos corruptos / esquema invalido | Alto | Medio | Validacion XSD + validacion semantica |
| Congestion por escaneo | Medio | Alto | Paralelismo controlado + cola asincrona |

## Enhancement 1 - AntivirusValidator (basico)
### Propuesta
Agregar `AntivirusValidator` al chain de validadores para detectar archivos sospechosos antes de procesamiento.

### Ubicacion sugerida en el chain
- `@Order(220)`
- `ValidationMode.FAIL_FAST`

### Advertencias importantes
- El escaneo agrega latencia por archivo.
- Dependencia de motor externo (ej. ClamAV o API third-party).
- Posibles falsos positivos/negativos que requieren politica de negocio.

## Throughput y latencia (advertencia para decision)
Si recibes:
- `100 archivos/minuto`

Y cada scan tarda:
- `500 ms`

Entonces:
- En promedio, un worker puede escanear ~`120 archivos/min` (sin contar overhead real).
- Con picos, reintentos o latencia variable, se puede formar cola facilmente.

### Implicacion
Se recomienda paralelismo controlado y colas para absorber picos y mantener SLA.

## Nota de desarrollo - modelo asincrono
Estas tareas deben tratarse como asincronas:
- Escaneo antivirus
- Validaciones pesadas (XSD grande, reglas semanticas complejas)
- Enriquecimientos externos

Patron recomendado:
1. Recepcion rapida y persistencia de metadata.
2. Publicacion a cola de validacion/escaneo.
3. Workers asincronos con concurrencia controlada.
4. Estado final (`PENDING`, `REJECTED`, `READY`) y trazabilidad.

## Recomendaciones de implementacion por fases
### Fase 1 (quick win)
- Implementar `AntivirusValidator` stub desacoplado por interfaz.
- Feature flag para activar/desactivar escaneo.
- Metricas basicas: tiempo de scan, tasa de rechazo, cola pendiente.

### Fase 2 (produccion)
- Integracion real con motor AV.
- Cuarentena de archivos sospechosos.
- Timeouts, circuit breaker y politica de fallback.

### Fase 3 (madurez)
- Auto-escalado de workers por backlog.
- Dashboard de capacidad (p50/p95/p99 de latencia).
- Ajuste fino de concurrencia por tipo/tamano de archivo.

## Preguntas para validacion con cliente
- Cual es el tiempo maximo aceptable por archivo (SLA)?
- Se permite "aceptar y validar despues" o debe ser bloqueo inmediato?
- Que politica aplicar ante timeout de antivirus: rechazar, reintentar o poner en cuarentena?

