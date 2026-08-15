# ARQUITECTURA FRONTEND — SRI-FILES

**Proyecto:** Administración de Documentos Electrónicos SRI  
**Frontend:** Angular 18+ standalone  
**UI:** Angular Material + Bootstrap 5  
**Gráficos:** Chart.js  
**API:** REST `/api/v1`  
**Autenticación:** JWT  
**Backend:** Spring Boot `sri-files`  
**Versión:** 1.0 — 2026-08-14

## 1. Objetivo

Construir un frontend administrativo completo en Angular para monitorear y administrar el ciclo de los documentos electrónicos:

`JSON → Validación → XML → Firma → Recepción SRI → Autorización → RIDE → Correo → Finalizado`.

Angular se limita a presentación, interacción, formularios, filtros, dashboard y administración. XML, XAdES, SOAP SRI, autorización, Jasper/RIDE, persistencia y correo pertenecen al backend.

Documentos:
- Facturas.
- Liquidaciones de compra.
- Notas de crédito.
- Notas de débito.
- Comprobantes de retención.
- Guías de remisión.

## 2. Stack

- Angular 18+ con componentes standalone.
- TypeScript.
- Angular Router con lazy loading.
- Reactive Forms.
- HttpClient + RxJS.
- Signals para estado local.
- Angular Material para componentes UI.
- Bootstrap 5 principalmente para grid/spacing.
- Chart.js para dashboard.
- SCSS.
- JWT y control de permisos.

No introducir NgRx inicialmente; Signals + RxJS + servicios son suficientes mientras no exista una necesidad demostrada.

## 3. Estructura

```text
src/app/
├── core/
│   ├── auth/
│   ├── guards/
│   ├── interceptors/
│   ├── config/
│   └── services/
├── layout/
│   ├── admin-layout/
│   ├── auth-layout/
│   ├── sidebar/
│   ├── navbar/
│   ├── breadcrumb/
│   └── footer/
├── shared/
│   ├── components/
│   ├── directives/
│   ├── pipes/
│   └── validators/
├── models/
├── services/
├── features/
│   ├── auth/
│   ├── dashboard/
│   ├── documentos/
│   ├── facturas/
│   ├── liquidaciones/
│   ├── notas-credito/
│   ├── notas-debito/
│   ├── retenciones/
│   ├── guias-remision/
│   ├── empresas/
│   ├── establecimientos/
│   ├── puntos-emision/
│   ├── certificados/
│   ├── configuracion/
│   ├── usuarios/
│   ├── roles/
│   ├── auditoria/
│   └── monitoreo/
└── app.routes.ts
```

## 4. Menú

```text
Dashboard

Documentos electrónicos
 ├─ Todos
 ├─ Facturas
 ├─ Liquidaciones de compra
 ├─ Notas de crédito
 ├─ Notas de débito
 ├─ Comprobantes de retención
 └─ Guías de remisión

Administración
 ├─ Empresas
 ├─ Establecimientos
 ├─ Puntos de emisión
 ├─ Secuenciales
 ├─ Certificados
 ├─ Logos y recursos
 ├─ Plantillas RIDE
 ├─ Configuración SRI
 └─ Configuración de correo

Seguridad
 ├─ Usuarios
 ├─ Roles
 └─ Permisos

Control
 ├─ Auditoría
 ├─ Errores
 ├─ Correos
 └─ Monitoreo
```

El menú se construye según permisos del usuario.

## 5. Layout

Aplicación administrativa responsive con navbar, sidebar colapsable, breadcrumb, contenido, perfil de usuario, empresa activa y ambiente SRI visible.

Mostrar permanentemente `SRI: PRUEBAS` o `SRI: PRODUCCIÓN` para reducir errores operativos.

## 6. Rutas

Usar lazy loading por feature:

```text
/login
/dashboard
/documentos
/documentos/:id
/facturas
/liquidaciones
/notas-credito
/notas-debito
/retenciones
/guias-remision
/administracion/empresas
/administracion/establecimientos
/administracion/puntos-emision
/administracion/secuenciales
/administracion/certificados
/administracion/recursos
/administracion/plantillas-ride
/administracion/configuracion-sri
/administracion/configuracion-correo
/seguridad/usuarios
/seguridad/roles
/control/auditoria
/control/errores
/control/correos
/control/monitoreo
```

## 7. Autenticación

Flujo:

`Login → POST /api/v1/auth/login → JWT → TokenService → AuthInterceptor → API`.

Crear:
- `AuthService`.
- `TokenService`.
- `authGuard`.
- `permissionGuard`.
- `authInterceptor`.
- `errorInterceptor`.
- `requestIdInterceptor`.
- `loadingInterceptor`.

No almacenar contraseñas. Ante 401 limpiar sesión y redirigir a login; un 403 debe mostrar acceso denegado sin cerrar la sesión.

## 8. Permisos

Crear `HasPermissionDirective` para ocultar controles no autorizados, por ejemplo `DOCUMENTO_REPROCESAR`, `DOCUMENTO_DESCARGAR`, `CERTIFICADO_ADMINISTRAR` y `AUDITORIA_VER`.

El backend siempre será la autoridad definitiva: ocultar un botón no reemplaza la autorización del servidor.

## 9. Dashboard

Ruta `/dashboard`.

Tarjetas:
- Total documentos.
- Autorizados.
- En proceso.
- Pendientes.
- No autorizados.
- Con error.
- Correos pendientes.

Gráficos:
- Documentos por día.
- Documentos por tipo.
- Documentos por estado.
- Errores por etapa.
- Autorizados vs. no autorizados.

Alertas:
- Documentos que requieren intervención.
- Certificados próximos a vencer.
- Correos pendientes.
- Documentos pendientes de autorización.

Filtros: hoy, 7 días, 30 días, rango personalizado y empresa.

## 10. Bandeja documental

Ruta `/documentos`.

Columnas:
`Tipo | Número | Fecha | Identificación | Receptor | Total | Estado | Correo | Recepción | Acciones`.

Filtros server-side:
- Tipo.
- Estado.
- Fecha desde/hasta.
- Identificación.
- Número.
- Clave de acceso.
- External ID.
- Establecimiento.
- Punto de emisión.

Búsqueda rápida con debounce por número, identificación, razón social, clave o externalId.

La paginación y ordenamiento serán server-side. Los filtros relevantes se reflejan en query params para conservarlos al navegar.

## 11. Bandejas de los seis comprobantes

Las rutas específicas reutilizan una base común. Crear `DocumentListComponent` configurable por `tipoDocumento`, título, columnas y filtros adicionales.

No duplicar seis implementaciones completas.

## 12. Detalle documental

Ruta `/documentos/:id`.

Cabecera:
- Tipo.
- Número.
- Estado.
- Clave de acceso.
- Fecha de emisión.
- Empresa.
- Receptor.
- Total.

Acciones condicionadas por estado y permiso:
- Descargar XML.
- Descargar RIDE.
- Consultar SRI.
- Reprocesar.
- Regenerar RIDE.
- Reenviar correo.

Tabs:
1. Resumen.
2. Datos JSON.
3. Archivos.
4. SRI.
5. Historial.
6. Errores.
7. Correos.
8. Auditoría.

## 13. Timeline

Crear `DocumentTimelineComponent` basado exclusivamente en el historial real:

```text
✓ RECIBIDO
│
✓ VALIDADO
│
✓ XML GENERADO
│
✓ FIRMADO
│
✓ RECIBIDO SRI
│
● PENDIENTE AUTORIZACIÓN
│
○ AUTORIZADO
│
○ RIDE
│
○ CORREO
```

El usuario debe comprender rápidamente qué ocurrió, dónde está el documento, qué falta y si requiere intervención.

## 14. JSON y XML

Crear `JsonViewerComponent` con expandir, contraer, copiar y buscar. El JSON original será de solo lectura.

Para XML permitir ver, buscar, copiar y descargar. JSON, XML y mensajes SRI deben renderizarse como texto escapado, nunca mediante `innerHTML`.

## 15. Archivos

Mostrar:
- XML generado.
- XML firmado.
- XML autorizado.
- RIDE PDF.

Tabla: `Tipo | Nombre | Tamaño | Fecha | Acción`.

Las descargas usarán `responseType: 'blob'` y, cuando exista, el nombre indicado por `Content-Disposition`.

## 16. SRI

Mostrar estado de recepción, autorización, número y fecha de autorización, intentos, duración y mensajes.

Tabla:
`Operación | Intento | Fecha | Duración | Resultado | Mensaje`.

## 17. Errores

Mostrar etapa, código, mensaje, fecha, recuperable y resuelto.

Las acciones deben depender de la etapa. Por ejemplo, `ERROR_RIDE` ofrece `Regenerar RIDE`, no un reenvío indiscriminado al SRI.

## 18. Correos

Mostrar destinatario, estado, intentos, fecha y error. Permitir reenvío con permiso.

## 19. Polling

Mientras el documento esté en un estado transitorio, el detalle puede consultar estado aproximadamente cada 5 segundos y detenerse al alcanzar un estado terminal.

No realizar polling global permanente de todos los documentos.

## 20. Factura

Además de la información común mostrar detalles, subtotal, descuentos, impuestos, total y formas de pago.

## 21. Liquidación de compra

Mostrar información específica del proveedor, detalles, impuestos, pagos y totales.

## 22. Nota de crédito

Mostrar documento modificado, motivo, detalles y valor de modificación.

## 23. Nota de débito

Mostrar documento modificado, motivos, impuestos y valores adicionales.

## 24. Retención

Mostrar sujeto retenido, documentos sustento, impuestos, porcentajes y valores retenidos.

## 25. Guía de remisión

Mostrar transportista, placa, fechas, punto de partida, destinos, documentos sustento y productos.

## 26. Empresas

CRUD controlado con RUC, razón social, nombre comercial, dirección y estado.

Detalle por tabs:
`Información | Establecimientos | Certificados | Logos | Configuración SRI | Correo | Plantillas RIDE`.

Usar Reactive Forms.

## 27. Establecimientos y puntos de emisión

Administrar empresa, código, dirección y estado. En puntos de emisión mostrar los secuenciales por tipo documental.

## 28. Secuenciales

Tabla:
`Tipo documento | Valor actual | Estado | Última modificación`.

Modificar un secuencial requiere permiso `SECUENCIAL_ADMINISTRAR`, confirmación y auditoría backend.

## 29. Certificados

Tabla:
`Empresa | Nombre | Emisor | Inicio | Expiración | Días restantes | Estado`.

Estados visuales:
`VÁLIDO | PRÓXIMO A VENCER | VENCIDO | INACTIVO`.

Carga mediante archivo `.p12/.pfx`. No guardar contraseña en localStorage, sessionStorage ni logs.

## 30. Logos y recursos

Permitir subir, previsualizar, activar y desactivar logo principal, secundario y marca de agua.

## 31. Plantillas RIDE

Administrar empresa, tipo de documento, nombre, versión, plantilla predeterminada y estado.

## 32. Configuración SRI

Administrar empresa, ambiente, timeouts, reintentos y estado. Al cambiar de PRUEBAS a PRODUCCIÓN mostrar una confirmación explícita.

## 33. Configuración de correo

Administrar remitente, nombre, envío de XML, envío de RIDE, asunto y plantilla.

## 34. Usuarios

Tabla:
`Usuario | Nombre | Email | Roles | Estado | Último acceso | Acciones`.

Formulario con username, nombre, email, roles y estado. Password únicamente al crear o cambiar explícitamente.

## 35. Roles y permisos

Roles iniciales:
- SUPER_ADMIN.
- ADMIN.
- OPERADOR.
- CONSULTA.
- AUDITOR.

Crear una matriz visual de permisos, enviando al backend los códigos reales.

## 36. Auditoría

Filtros por usuario, acción, entidad, fechas y resultado.

Tabla:
`Fecha | Usuario | Acción | Entidad | Resultado | IP | Request ID`.

Detalle con información anterior/posterior cuando la API la entregue.

## 37. Monitoreo

Mostrar estado de backend, base de datos, storage, SRI Recepción, SRI Autorización, correo y certificado.

También:
- Recibidos pendientes.
- Pendientes de autorización.
- Correos pendientes.
- Requieren intervención.

## 38. Modelos TypeScript

Los modelos deben reflejar los DTO Response de Spring Boot. No inventar enums diferentes al backend.

Ejemplo:

```typescript
export interface DocumentoResumen {
  id: string;
  externalId: string;
  tipoDocumento: TipoDocumento;
  numeroDocumento: string;
  fechaEmision: string;
  identificacionReceptor: string;
  razonSocialReceptor: string;
  total: number;
  estado: EstadoDocumento;
}
```

## 39. Servicios API

Crear:
- `DocumentoApiService`.
- `DashboardApiService`.
- `EmpresaApiService`.
- `EstablecimientoApiService`.
- `PuntoEmisionApiService`.
- `CertificadoApiService`.
- `ConfiguracionApiService`.
- `UsuarioApiService`.
- `RolApiService`.
- `AuditoriaApiService`.
- `CatalogoApiService`.

Los componentes no deben utilizar `HttpClient` directamente salvo casos excepcionales; la comunicación se centraliza en servicios.

## 40. Estado local

Usar Signals para loading, usuario, selección, empresa activa y estados UI simples.

Usar RxJS para HTTP, debounce, switchMap, combinación de filtros y polling controlado.

## 41. Multiempresa

Crear `CompanyContextService` para empresa seleccionada, empresas disponibles y cambio de contexto.

No asumir una sola empresa en los componentes.

## 42. Componentes compartidos

Crear:
- PageHeader.
- StatusChip.
- DocumentTypeChip.
- ConfirmDialog.
- EmptyState.
- ErrorState.
- Loading/Skeleton.
- SearchBox.
- DateRangeFilter.
- FileDownload.
- JsonViewer.
- DocumentTimeline.

No duplicar confirmaciones, estados o lógica de descargas en cada feature.

## 43. UX

Priorizar información y trazabilidad. Evitar gradientes excesivos, animaciones innecesarias y exceso de tarjetas.

Las operaciones críticas siempre requieren confirmación.

Los mensajes deben ser funcionales: `El documento fue programado para reprocesamiento`, no simplemente `HTTP 202`.

## 44. Responsive y accesibilidad

Prioridad: desktop administrativo, tablet y mobile de consulta.

En móvil: sidebar como drawer, acciones en menú y tablas adaptadas.

Implementar labels, ARIA, foco visible, teclado, contraste y mensajes accesibles. No depender solo del color para indicar estado.

## 45. Seguridad

- No secretos en Angular.
- No credenciales productivas en `environment`.
- No `innerHTML` para XML/JSON.
- Backend valida siempre permisos.
- CORS restringido en producción.
- Tokens de vida limitada según soporte backend.
- 401 y 403 tratados de forma diferenciada.

## 46. Environment

Centralizar URL:

```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:9090'
};
```

No hardcodear endpoints en componentes.

## 47. Rendimiento

No cargar XML, historial, errores completos o binarios en bandejas. Solicitar información pesada solo al abrir el detalle.

Usar lazy loading, `@for` con tracking, pipes puros, Signals/computed y consultas server-side.

## 48. Testing

Priorizar:
- AuthService.
- Guards.
- Interceptors.
- DocumentoApiService.
- Filtros/paginación.
- Formularios.
- Descargas.
- Reprocesamiento.
- Acciones críticas.

E2E prioritario:
`login → dashboard → buscar factura → detalle → historial → descargar RIDE`.

## 49. Fases

### Fase 1 — Base
Proyecto Angular, layout, routing, environments, auth, interceptors y shared.

### Fase 2 — Documentos
Bandeja, filtros, paginación, detalle, estado e historial.

### Fase 3 — Operación documental
Timeline, XML, RIDE, SRI, errores y correos.

### Fase 4 — Acciones
Reprocesar, consultar autorización, regenerar RIDE y reenviar correo.

### Fase 5 — Dashboard
Cards, gráficos, alertas y filtros.

### Fase 6 — Administración
Empresas, establecimientos, puntos de emisión, secuenciales, certificados, configuración, recursos y plantillas RIDE.

### Fase 7 — Seguridad
Usuarios, roles y permisos.

### Fase 8 — Control
Auditoría, errores, correos y monitoreo.

## 50. Primer sprint recomendado

Objetivo:

`LOGIN → LAYOUT → BANDEJA → DETALLE → HISTORIAL`.

Crear primero:
- `core/auth`.
- `core/interceptors`.
- `layout`.
- modelos documentales.
- `DocumentoApiService`.
- login.
- lista documental.
- detalle.
- status chip.
- loading/empty/error states.

## 51. Segundo sprint

Completar el detalle operativo:
`Timeline + XML + RIDE + intentos SRI + errores + correo + reprocesamiento`.

## 52. Tercer sprint

`Dashboard + Empresas + Establecimientos + Puntos de emisión + Certificados`.

## 53. Cuarto sprint

`Configuración + Usuarios + Roles + Auditoría + Monitoreo`.

## 54. Criterios de aceptación

- [ ] Angular standalone.
- [ ] Lazy loading.
- [ ] JWT.
- [ ] Guards e interceptors.
- [ ] Control de permisos.
- [ ] Dashboard.
- [ ] Bandeja general.
- [ ] Seis vistas documentales reutilizables.
- [ ] Filtros y paginación server-side.
- [ ] Detalle y timeline.
- [ ] JSON/XML.
- [ ] RIDE.
- [ ] Historial, errores e intentos SRI.
- [ ] Reprocesamiento seguro.
- [ ] Regeneración RIDE.
- [ ] Reenvío correo.
- [ ] Empresas/establecimientos/puntos.
- [ ] Secuenciales.
- [ ] Certificados.
- [ ] Configuración SRI/correo.
- [ ] Recursos y plantillas RIDE.
- [ ] Usuarios/roles/permisos.
- [ ] Auditoría y monitoreo.
- [ ] Responsive.
- [ ] Accesibilidad básica.
- [ ] Tests.
- [ ] Sin secretos en frontend.

## 55. Orden de programación

```text
01 Angular base
02 Layout
03 Routing
04 Auth
05 Interceptors
06 Shared
07 Models
08 API services
09 Bandeja
10 Detalle
11 Timeline
12 Historial
13 Archivos
14 SRI
15 Errores
16 Correos
17 Operaciones
18 Dashboard
19 Empresas
20 Establecimientos
21 Puntos emisión
22 Secuenciales
23 Certificados
24 Configuración SRI
25 Configuración correo
26 Recursos
27 Plantillas RIDE
28 Usuarios
29 Roles
30 Auditoría
31 Monitoreo
32 Responsive
33 Tests
34 Optimización
```

## 56. Regla de integración

El frontend se construirá estrictamente contra `API_SRI_FILES_V1.md`.

Si una pantalla necesita información que la API no expone, no se permitirá acceso directo a PostgreSQL: deberá ampliarse formalmente la API.

## 57. Resultado final

```text
                    ANGULAR
                       │
       ┌───────────────┼────────────────┐
       │               │                │
   DOCUMENTOS       CONTROL       ADMINISTRACIÓN
       │               │                │
   Bandejas         Errores          Empresas
   Detalle          Auditoría        Certificados
   XML/RIDE         Monitoreo        Configuración
   Timeline         Correos          Usuarios/Roles
       │               │                │
       └───────────────┼────────────────┘
                       │
                  REST /api/v1
                       │
                   SPRING BOOT
```

La aplicación Angular será el centro administrativo y de observabilidad de `sri-files`, mientras Spring Boot seguirá siendo responsable de todas las reglas y operaciones tributarias.

## 58. Siguiente documento

Crear posteriormente:

`PLAN_IMPLEMENTACION_FRONTEND_SRI_FILES.md`

con tareas ejecutables por fase, archivos a crear, componentes, modelos, endpoints, pruebas y criterios de aceptación, sincronizado con `API_SRI_FILES_V1.md` y el plan del backend.
