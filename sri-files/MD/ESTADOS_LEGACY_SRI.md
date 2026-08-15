# Estados legacy de `fec_factura`

Este archivo deja documentados los estados cortos actuales para facilitar la migracion incremental hacia los nuevos estados funcionales de `sri-files`.

| Codigo | Significado actual | Estado objetivo sugerido |
| --- | --- | --- |
| `I` | Pendiente de procesamiento o reintento | `RECIBIDO` |
| `P` | Procesando en flujo interno | `VALIDANDO` |
| `C` | Pendiente de autorizacion SRI | `PENDIENTE_AUTORIZACION` |
| `A` | Autorizada | `AUTORIZADO` |
| `O` | Autorizada con correo pendiente o fallido | `CORREO_PENDIENTE` |
| `N` | No autorizada | `NO_AUTORIZADO` |
| `M` | Devuelta u observada por SRI | `DEVUELTO_SRI` |

Notas:

- Esta equivalencia no reemplaza aun la logica legacy.
- El codigo actual sigue vigente mientras se completa la migracion.
- El siguiente paso recomendado es persistir historial funcional por transicion de estado.
