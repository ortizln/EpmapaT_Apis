# Estado Actual del Servicio `sri-files`

Fecha de actualizacion: 2026-08-24

## 1. Resumen ejecutivo

`sri-files` ya no debe evaluarse solamente como el servicio legacy basado en `/api/singsend`.

El proyecto evoluciono hacia una plataforma administrativa y operativa con backend Spring Boot bajo `/api/v1`, autenticacion JWT, administracion de documentos electronicos, configuracion por empresa, control de usuarios/roles/permisos, auditoria y gestion de plantillas RIDE con JasperReports.

El estado general actual es:

- `operativo y funcional` en los modulos principales.
- `muy avanzado` respecto al plan de reestructuracion.
- `parcial` en algunos contratos puntuales documentados en los `.md`.
- `con deuda tecnica controlada` por coexistencia de componentes legacy.

## 2. Estado funcional actual

### Backend implementado

Se encuentra implementado el backend administrativo principal bajo `/api/v1` con los siguientes modulos:

- autenticacion:
  - `POST /api/v1/auth/login`
  - `GET /api/v1/auth/me`
- documentos:
  - `POST /api/v1/documentos`
  - `GET /api/v1/documentos`
  - `GET /api/v1/documentos/{uuid}`
  - `GET /api/v1/documentos/{uuid}/estado`
  - `GET /api/v1/documentos/{uuid}/historial`
  - `GET /api/v1/documentos/{uuid}/errores`
  - `GET /api/v1/documentos/{uuid}/intentos-sri`
  - `GET /api/v1/documentos/{uuid}/archivos`
  - `GET /api/v1/documentos/{uuid}/xml`
  - `GET /api/v1/documentos/{uuid}/xml-firmado`
  - `GET /api/v1/documentos/{uuid}/xml-autorizado`
  - `GET /api/v1/documentos/{uuid}/ride`
  - `GET /api/v1/documentos/{uuid}/ride/contrato`
  - `POST /api/v1/documentos/{uuid}/reprocesar`
  - `POST /api/v1/documentos/{uuid}/consultar-autorizacion`
  - `POST /api/v1/documentos/{uuid}/regenerar-ride`
  - `POST /api/v1/documentos/{uuid}/reenviar-correo`
- dashboard:
  - `GET /api/v1/dashboard/resumen`
  - `GET /api/v1/dashboard/documentos-por-tipo`
  - `GET /api/v1/dashboard/documentos-por-estado`
  - `GET /api/v1/dashboard/documentos-por-dia`
  - `GET /api/v1/dashboard/errores-por-etapa`
  - `GET /api/v1/dashboard/tiempos`
- catalogos:
  - tipos de documento
  - estados de documento
  - tipos de identificacion
  - formas de pago
  - impuestos
  - codigos de retencion
- administracion empresarial:
  - empresas
  - establecimientos
  - puntos de emision
  - secuenciales
  - certificados
  - configuracion SRI
  - configuracion correo
  - recursos graficos
  - plantillas RIDE
- seguridad y control:
  - usuarios
  - roles
  - permisos
  - auditoria
  - monitoreo operativo

### Frontend implementado

El frontend Angular ya dispone de una estructura administrativa real y no solo prototipos. Entre lo ya incorporado se encuentran:

- login con JWT.
- layout administrativo con navbar, sidebar y modulos.
- dashboard.
- bandeja/listado de documentos.
- detalle de documentos y operaciones principales.
- administracion de empresas, establecimientos, puntos de emision y secuenciales.
- certificados.
- configuracion SRI y correo.
- usuarios y roles.
- auditoria y monitoreo.
- recursos por empresa.
- modulo de plantillas RIDE con carga de `.jrxml`, verificacion, preview y consulta de contrato de campos.

## 3. Componentes clave ya resueltos

### 3.1 Independencia funcional del servicio

Se avanzo en la separacion para que `sri-files` funcione como aplicacion propia y no solo como dependencia operativa de otro sistema.

Ya existe:

- esquema de base de datos independiente.
- script de base completo en `backend/database/sri-files-full-schema.sql`.
- usuarios administrativos semilla.
- configuracion propia de certificados, SRI y correo.
- persistencia de archivos generados y metadata documental.

### 3.2 Facturacion electronica y fecha de emision

Se ajusto el flujo de generacion para que la fecha de emision del comprobante pueda alinearse con la fecha real de emision del documento electronico y no depender automaticamente de la fecha de pago o transferencia heredada del sistema origen.

### 3.3 Auditoria

Se implemento auditoria para acciones administrativas y operativas relevantes, incluyendo consultas consolidadas para documentos, usuarios, roles y empresas.

### 3.4 JasperReports y RIDE parametrizable

Se encuentra implementado un modulo administrativo para:

- cargar plantillas `.jrxml`;
- validarlas;
- previsualizarlas con documentos reales;
- marcar plantilla predeterminada por empresa y tipo;
- administrar recursos graficos como logos y marcas de agua.

Tambien existe la guia:

- [GUIA_JASPER_RIDE_SRI_FILES.md](C:/Users/Alexi/Documents/PROYECTOS_EPMAPA-T/EpmapaT_Apis/sri-files/MD/GUIA_JASPER_RIDE_SRI_FILES.md)

## 4. Estado frente a la documentacion del plan

### Cumplido o muy avanzado

- existe la nueva API `/api/v1`.
- existe autenticacion administrativa.
- existe recepcion y consulta de documentos.
- existen descargas de XML y RIDE.
- existen operaciones manuales de reproceso.
- existe dashboard.
- existen catalogos.
- existe administracion empresarial.
- existe control de certificados.
- existe configuracion SRI y correo.
- existen usuarios, roles y permisos.
- existe auditoria.
- existen recursos y plantillas RIDE.
- existe base de datos desacoplada y script integral.

### Parcial o con diferencias respecto a los `.md`

- el monitoreo en codigo usa actualmente `/api/v1/monitoreo`, mientras que algunos documentos definen `/api/v1/monitoring`.
- algunos contratos documentados aun no estan expuestos exactamente con el mismo path o shape esperado.
- el controlador legacy `SRI_Controller` sigue existiendo para compatibilidad y soporte de flujos heredados.
- no todos los puntos del plan de migracion documental y operativa estan formalizados en archivos auxiliares.

### Pendiente para declarar cumplimiento total

- alinear completamente el contrato publicado en `API_SRI_FILES_V1.md` con la implementacion final.
- revisar si faltan endpoints puntuales como:
  - `GET /api/v1/documentos/search`
  - `GET /api/v1/documentos/export`
  - endpoints globales de correos si se desean separados del monitoreo actual
- unificar nomenclatura final entre `monitoring` y `monitoreo`.
- seguir reduciendo dependencia funcional del controlador legacy.
- validar modulo por modulo del frontend contra `ARQUITECTURA_FRONTEND_SRI_FILES.md`.

## 5. Validacion tecnica reciente

### Backend

Validaciones recientes ejecutadas:

- `./mvnw.cmd -q -DskipTests compile`: exitoso.
- `./mvnw.cmd -q test`: exitoso.

### Frontend

Validacion reciente ejecutada:

- `npm run build`: exitoso.

Persisten advertencias menores de presupuesto de bundle y estilos, pero no bloquean la compilacion.

## 6. Riesgos y deuda tecnica vigente

### Riesgo alto

- coexistencia de backend nuevo `/api/v1` con componentes legacy bajo `/api/singsend`, lo que puede generar duplicidad funcional si no se controla el cierre gradual.

### Riesgo medio

- diferencias entre la documentacion funcional y los paths implementados en algunos modulos.
- necesidad de cerrar el inventario final de endpoints y pantallas para declarar cumplimiento del 100%.

### Riesgo bajo

- ajustes visuales y de consistencia UX todavia pueden requerir refinamiento en algunos modulos del frontend.
- algunos `.md` historicos ya no representan fielmente el estado actual del codigo.

## 7. Recomendaciones inmediatas

1. Actualizar `API_SRI_FILES_V1.md` cuando se cierre el contrato final real.
2. Mantener este archivo como referencia de avance real del proyecto.
3. Completar el checklist modulo por modulo usando:
   - arquitectura backend
   - arquitectura frontend
   - modelo de base de datos
   - plan de implementacion backend
   - plan de implementacion frontend
4. Definir si la ruta oficial de monitoreo sera `monitoring` o `monitoreo` y estandarizar backend, frontend y documentacion.
5. Planificar el retiro gradual de funciones legacy que ya tengan reemplazo estable en `/api/v1`.

## 8. Conclusion

`sri-files` ya no esta en una fase inicial de reestructuracion. El proyecto cuenta con una base operativa solida tanto en backend como en frontend administrativo, incluyendo seguridad, configuracion por empresa, control documental, auditoria y personalizacion del RIDE.

Lo que falta para hablar de cumplimiento del `100%` ya no es construir la base principal, sino cerrar diferencias entre documentacion y codigo, completar endpoints puntuales faltantes y consolidar el reemplazo definitivo de los componentes legacy.
