# Pendiente: Revisión de campos F5 sin mapear a medida_cch

## Estado: ✅ RESUELTO — descarte es intencional

La tabla `medida_cch` tiene **10 columnas**, de las cuales solo 3 corresponden a campos
de medida: `bandera_inv_ver`, `actent` y `metod`.
El resto son auditoría (`created_on`, `created_by`, etc.) e identificadores.
Esto confirma que el modelo CCH fue diseñado para almacenar únicamente esos tres valores.

```
medida_cch
├── id_medida_cch  int (auto increment)
├── id_cliente     int
├── fecha          datetime
├── bandera_inv_ver int
├── actent          int
├── metod           int
├── created_on      datetime
├── created_by      varchar(50)
├── updated_on      datetime
└── updated_by      varchar(50)
```

## Conclusión

Los campos F5 descartados (`as1`, `rq1..rq4`, `indicFirmez`, `codigoFactura`) **no tienen
columna en `medida_cch`**, por lo que su descarte es correcto conforme al esquema actual.

No se requiere acción técnica a menos que el cliente solicite ampliar la tabla.

---

## Detalle del mapeo F5 → CCH (referencia)

| Columna | Campo F5       | Se persiste en CCH  | Motivo del descarte               |
|---------|----------------|---------------------|-----------------------------------|
| 0       | cups           | ✅ (via cliente)     |                                   |
| 1       | timestamp      | ✅ fecha             |                                   |
| 2       | banderaInvVer  | ✅ bandera_inv_ver   |                                   |
| 3       | actent (ae1)   | ✅ actent            |                                   |
| 4       | as1            | ❌ descartado        | Sin columna en medida_cch         |
| 5       | rq1            | ❌ descartado        | Sin columna en medida_cch         |
| 6       | rq2            | ❌ descartado        | Sin columna en medida_cch         |
| 7       | rq3            | ❌ descartado        | Sin columna en medida_cch         |
| 8       | rq4            | ❌ descartado        | Sin columna en medida_cch         |
| 9       | metodObt       | ✅ metod             |                                   |
| 10      | indicFirmez    | ❌ descartado        | Sin columna en medida_cch         |
| 11      | codigoFactura  | ❌ descartado        | Sin columna en medida_cch         |

## Acción futura (opcional)

Si el cliente decide que necesita energía reactiva por cuadrante u otros campos en CCH,
el trabajo implicaría:

1. Migración Liquibase para añadir columnas a `medida_cch`.
2. Ampliar `MedidaCCHEntity` y `MeasureRecord.Cch`.
3. Actualizar `MeasureFileParserService#parseF5` (eliminar `skipF5IntermediateFields`).
4. Actualizar `JpaMeasurePersistenceAdapter#toMedidaCch`.
5. Actualizar tests.

## Ubicación del código afectado

- Parser: `MeasureFileParserService#parseF5` → `skipF5IntermediateFields()`
- Modelo: `MeasureRecord.Cch`
- Entidad: `MedidaCCHEntity`
- Adaptador: `JpaMeasurePersistenceAdapter#toMedidaCch`
