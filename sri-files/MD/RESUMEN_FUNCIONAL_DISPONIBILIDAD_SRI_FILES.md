# Resumen Funcional y Disponibilidad del Sistema SRI-FILES

**Fecha de revision:** 2026-09-11
**Revisado sobre:** codigo fuente real (backend `backend/src`, frontend `frontend/src`) y documentacion existente en `MD/`.
**Proposito:** servir como insumo para elaborar un plan completo de implementacion.

---

## 1. Identificacion del sistema

| Campo | Valor |
|---|---|
| Nombre | SRI-FILES (Plataforma de Administracion de Documentos Electronicos SRI) |
| Entidad | EPMAPA-T (Empresa Publica Municipal de Agua Potable y Alcantarillado de Tulcan) |
| Backend | Spring Boot, Java (Maven), puerto 9090, PostgreSQL |
| Frontend | Angular 18.2 (standalone), Bootstrap 5.3.8, bootstrap-icons |
| API | REST `/api/v1` (nueva) + `/api/singsend` (legacy) |
| Autenticacion | JWT (token artesanal HMAC) |
| Despliegue | Backend en Docker; Frontend en Nginx nativo |
| Base de datos | PostgreSQL, migraciones Flyway V1..V13 (deshabilitadas por defecto) |

---

## 2. Vision general y arquitectura

`sri-files` es una plataforma que recibe documentos electronicos (facturas, retenciones, notas, guias, liquidaciones), los procesa a traves de un pipeline que genera XML, lo firma digitalmente (XMLDSIG / XAdES-BES), lo envia al SRI (SOAP), obtiene autorizacion, genera el RIDE en PDF con JasperReports y envia el comprobante por correo. Incluye ademas administracion por empresa (establecimientos, puntos de emision, secuenciales, certificados, plantillas RIDE), seguridad (usuarios, roles, permisos), auditoria, monitoreo y dashboard.

El sistema esta compuesto por dos mundos que coexisten:

- **Nuevo (`/api/v1`):** paquete `controller/`, 20 controladores REST + 15 servicios administrativos. Es la plataforma actual.
- **Legacy (`/api/singsend`):** paquete `controllers/`, 2 controladores (`SRI_Controller` con ~20 rutas y `FacturaQueryController`). Se mantiene para compatibilidad y debe retirarse gradualmente.

---

## 3. Inventario funcional del Backend (`/api/v1`)

### 3.1 Modulos y disponibilidad

| Modulo | Controlador | Estado |
|---|---|---|
| Autenticacion | AuthController | Completo |
| Documentos | DocumentoController | Completo |
| Dashboard | DashboardController | Completo |
| Catalogos SRI | CatalogoController | Completo |
| Catalogos comerciales | CatalogoComercialController | Completo |
| Empresas | EmpresaController | Completo |
| Configuracion por empresa (SRI/correo) | ConfiguracionEmpresaController | Completo |
| Establecimientos | EstablecimientoController | Completo |
| Puntos de emision | PuntoEmisionController | Completo |
| Secuenciales | SecuencialController | Completo |
| Certificados | CertificadoController | Completo |
| Recursos / logos | RecursoEmpresaController | Completo |
| Plantillas RIDE | PlantillaRideController | Completo |
| Usuarios | UsuarioController | Completo |
| Roles y permisos | AccessControlController | Completo |
| Auditoria | AuditoriaController | Completo |
| Monitoreo | MonitorController | Completo |
| Alias de monitoreo | MonitoringCompatibilityController | Completo (duplicado por compatibilidad) |

### 3.2 Endpoints exactos por modulo

#### Autenticacion
```
POST /api/v1/auth/login
GET  /api/v1/auth/me
```

#### Documentos (~26 rutas)
```
POST /api/v1/documentos                          (recepcion, header Idempotency-Key, responde 202)
GET  /api/v1/documentos                          (listado paginado con filtros empresaUuid/tipoDocumento/estado/busqueda)
GET  /api/v1/documentos/search                   (busqueda rapida por clave/número/identificacion/razon social/externalId)
GET  /api/v1/documentos/export                   (exportacion CSV)
GET  /api/v1/documentos/resumen                  (resumen de la bandeja)
GET  /api/v1/documentos/auditoria                (auditoria transversal de documentos)
GET  /api/v1/documentos/contratos/{tipoDocumento}(contrato JSON por tipo)
GET  /api/v1/documentos/{uuid}
GET  /api/v1/documentos/{uuid}/estado
GET  /api/v1/documentos/{uuid}/historial
GET  /api/v1/documentos/{uuid}/errores
GET  /api/v1/documentos/{uuid}/intentos-sri
GET  /api/v1/documentos/{uuid}/archivos
GET  /api/v1/documentos/{uuid}/xml
GET  /api/v1/documentos/{uuid}/xml-firmado
GET  /api/v1/documentos/{uuid}/xml-autorizado
GET  /api/v1/documentos/{uuid}/ride
GET  /api/v1/documentos/{uuid}/ride/contrato
GET  /api/v1/documentos/{uuid}/correo
GET  /api/v1/documentos/{uuid}/correos
GET  /api/v1/documentos/autorizacion             (?claveAcceso)
POST /api/v1/documentos/{uuid}/reenviar-correo
POST /api/v1/documentos/{uuid}/reprocesar
POST /api/v1/documentos/{uuid}/xml-sin-firmar     (multipart)
POST /api/v1/documentos/{uuid}/regenerar-ride
POST /api/v1/documentos/{uuid}/consultar-autorizacion
```

#### Dashboard
```
GET /api/v1/dashboard/resumen
GET /api/v1/dashboard/documentos-por-tipo
GET /api/v1/dashboard/documentos-por-estado
GET /api/v1/dashboard/documentos-por-dia
GET /api/v1/dashboard/errores-por-etapa
GET /api/v1/dashboard/tiempos
```

#### Catalogos
```
GET /api/v1/catalogos/tipos-documento
GET /api/v1/catalogos/estados-documento
GET /api/v1/catalogos/tipos-identificacion
GET /api/v1/catalogos/formas-pago
GET /api/v1/catalogos/impuestos
GET /api/v1/catalogos/codigos-retencion
```

#### Catalogos comerciales (20 endpoints: clientes, productos, formas-pago, iva)
```
GET/POST /api/v1/catalogos-comerciales/{entidad}
GET/PUT /api/v1/catalogos-comerciales/{entidad}/{uuid}
PATCH /api/v1/catalogos-comerciales/{entidad}/{uuid}/estado
```

#### Empresas
```
GET    /api/v1/empresas
POST   /api/v1/empresas
GET    /api/v1/empresas/{uuid}
PUT    /api/v1/empresas/{uuid}
PATCH  /api/v1/empresas/{uuid}/estado
GET    /api/v1/empresas/{uuid}/configuracion
PUT    /api/v1/empresas/{uuid}/configuracion
GET    /api/v1/empresas/{uuid}/configuracion-sri
PUT    /api/v1/empresas/{uuid}/configuracion-sri
GET    /api/v1/empresas/{uuid}/configuracion-correo
PUT    /api/v1/empresas/{uuid}/configuracion-correo
GET    /api/v1/empresas/auditoria-reciente
GET    /api/v1/empresas/{uuid}/auditoria
```

#### Establecimientos
```
GET/POST /api/v1/empresas/{empresaUuid}/establecimientos
GET/PUT  /api/v1/establecimientos/{uuid}
PATCH    /api/v1/establecimientos/{uuid}/estado
```

#### Puntos de emision
```
GET/POST /api/v1/establecimientos/{establecimientoUuid}/puntos-emision
GET/PUT  /api/v1/puntos-emision/{uuid}
PATCH    /api/v1/puntos-emision/{uuid}/estado
```

#### Secuenciales
```
GET /api/v1/puntos-emision/{puntoEmisionUuid}/secuenciales
PUT /api/v1/puntos-emision/{puntoEmisionUuid}/secuenciales/{tipoDocumento}
```

#### Certificados
```
GET   /api/v1/empresas/{empresaId}/certificados
POST  /api/v1/empresas/{empresaId}/certificados     (multipart)
POST  /api/v1/certificados/{uuid}/verificar
PATCH /api/v1/certificados/{uuid}/estado
```

#### Recursos / logos
```
GET   /api/v1/empresas/{empresaId}/recursos
POST  /api/v1/empresas/{empresaId}/recursos         (multipart, LOGO_PRINCIPAL/LOGO_SECUNDARIO/MARCA_AGUA)
PATCH /api/v1/recursos/{uuid}/estado
```

#### Plantillas RIDE
```
GET   /api/v1/empresas/{empresaId}/plantillas-ride
POST  /api/v1/empresas/{empresaId}/plantillas-ride  (multipart .jrxml)
PUT   /api/v1/plantillas-ride/{uuid}                (multipart)
PATCH /api/v1/plantillas-ride/{uuid}/estado
POST  /api/v1/plantillas-ride/{uuid}/verificar
GET   /api/v1/plantillas-ride/base/{tipoDocumento}  (plantilla base descargable)
GET   /api/v1/plantillas-ride/{uuid}/preview/{documentoUuid}
```

#### Usuarios
```
GET    /api/v1/usuarios
POST   /api/v1/usuarios
PUT    /api/v1/usuarios/{uuid}
PATCH  /api/v1/usuarios/{uuid}/estado
PATCH  /api/v1/usuarios/{uuid}/password
GET    /api/v1/usuarios/{uuid}/auditoria
GET    /api/v1/usuarios/auditoria-reciente
```

#### Roles y permisos
```
GET    /api/v1/roles
POST   /api/v1/roles
GET    /api/v1/roles/{codigo}
PUT    /api/v1/roles/{codigo}
GET    /api/v1/permisos
GET    /api/v1/roles/auditoria-reciente
GET    /api/v1/roles/{codigo}/auditoria
```

#### Auditoria
```
GET /api/v1/auditoria
GET /api/v1/auditoria/{id}
```

#### Monitoreo
```
GET /api/v1/monitoreo/health
GET /api/v1/monitoreo/resumen
GET /api/v1/monitoreo/pendientes
GET /api/v1/monitoreo/correos
```
Alias de compatibilidad `GET /api/v1/monitoring/{status|pending|summary|emails}`.

### 3.3 Pipeline de procesamiento documental (estado nuevo)

1. `POST /api/v1/documentos` -> `DocumentoRecepcionService.recibir()`: valida contrato, idempotencia, crea `DocumentoElectronico` en estado `RECIBIDO`, guarda JSON original y marca `PENDIENTE_PROCESAR`.
2. `DocumentoPendingScheduler` (cron cada minuto) -> `DocumentoProcessingService.procesarPendientesRecibidos()`.
3. `DocumentoProcessorFactory` selecciona procesador por tipo (`FacturaProcessor`, `GuiaRemisionProcessor`, `NotaCreditoProcessor`, `NotaDebitoProcessor`, `RetencionProcessor`).
4. `DocumentoWorkflowService.procesar()` recorre las etapas:
   `VALIDACION -> XML -> FIRMA -> ENVIO_SRI -> AUTORIZACION -> RIDE -> CORREO`
   con registro en `documento_estado_historial` y eventos de error en `documento_error`.

Tipos de documento soportados: `FACTURA`, `LIQUIDACION_COMPRA`, `NOTA_CREDITO`, `NOTA_DEBITO`, `GUIA_REMISION`, `RETENCION`.

Maquina de estados: enum `DocumentoEstado` (27 valores) y `DocumentoEtapa` (VALIDACION, XML, FIRMA, ENVIO_SRI, AUTORIZACION, RIDE, CORREO).

### 3.4 Servicios nuevos (capa `service/`, 34 clases)

`DocumentoRecepcionService`, `DocumentoApplicationService` (facade), `DocumentoContratoService`, `DocumentoConsultaService`, `DocumentoOperacionService`, `DocumentoProcessingService`, `DocumentoWorkflowService`, `DocumentoXmlService`, `DocumentoXmlValidationService`, `DocumentoRideService`, `EstadoDocumentoService`, `DocumentoErrorService`, `ArchivoDocumentoService`, `EmpresaService`, `EmpresaConfiguracionService`, `EstablecimientoService`, `PuntoEmisionService`, `SecuencialService`, `CatalogoService`, `CatalogoComercialService`, `UsuarioSistemaService`, `AccessControlService`, `AuditoriaService`, `AuthService`, `PasswordHashService`, `RecursoEmpresaService`, `PlantillaRideAdminService`, `PlantillaRidePreviewService`, `RideContratoService`, `RideTemplateCatalogService`, `JasperRideTemplateRenderer`, `DashboardService`, `MonitorService`, `BasicPdfDocumentService`.

### 3.5 Componentes tecnicos clave

- **Firma electronica:** `signature/FirmaElectronicaService` + `LegacyFirmaElectronicaService`; `utils/FirmaComprobantesService` con modos XMLDSIG y XAdES-BES (`XadesBesService`, `XmlDsigService`, `XmlSignatureVerifier`, `Pkcs12Loader`). Certificado desde tabla `DEFINIR` (id=1).
- **Integracion SRI SOAP:** puertos `SriRecepcionPort` y `SriAutorizacionPort`, adaptadores en `sri/adapter/soap/`, WSDL locales en `classpath:wsdl/`. `SendXmlToSriService` (legacy) tambien usado por el nuevo pipeline.
- **RIDE:** `ride/RideService` + `JasperRideService`; plantillas `.jrxml` en `resources/reports/templates/` (factura, guia, nota credito, nota debito, retencion, liquidacion compra).
- **Storage:** `storage/StorageService` + `LocalStorageService`, ruta `YYYY/MM/tipoDocumento/claveAcceso`, configurable con `sri-files.storage.root`.
- **Scheduler:** pool de 4 hilos; `DocumentoPendingScheduler` (nuevo) y `EnvioSriBatchService` (legacy, flag `sri.legacy-scheduler.enabled`).
- **Correo:** `mail/EmailClient` + `EmailMicroserviceClient` (delega en `MailService`, Thymeleaf, adjuntos base64, limite 20MB, retry 3); `CorreoDocumentoService`.
- **Seguridad:** JWT artesanal (`payload|codigo HMAC`), secreto por defecto `sri-files-dev-secret`, expiracion 28800s; `AuthInterceptor` protege rutas seleccionadas de `/api/v1`; CORS solo `http://localhost:4200`.
- **Excepciones:** `GlobalExceptionHandler` con 8 handlers.

### 3.6 API legacy `/api/singsend` (compatibilidad)

`SRI_Controller` (~20 rutas):
```
POST /factura/xml, /factura/string, /factura
GET  /generar-pdf, /factura_electronica
POST /retencion (multipart), /retencion/string, /retencion/procesar (multipart)
POST /retenciones/pdf, /autorizacion (?claveAcceso&wait&attempts&sleepMillis)
GET  /retencion/download, /retenciones/download, /retenciones/pdf, /retenciones/xml
POST /retencion/mail, /retenciones/mail, /send, /send-template
POST /autorizacion/by-xml
GET  /health
```
`FacturaQueryController`:
```
GET /facturas-por-abonado?idabonado=
GET /facturas-por-cliente-cedula?cedula=
```
Nota: **sin autenticacion y sin auditoria**. Su retirada debe planificarse contra los consumidores actuales.

### 3.7 Modelo de datos

PostgreSQL, 13 tablas nuevas via Flyway (V1..V13):

- **Documental:** `empresa`, `establecimiento`, `punto_emision`, `secuencial`, `documento_electronico`, `documento_estado_historial`, `documento_archivo`, `documento_error`, `recurso_empresa`, `plantilla_ride`.
- **Comerciales:** `cliente`, `producto`, `forma_pago`, `iva_tarifa` (catalogos comerciales).
- **Seguridad:** `usuario_sistema`, `usuario_auditoria`, `rol`, `rol_auditoria`, `permiso`, `rol_permiso`, `empresa_auditoria`.
- **Legacy:** `factura`, `facturas`, `factura_detalle`, `factura_detalle_impuesto`, `factura_pago`, `definir`, `tabla15`.

Script completo adicional: `backend/database/sri-files-full-schema.sql`.

Importante: `spring.flyway.enabled=false` y `JPA ddl-auto=none` por defecto; el esquema debe crearse con el script SQL.

---

## 4. Inventario funcional del Frontend Angular

### 4.1 Rutas implementadas

| Ruta | Pantalla | Estado |
|---|---|---|
| `/login` | Login (username/password, JWT) | Completo |
| `/dashboard` | KPIs y graficas del dashboard | Completo |
| `/documentos` `/documentos/nuevo` `/documentos/:id` | Bandeja, recepcion con formulario dinamico por contrato, detalle con 8 tabs (resumen, json, XML, RIDE, SRI, historial, correos, auditoria) | Completo |
| `/facturacion` `/facturacion/nuevo` | Bandeja FACTURA + recepcion dedicada | Completo |
| `/retenciones` `/retenciones/nuevo` | Idem RETENCION | Completo |
| `/guias-remision` `/guias-remision/nuevo` | Idem GUIA_REMISION | Completo |
| `/notas-credito` `/notas-credito/nuevo` | Idem NOTA_CREDITO | Completo |
| `/notas-debito` `/notas-debito/nuevo` | Idem NOTA_DEBITO | Completo |
| `/liquidaciones-compra` `/liquidaciones-compra/nuevo` | Idem LIQUIDACION_COMPRA | Completo |
| `/catalogos` y aliases | Hub administracion: empresas / establecimientos / puntos de emision / secuenciales | Completo |
| `/usuarios`, `/seguridad/usuarios`, crear/editar | CRUD usuarios, activar/desactivar, cambio password, auditoria por usuario | Completo |
| `/seguridad/roles` | Matriz de roles y permisos | Completo |
| `/administracion/certificados` | Gestion de certificados | **ROTA** (falta el feature) |
| `/administracion/configuracion-correo` | Config correo por empresa | Completo |
| `/administracion/configuracion-sri` | Config SRI | Completo (permiso erroneo, ver seccion 6) |
| `/administracion/plantillas-ride` | CRUD plantillas RIDE, recursos, verificar, preview | Completo |
| `/administracion/catalogos-comerciales` | Clientes / productos / formas de pago / tarifas IVA | Completo |
| `/control/errores` | Errores por etapa | Completo |
| `/control/correos` | Correos pendientes/error | Completo |
| `/control/monitoreo` | Health, resumen, pendientes, auto-refresh | Completo |
| `/control/auditoria` | Resumen central de auditoria | Completo |
| `/control/auditoria-documentos` `/auditoria-usuarios` `/auditoria-roles` `/auditoria-empresas` | Auditorias por dominio | Completo |

### 4.2 Infraestructura frontend

- **Auth:** `LoginComponent`, `AuthService` (login, me, restoreSession), `AuthStore` (signals), `TokenService`, `auth.guard`, `permission.guard`.
- **Interceptores:** request-id (X-Request-Id), auth (Bearer), loading, error (401->logout, 5xx->dashboard).
- **Layout:** sidebar (4 grupos: General/Documentos/Administracion/Control, filtrado por permiso), navbar con selector de empresa activa (localStorage `sri-files.empresa-activa`), breadcrumb, footer, alert-center.
- **Shared:** `has-permission.directive`, `status-chip`, `page-header`, `app-modal`, `alert-center`.
- **Servicios de datos (15):** Auth, Dashboard, DocumentoContrato, DocumentoAuditoria, Empresas, Establecimientos, PuntosEmision, Secuenciales, Usuarios, Roles, Monitor, RecursosEmpresa, PlantillasRide, CatalogosComerciales, Acceso (permisos/roles fallback).
- **Modelos (12):** auth, documento, empresa, dashboard, secuencial, punto-emision, establecimiento, monitor, monitor-queue, recurso-empresa, plantilla-ride, catalogos-comerciales.
- **Testing:** solo 1 spec sin cobertura real (`app.component.spec.ts`).
- **Ambiente:** API `http://localhost:9090/api/v1` en dev y prod (sin entorno real de produccion configurado).

---

## 5. Matriz de disponibilidad consolidada

| Funcionalidad | Backend | Frontend | Observacion |
|---|:---:|:---:|---|
| Autenticacion JWT | Si | Si | |
| Recepcion de documentos (6 tipos) | Si | Si | Idempotencia por externalId |
| Pipeline completo (XML->Firma->SRI->RIDE->Correo) | Si | n/a | Scheduler cada 1 min |
| Bandeja + filtros + detalle | Si | Si | |
| Busqueda rapida (`/search`) | Si | Si | |
| Exportacion CSV (`/export`) | Si | Si | |
| Descargas XML/RIDE | Si | Si | |
| Reproceso, consultar autorizacion, regenerar RIDE, reenvio | Si | Si | |
| Dashboard | Si | Si | 6 endpoints |
| Catalogos SRI | Si | Si | |
| Catalogos comerciales | Si | Si | CRUD completo |
| Empresas | Si | Si | |
| Establecimientos | Si | Si | |
| Puntos de emision | Si | Si | |
| Secuenciales | Si | Si | |
| Certificados digitales | Si | **No (roto)** | Ruta muerta en frontend |
| Configuracion SRI/correo | Si | Si | Permiso equivocado en config-sri |
| Recursos (logos/marca de agua) | Si | Parcial | API ok; gestion visual dentro de plantillas-ride |
| Plantillas RIDE | Si | Si | Verificar, preview, base descargable |
| Usuarios | Si | Si | |
| Roles y permisos | Si | Si | |
| Auditoria | Si | Si | Documentos, usuarios, roles, empresas |
| Monitoreo | Si | Si | `/monitoreo` + alias `/monitoring` |
| API legacy `/api/singsend` | Si (sin auth) | n/a | Pendiente de retiro |
| Webhooks y operaciones masivas | No | No | Evolucion futura (V1 no lo exige) |
| Rate limiting | No | n/a | Recomendado, no obligatorio inicial |

---

## 6. Brechas, defectos y discrepancias detectadas (verificadas en codigo)

### 6.1 Frontend
1. **Ruta rota:** `/administracion/certificados` importa `features/certificados/certificados-page.component`, carpeta inexistente. Causa error de lazy-loading. El backend de certificados esta completo, solo falta la pantalla.
2. **Permiso incorrecto:** la ruta `/administracion/configuracion-sri` usa el permiso `CONFIGURACION_CORREO_ADMINISTRAR` (copia de la ruta de correo). Falta distinguir `CONFIGURACION_SRI_ADMINISTRAR`.
3. **Entorno de produccion:** `environment.prod.ts` apunta a `http://localhost:9090/api/v1`; no hay URL real de produccion ni HTTPS configurado.
4. **Cobertura de tests casi nula:** unico spec sin cobertura.
5. **`index.html` sin personalizar:** titulo "Frontend", `lang="en"`.

### 6.2 Backend
1. **Secreto JWT por defecto en codigo** (`sri-files-dev-secret`) - debe externalizarse por ambiente.
2. **Clave AES hardcodeada** en `config/AESUtil.java` (`"1234567890123456"`) para cifrar la clave del certificado.
3. **Ambiente SRI por defecto = produccion (2)** (`sri-files.sri.default-environment` y `SendXmlToSriService`) - riesgo de envios reales por error de configuracion.
4. **Flyway deshabilitado por defecto** (`spring.flyway.enabled=false`, `ddl-auto=none`) - el esquema depende del script manual.
5. **Legacy `/api/singsend` sin autenticacion** ni auditoria - expuesto en produccion si esta publicado.
6. **Doble ruta de monitoreo** (`/api/v1/monitoreo` y `/api/v1/monitoring`): funcional pero debe unificarse nomenclatura.

### 6.3 Discrepancias documentacion vs codigo
- La documentacion (`ESTADO_ACTUAL_SRI_FILES.md` / `API_SRI_FILES_V1.md`) marcaba `GET /documentos/search` y `GET /documentos/export` como **pendientes**, pero en el codigo **ya existen** en `DocumentoController`. La documentacion esta desactualizada.
- `ESTADO_ACTUAL_SRI_FILES.md` menciona monitoreo solo como `/api/v1/monitoreo`; en codigo existe ademas el alias `/api/v1/monitoring`.
- Los contratos JSON definidos en `CONTRATOS_JSON_SRI_FILES.md` requieren validacion contra la implementacion real de `DocumentoContratoService`.

---

## 7. Requerimientos conocidos de despliegue

- Backend Docker: `backend/Dockerfile`, `deploy-backend-docker.sh`, `start-prod.sh`, `docker-compose.yml`, `.env.prod.example`.
- Frontend Nginx: `deploy-frontend-nginx.sh` (desde raiz), template `frontend/deploy/nginx.sri-files.conf.template`.
- Config sensible requerida: `SRI_AMBIENTE`, `DB_URL/DB_USER/DB_PASS`, `SERVER_PORT`, secretos JWT/AES, clave SMTP, URL real de produccion, CORS de produccion, ruta de storage.

---

## 8. Insumo para el plan completo de implementacion (prioridades sugeridas)

### Fase A - Estabilizacion minima (bloqueantes)
| Tarea | Tipo |
|---|---|
| Crear el feature `certificados` del frontend (CRUD + carga multipart + verificar + activar) | Bug urgente |
| Corregir permiso de la ruta `configuracion-sri` | Bug |
| Externalizar secretos (JWT, AES) + config `environment.prod.ts` real | Seguridad |
| Revisar default `SRI_AMBIENTE` (riesgo de produccion real) | Seguridad |
| Decidir y estandarizar ruta de monitoreo (`monitoreo` vs `monitoring`) | Consistencia |

### Fase B - Alineacion documental
| Tarea | Tipo |
|---|---|
| Actualizar `API_SRI_FILES_V1.md`, `ESTADO_ACTUAL_SRI_FILES.md` con search/export y monitoreo real | Documentacion |
| Validar `CONTRATOS_JSON_SRI_FILES.md` contra `DocumentoContratoService` | Documentacion |
| Validar pantallas contra `ARQUITECTURA_FRONTEND_SRI_FILES.md` y `GUIA_MAESTRA_UX_UI_ANGULAR_V2.md` | Documentacion |
| Formalizar checklist modulo por modulo (backend + frontend) | Plan |

### Fase C - Consolidacion
| Tarea | Tipo |
|---|---|
| Plan de retiro del legacy `/api/singsend` (migrar consumidores a `/api/v1`) | Migracion |
| Activar Flyway de forma controlada y eliminar dependencia del script manual | Infraestructura |
| Rate limiting (`/documentos`, login, operaciones manuales) | Endurecimiento |
| Autenticacion del legacy o bloqueo por red | Seguridad |
| Tests: minima cobertura de servicios de documento y pantallas criticas | Calidad |

### Fase D - Evolucion (opcional)
| Tarea | Tipo |
|---|---|
| Operaciones masivas (`POST /api/v1/documentos/bulk/reprocesar`) con limites y permisos | Futuro |
| Webhooks `DOCUMENTO_AUTORIZADO` para ERP | Futuro |
| Exportacion XLSX, bandeja `GET /api/v1/correos` dedicada | Futuro |
| OpenAPI/Swagger publico en `/swagger-ui.html` | Futuro |

---

## 9. Conclusion

`sri-files` tiene una base **operativa y funcionalmente avanzada** en backend (`/api/v1`, 20 controladores, pipeline completo) y frontend (casi todas las pantallas administrativas). No quedan pendientes estructurales de construccion; lo que falta es:

1. corregir 1 ruta rota y 1 permiso en el frontend;
2. endurecer seguridad de secretos y defaults de ambiente;
3. actualizar la documentacion al estado real;
4. planificar el retiro del legacy y consolidar infraestructura;
5. agregar cobertura de pruebas y cierre de checklist por modulo.

Este documento es la referencia base para redactar el plan completo de implementacion (fases, responsables, prioridades y criterios de aceptacion).

---

### Documentos relacionados en `MD/`
- `API_SRI_FILES_V1.md` - contrato API (desactualizado en search/export)
- `CONTRATOS_JSON_SRI_FILES.md` - contratos de recepcion
- `ARQUITECTURA_BACKEND_SRI_FILES.md` y `ARQUITECTURA_FRONTEND_SRI_FILES.md`
- `MODELO_BASE_DATOS_SRI_FILES.md`
- `ESTADO_ACTUAL_SRI_FILES.md` (2026-08-24, desactualizado)
- `GUIA_JASPER_RIDE_SRI_FILES.md` y `GUIA_MAESTRA_UX_UI_ANGULAR_V2.md`
- `PLAN_IMPLEMENTACION_BACKEND_SRI_FILES.md` y `PLAN_IMPLEMENTACION_FRONTEND_SRI_FILES.md`
- `PLANIFICACION_REESTRUCTURACION_SRI_FILES.md` y `ESTADOS_LEGACY_SRI.md`