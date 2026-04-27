# I18N Bundle Consolidation TODO

Estado actual:
- `Messages` (en `c4e-i18n-core`) resuelve exclusivamente `messages.properties`.
- Existen claves duplicadas en `messages.properties` y `logs-messages.properties`.

Objetivo:
- Evitar drift de claves y falsos positivos en tests/produccion por bundles no alineados.

Pendientes:
1. Definir bundle canonico para todas las claves `log.*` (recomendado: `messages.properties`).
2. Reducir o eliminar duplicados en `logs-messages.properties` una vez validado impacto.
3. Mantener el test `LogsCommonMessageKeyCoverageTest` como contrato minimo de cobertura.

Nota:
- Mientras `Messages` siga fijo a `messages`, cualquier key usada por `LogsCommonMessageKey` debe existir ahi.

