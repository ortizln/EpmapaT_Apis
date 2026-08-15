# PLAN IMPLEMENTACION FRONTEND SRI-FILES

**Proyecto:** Administracion de Documentos Electronicos SRI  
**Frontend objetivo:** Angular 18+ standalone  
**Backend asociado:** `sri-files` Spring Boot `/api/v1`  
**Fecha base:** 2026-08-14  
**Estado:** Plan operativo inicial

## 1. Objetivo

Convertir la arquitectura frontend y la API V1 en tareas ejecutables para construir el panel administrativo Angular de `sri-files` sin inventar contratos paralelos.

El frontend se construira estrictamente contra:

- `MD/ARQUITECTURA_FRONTEND_SRI_FILES.md`
- `MD/API_SRI_FILES_V1.md`
- `MD/CONTRATOS_JSON_SRI_FILES.md`
- `GET /api/v1/documentos/contratos/{tipoDocumento}`

## 2. Principios de implementacion

- Angular standalone, sin NgModules de feature tradicionales.
- Lazy loading por feature.
- JWT con interceptors y guards.
- Signals para estado local y RxJS para IO/polling/filtros.
- Componentes compartidos antes de duplicar UI.
- Server-side pagination, sorting y filtering.
- Nada de acceso directo a BD ni secretos en frontend.
- Si falta informacion en API, se amplia backend; no se improvisa en Angular.

## 3. Estructura objetivo

```text
src/app/
├── core/
│   ├── auth/
│   ├── guards/
│   ├── interceptors/
│   ├── config/
│   └── services/
├── layout/
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

## 4. Fase 0 - Preparacion

### Tareas

1. Crear repositorio o carpeta de frontend Angular separada del backend.
2. Inicializar Angular 18+ standalone con SCSS, routing y strict mode.
3. Instalar Angular Material, Bootstrap 5 y Chart.js.
4. Definir `environment.ts` y `environment.prod.ts`.
5. Registrar convenciones de codigo y estructura de carpetas.

### Archivos a crear

- `angular.json`
- `package.json`
- `src/environments/environment.ts`
- `src/environments/environment.prod.ts`
- `src/styles.scss`
- `src/app/app.routes.ts`

### Criterio de aceptacion

- La app arranca en local.
- Existe base URL configurable.
- No hay endpoints hardcodeados en componentes.

## 5. Fase 1 - Core, Auth y Layout

### Tareas

1. Crear `AuthService`, `TokenService`, `AuthStore` y modelos de usuario autenticado.
2. Implementar `authGuard` y `permissionGuard`.
3. Implementar `authInterceptor`, `errorInterceptor`, `requestIdInterceptor`, `loadingInterceptor`.
4. Crear login con Reactive Forms.
5. Crear layout administrativo: navbar, sidebar, breadcrumb, footer.
6. Mostrar empresa activa y ambiente SRI visible en layout.

### Archivos a crear

- `src/app/core/auth/auth.service.ts`
- `src/app/core/auth/token.service.ts`
- `src/app/core/auth/auth.store.ts`
- `src/app/core/guards/auth.guard.ts`
- `src/app/core/guards/permission.guard.ts`
- `src/app/core/interceptors/auth.interceptor.ts`
- `src/app/core/interceptors/error.interceptor.ts`
- `src/app/core/interceptors/request-id.interceptor.ts`
- `src/app/core/interceptors/loading.interceptor.ts`
- `src/app/layout/admin-layout/admin-layout.component.ts`
- `src/app/layout/sidebar/sidebar.component.ts`
- `src/app/layout/navbar/navbar.component.ts`
- `src/app/features/auth/login/login.component.ts`

### Endpoints usados

- `POST /api/v1/auth/login`
- `GET /api/v1/auth/me`

### Criterio de aceptacion

- Login funcional.
- 401 limpia sesion.
- 403 muestra acceso denegado sin cerrar sesion.
- Sidebar se construye por permisos.

## 6. Fase 2 - Shared y Models base

### Tareas

1. Crear enums y modelos TypeScript alineados con backend.
2. Crear componentes compartidos base.
3. Crear directiva `HasPermissionDirective`.
4. Crear utilidades para estados, fechas, archivos y chips.

### Archivos a crear

- `src/app/models/documento.model.ts`
- `src/app/models/auth.model.ts`
- `src/app/models/common/paged-response.model.ts`
- `src/app/shared/components/page-header/page-header.component.ts`
- `src/app/shared/components/status-chip/status-chip.component.ts`
- `src/app/shared/components/document-type-chip/document-type-chip.component.ts`
- `src/app/shared/components/loading-state/loading-state.component.ts`
- `src/app/shared/components/empty-state/empty-state.component.ts`
- `src/app/shared/components/error-state/error-state.component.ts`
- `src/app/shared/components/confirm-dialog/confirm-dialog.component.ts`
- `src/app/shared/directives/has-permission.directive.ts`

### Criterio de aceptacion

- Los enums coinciden con backend.
- No hay literales de estado repetidos por toda la UI.

## 7. Fase 3 - Servicios API base

### Tareas

1. Crear `DocumentoApiService`.
2. Crear `CatalogoApiService`.
3. Crear `DashboardApiService`.
4. Preparar servicios administrativos vacios o parciales para siguientes fases.
5. Centralizar todo `HttpClient` en servicios.

### Archivos a crear

- `src/app/services/documento-api.service.ts`
- `src/app/services/catalogo-api.service.ts`
- `src/app/services/dashboard-api.service.ts`
- `src/app/services/empresa-api.service.ts`
- `src/app/services/usuario-api.service.ts`

### Endpoints usados inicialmente

- `GET /api/v1/catalogos/tipos-documento`
- `GET /api/v1/catalogos/estados-documento`
- `GET /api/v1/documentos`
- `GET /api/v1/documentos/{uuid}`
- `GET /api/v1/documentos/{uuid}/estado`
- `GET /api/v1/documentos/contratos/{tipoDocumento}`

### Criterio de aceptacion

- Los componentes no usan `HttpClient` directo.
- Existe interfaz para paginacion server-side.

## 8. Fase 4 - Bandeja documental

### Tareas

1. Crear `DocumentListComponent` reusable.
2. Implementar filtros server-side y sincronizacion con query params.
3. Implementar busqueda rapida con debounce.
4. Implementar columnas base y acciones condicionales por permiso.
5. Crear wrappers por tipo documental reutilizando la misma base.

### Archivos a crear

- `src/app/features/documentos/document-list/document-list.component.ts`
- `src/app/features/documentos/document-filters/document-filters.component.ts`
- `src/app/features/documentos/documentos.routes.ts`
- `src/app/features/facturas/facturas-page.component.ts`
- `src/app/features/notas-credito/notas-credito-page.component.ts`
- `src/app/features/notas-debito/notas-debito-page.component.ts`
- `src/app/features/retenciones/retenciones-page.component.ts`
- `src/app/features/guias-remision/guias-remision-page.component.ts`
- `src/app/features/liquidaciones/liquidaciones-page.component.ts`

### Endpoints usados

- `GET /api/v1/documentos`
- `GET /api/v1/documentos/search?q=...`

### Criterio de aceptacion

- Paginacion, filtros y ordenamiento son server-side.
- Los seis tipos reutilizan la misma base.

## 9. Fase 5 - Detalle documental

### Tareas

1. Crear `DocumentDetailComponent`.
2. Crear tabs: resumen, JSON, archivos, SRI, historial, errores, correos, auditoria.
3. Crear `JsonViewerComponent`.
4. Crear `DocumentTimelineComponent`.
5. Habilitar polling controlado para estados transitorios.

### Archivos a crear

- `src/app/features/documentos/document-detail/document-detail.component.ts`
- `src/app/shared/components/json-viewer/json-viewer.component.ts`
- `src/app/shared/components/document-timeline/document-timeline.component.ts`
- `src/app/features/documentos/document-summary/document-summary.component.ts`
- `src/app/features/documentos/document-files/document-files.component.ts`
- `src/app/features/documentos/document-sri/document-sri.component.ts`
- `src/app/features/documentos/document-errors/document-errors.component.ts`
- `src/app/features/documentos/document-mail/document-mail.component.ts`

### Endpoints usados

- `GET /api/v1/documentos/{uuid}`
- `GET /api/v1/documentos/{uuid}/estado`
- `GET /api/v1/documentos/{uuid}/historial`
- `GET /api/v1/documentos/{uuid}/errores`
- `GET /api/v1/documentos/{uuid}/intentos-sri`
- `GET /api/v1/documentos/{uuid}/correos`
- `GET /api/v1/documentos/{uuid}/archivos`

### Criterio de aceptacion

- El usuario entiende etapa actual y pendientes.
- JSON y XML se muestran escapados.
- Polling se detiene en estado terminal.

## 10. Fase 6 - Descargas y operaciones documentales

### Tareas

1. Implementar descarga de XML/RIDE por blob.
2. Implementar reprocesamiento.
3. Implementar consulta manual de autorizacion.
4. Implementar regeneracion de RIDE.
5. Implementar reenvio de correo.
6. Usar confirmaciones para acciones criticas.

### Archivos a crear

- `src/app/shared/components/file-download/file-download.component.ts`
- `src/app/features/documentos/document-actions/document-actions.component.ts`

### Endpoints usados

- `GET /api/v1/documentos/{uuid}/xml`
- `GET /api/v1/documentos/{uuid}/xml-firmado`
- `GET /api/v1/documentos/{uuid}/xml-autorizado`
- `GET /api/v1/documentos/{uuid}/ride`
- `POST /api/v1/documentos/{uuid}/reprocesar`
- `POST /api/v1/documentos/{uuid}/consultar-autorizacion`
- `POST /api/v1/documentos/{uuid}/regenerar-ride`
- `POST /api/v1/documentos/{uuid}/reenviar-correo`

### Criterio de aceptacion

- Las acciones solo aparecen con permiso y estado compatible.
- Los mensajes son funcionales, no tecnicos.

## 11. Fase 7 - Formularios por tipo documental

### Tareas

1. Crear builders de formularios basados en contrato backend.
2. Consumir `GET /api/v1/documentos/contratos/{tipoDocumento}` para metadata de campos.
3. Construir formularios para:
   - factura
   - liquidacion
   - nota credito
   - nota debito
   - retencion
   - guia remision
4. Crear validadores compartidos por tipo.

### Archivos a crear

- `src/app/features/documentos/document-contract.service.ts`
- `src/app/features/facturas/factura-form.component.ts`
- `src/app/features/notas-credito/nota-credito-form.component.ts`
- `src/app/features/notas-debito/nota-debito-form.component.ts`
- `src/app/features/retenciones/retencion-form.component.ts`
- `src/app/features/guias-remision/guia-remision-form.component.ts`
- `src/app/shared/validators/documento.validators.ts`

### Endpoints usados

- `GET /api/v1/documentos/contratos/{tipoDocumento}`
- `POST /api/v1/documentos`

### Criterio de aceptacion

- Los formularios no inventan campos fuera del contrato.
- Los tipos complejos soportan arrays reales como `detalles`, `destinatarios` y `motivos`.

## 12. Fase 8 - Dashboard

### Tareas

1. Crear vista `/dashboard`.
2. Tarjetas de resumen.
3. Graficos por tipo, estado, dia y errores por etapa.
4. Alertas operativas.
5. Filtros por rango y empresa.

### Archivos a crear

- `src/app/features/dashboard/dashboard-page.component.ts`
- `src/app/features/dashboard/dashboard-filters.component.ts`
- `src/app/features/dashboard/dashboard-charts.component.ts`

### Endpoints usados

- `GET /api/v1/dashboard/resumen`
- `GET /api/v1/dashboard/documentos-por-tipo`
- `GET /api/v1/dashboard/documentos-por-estado`
- `GET /api/v1/dashboard/documentos-por-dia`
- `GET /api/v1/dashboard/errores-por-etapa`
- `GET /api/v1/dashboard/tiempos`

### Criterio de aceptacion

- Dashboard usable en escritorio y tablet.
- Filtros reflejados en URL cuando aplique.

## 13. Fase 9 - Multiempresa y catalogos

### Tareas

1. Crear `CompanyContextService`.
2. Seleccion de empresa activa.
3. Cargar catalogos reutilizables.
4. Refrescar vistas dependientes al cambiar contexto.

### Archivos a crear

- `src/app/core/services/company-context.service.ts`
- `src/app/core/services/app-catalogs.service.ts`

### Criterio de aceptacion

- Ninguna pantalla asume una sola empresa fija.

## 14. Fase 10 - Administracion

### Tareas

1. CRUD empresas.
2. CRUD establecimientos.
3. CRUD puntos de emision.
4. Gestion de secuenciales.
5. Gestion de certificados.
6. Configuracion SRI y correo.
7. Recursos y plantillas RIDE.

### Endpoints usados

- `/api/v1/empresas`
- `/api/v1/establecimientos/...`
- `/api/v1/puntos-emision/...`
- `/api/v1/puntos-emision/{uuid}/secuenciales`
- `/api/v1/empresas/{empresaId}/certificados`
- `/api/v1/empresas/{empresaId}/configuracion-sri`
- `/api/v1/empresas/{empresaId}/configuracion-correo`
- `/api/v1/empresas/{empresaId}/recursos`
- `/api/v1/empresas/{empresaId}/plantillas-ride`

### Criterio de aceptacion

- Toda operacion sensible usa permiso y confirmacion.

## 15. Fase 11 - Seguridad y control

### Tareas

1. CRUD usuarios.
2. CRUD roles.
3. Matriz visual de permisos.
4. Auditoria.
5. Monitoreo.
6. Bandeja de correos pendientes.

### Endpoints usados

- `/api/v1/usuarios`
- `/api/v1/roles`
- `/api/v1/permisos`
- `/api/v1/auditoria`
- `/api/v1/monitoring/status`
- `/api/v1/monitoring/pending`
- `/api/v1/correos`

### Criterio de aceptacion

- Roles y permisos visibles y editables sin inventar codigos.

## 16. Fase 12 - Testing

### Unit tests prioritarios

- `AuthService`
- guards
- interceptors
- `DocumentoApiService`
- filtros de bandeja
- polling de detalle
- formularios documentales
- descargas
- acciones criticas

### E2E prioritarios

1. `login -> dashboard`
2. `dashboard -> bandeja -> detalle`
3. `detalle -> historial -> xml -> ride`
4. `buscar factura -> abrir -> descargar ride`
5. `nota credito -> formulario -> enviar`

## 17. Modelos frontend minimos

### Base documental

- `DocumentoResumen`
- `DocumentoDetalle`
- `DocumentoEstado`
- `DocumentoHistorialItem`
- `DocumentoErrorItem`
- `DocumentoCorreoItem`
- `DocumentoArchivoItem`
- `DocumentoContrato`
- `DocumentoSeccionContrato`
- `DocumentoCampoContrato`

### Seguridad

- `LoginRequest`
- `LoginResponse`
- `UsuarioAutenticado`
- `Rol`
- `Permiso`

### Dashboard

- `DashboardResumen`
- `SerieCantidadPorTipo`
- `SerieCantidadPorEstado`
- `SerieCantidadPorDia`

## 18. Servicios frontend obligatorios

- `AuthService`
- `TokenService`
- `DocumentoApiService`
- `DashboardApiService`
- `CatalogoApiService`
- `CompanyContextService`
- `EmpresaApiService`
- `EstablecimientoApiService`
- `PuntoEmisionApiService`
- `CertificadoApiService`
- `ConfiguracionApiService`
- `UsuarioApiService`
- `RolApiService`
- `AuditoriaApiService`

## 19. Dependencias backend minimas para arrancar Angular

El frontend puede arrancar ya mismo con la base actual si usamos primero:

- `POST /api/v1/auth/login`
- `GET /api/v1/auth/me`
- `GET /api/v1/documentos/{uuid}`
- `GET /api/v1/documentos/{uuid}/estado`
- `GET /api/v1/documentos/contratos/{tipoDocumento}`

Para la bandeja general completa todavia faltan varias piezas de API V1 descritas en la documentacion, especialmente:

- `GET /api/v1/documentos` paginado real
- historial documental
- errores del documento
- intentos SRI
- archivos y descargas expuestas
- operaciones manuales
- dashboard

## 20. Orden recomendado de ejecucion real

1. Crear repo Angular.
2. Implementar core/auth/layout/shared.
3. Implementar modelos + servicios base.
4. Implementar bandeja general mockeada o con backend parcial.
5. Implementar detalle documental.
6. Implementar contrato dinamico y formularios por tipo.
7. Completar operaciones.
8. Completar dashboard.
9. Completar administracion.
10. Completar seguridad/control.

## 21. Backlog de ampliaciones backend para soportar frontend

- Exponer `GET /api/v1/documentos` paginado real.
- Exponer historial documental.
- Exponer errores por documento.
- Exponer intentos SRI.
- Exponer archivos listables y descargables.
- Exponer operaciones manuales seguras.
- Exponer dashboard.
- Exponer catalogos.
- Exponer empresas y contexto multiempresa.

## 22. Criterios de aceptacion del plan

- Existe un plan por fases con orden de construccion.
- Cada fase referencia archivos/componentes/servicios.
- El frontend se alinea con API V1 y no con contratos inventados.
- Se identifica claramente que ya esta listo en backend y que aun falta.

## 23. Primer sprint recomendado

Objetivo:

`LOGIN -> LAYOUT -> DOCUMENTOS -> DETALLE -> ESTADO`

Entregables:

- login funcional
- layout administrativo
- `DocumentoApiService`
- bandeja base
- detalle base
- chips/estados/shared base

## 24. Segundo sprint recomendado

Objetivo:

`TIMELINE -> JSON/XML -> RIDE -> ERRORES -> CORREOS -> OPERACIONES`

## 25. Tercer sprint recomendado

Objetivo:

`FORMULARIOS DINAMICOS -> DASHBOARD -> MULTIEMPRESA`

## 26. Cuarto sprint recomendado

Objetivo:

`ADMINISTRACION -> SEGURIDAD -> AUDITORIA -> MONITOREO`
