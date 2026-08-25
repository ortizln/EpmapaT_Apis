# API SRI-FILES V1

**Proyecto:** Plataforma de Administración de Documentos Electrónicos SRI  
**Versión API:** v1  
**Base Path:** `/api/v1`  
**Formato:** JSON / UTF-8  
**Autenticación:** JWT Bearer / clientes API autorizados  
**Backend:** Spring Boot  
**Fecha:** 2026-08-14

---

# 0. Estado de implementacion actual

Este documento define la API objetivo V1 y tambien sirve como referencia del backend actualmente disponible.

Al 2026-08-24:

- la mayor parte de `/api/v1` ya se encuentra implementada;
- el modulo de monitoreo opera con la ruta `/api/v1/monitoreo`;
- el seguimiento de correo por documento opera con `GET /api/v1/documentos/{uuid}/correo`;
- la consulta global de correos pendientes opera con `GET /api/v1/monitoreo/correos`;
- la busqueda rapida y la exportacion siguen siendo puntos de alineacion pendientes si se desea publicar exactamente:
  - `GET /api/v1/documentos/search`
  - `GET /api/v1/documentos/export`

Mientras exista una diferencia entre este archivo y el backend real, prevalece la implementacion efectiva del servicio.

---

# 1. Objetivo

Definir la API REST v1 de `sri-files` para que sistemas externos, el frontend administrativo y operadores autorizados puedan:

- registrar documentos electrónicos;
- consultar su procesamiento;
- descargar XML y RIDE;
- consultar historial;
- reprocesar etapas fallidas;
- reenviar correo;
- consultar estadísticas;
- administrar configuración;
- administrar usuarios y permisos;
- consultar auditoría.

La API deberá mantener desacoplados a los consumidores de:

```text
SOAP SRI
XAdES
XSD
JasperReports
certificados
rutas físicas
workers
schedulers
reintentos internos
```

---

# 2. URL base

Producción:

```text
https://<host>/sri-files/api/v1
```

Desarrollo:

```text
http://localhost:9090/api/v1
```

La URL real deberá configurarse por ambiente.

---

# 3. Convenciones

## Content-Type

```http
Content-Type: application/json
```

## Fechas

```text
YYYY-MM-DD
```

## Fecha/hora

ISO 8601:

```text
2026-08-14T13:30:00-05:00
```

## UUID

Los identificadores públicos serán UUID.

No exponer IDs `BIGINT` internos.

---

# 4. Autenticación

Para frontend administrativo:

```http
Authorization: Bearer <JWT>
```

Para integraciones ERP se podrá utilizar inicialmente JWT y posteriormente clientes API dedicados.

---

# 5. Headers comunes

```http
Authorization: Bearer <token>
Content-Type: application/json
X-Request-Id: <uuid-opcional>
Idempotency-Key: <valor-opcional>
```

`X-Request-Id` permitirá correlación.

Si no se recibe, el backend generará uno.

---

# 6. Respuesta de error estándar

```json
{
  "timestamp": "2026-08-14T13:30:00-05:00",
  "status": 400,
  "code": "DOC_VALIDATION_ERROR",
  "message": "El documento contiene errores de validación",
  "requestId": "88fddf37-25a2-4fd8-a92f-ae52b51fba32",
  "details": [
    {
      "field": "receptor.identificacion",
      "code": "REQUIRED",
      "message": "La identificación es obligatoria"
    }
  ]
}
```

---

# 7. Códigos HTTP

```text
200 OK
201 Created
202 Accepted
204 No Content

400 Bad Request
401 Unauthorized
403 Forbidden
404 Not Found
409 Conflict
422 Unprocessable Entity
429 Too Many Requests

500 Internal Server Error
502 Bad Gateway
503 Service Unavailable
```

---

# 8. MÓDULO AUTENTICACIÓN

## Login

```http
POST /api/v1/auth/login
```

Request:

```json
{
  "username": "admin",
  "password": "********"
}
```

Response:

```json
{
  "accessToken": "<jwt>",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "usuario": {
    "id": "uuid",
    "username": "admin",
    "nombre": "Administrador",
    "roles": [
      "ADMIN"
    ],
    "permisos": [
      "DOCUMENTO_VER",
      "DOCUMENTO_DESCARGAR"
    ]
  }
}
```

---

# 9. Usuario autenticado

```http
GET /api/v1/auth/me
```

Response:

```json
{
  "id": "uuid",
  "username": "admin",
  "nombre": "Administrador",
  "email": "admin@example.com",
  "roles": [
    "ADMIN"
  ],
  "permisos": []
}
```

---

# 10. MÓDULO DOCUMENTOS

Endpoint principal:

```http
POST /api/v1/documentos
```

Acepta:

```text
FACTURA
LIQUIDACION_COMPRA
NOTA_CREDITO
NOTA_DEBITO
RETENCION
GUIA_REMISION
```

El request completo de cada documento se define en:

```text
CONTRATOS_JSON_SRI_FILES.md
```

---

# 11. Registrar documento

```http
POST /api/v1/documentos
```

Ejemplo resumido:

```json
{
  "version": "1.0",
  "tipoDocumento": "FACTURA",
  "externalId": "ERP-FACTURA-348396",
  "emisor": {
    "ruc": "0460028810001",
    "establecimiento": "002",
    "puntoEmision": "018"
  },
  "secuencial": {
    "modo": "EXTERNO",
    "valor": "000348396"
  },
  "documento": {
    "fechaEmision": "2026-08-14"
  },
  "receptor": {},
  "detalles": [],
  "correo": {
    "enviar": true
  }
}
```

Response:

```http
202 Accepted
```

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "externalId": "ERP-FACTURA-348396",
  "tipoDocumento": "FACTURA",
  "estado": "RECIBIDO",
  "fechaRecepcion": "2026-08-14T13:30:00-05:00",
  "requestId": "88fddf37-25a2-4fd8-a92f-ae52b51fba32",
  "mensaje": "Documento recibido para procesamiento"
}
```

---

# 12. Idempotencia

Puede utilizarse:

```http
Idempotency-Key: ERP-FACTURA-348396
```

y siempre:

```json
"externalId": "ERP-FACTURA-348396"
```

Si el documento ya existe:

```http
200 OK
```

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "externalId": "ERP-FACTURA-348396",
  "tipoDocumento": "FACTURA",
  "estado": "AUTORIZADO",
  "duplicado": true,
  "mensaje": "El documento ya había sido registrado"
}
```

---

# 13. Listar documentos

```http
GET /api/v1/documentos
```

Filtros:

```text
empresaUuid
tipoDocumento
estado
busqueda
page
size
```

Ejemplo:

```text
GET /api/v1/documentos?tipoDocumento=FACTURA&estado=AUTORIZADO&busqueda=1790100634001&page=0&size=20
```

---

# 14. Paginación

Response:

```json
{
  "content": [
    {
      "id": "uuid",
      "tipoDocumento": "FACTURA",
      "numeroDocumento": "002-018-000348396",
      "fechaEmision": "2026-07-30",
      "identificacionReceptor": "1790100634001",
      "razonSocialReceptor": "CLIENTE",
      "total": 338.22,
      "estado": "AUTORIZADO"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1254,
  "totalPages": 63,
  "first": true,
  "last": false
}
```

---

# 15. Ordenamiento

Ejemplo:

```text
sort=fechaRecepcion,desc
```

Campos permitidos:

```text
fechaRecepcion
fechaEmision
numeroDocumento
razonSocialReceptor
total
estadoActual
```

No aceptar nombres arbitrarios de columnas SQL.

---

# 16. Consultar documento

```http
GET /api/v1/documentos/{uuid}
```

Response:

```json
{
  "id": "uuid",
  "externalId": "ERP-FACTURA-348396",
  "tipoDocumento": "FACTURA",
  "estado": "AUTORIZADO",

  "emisor": {
    "ruc": "0460028810001",
    "establecimiento": "002",
    "puntoEmision": "018"
  },

  "numeroDocumento": "002-018-000348396",

  "claveAcceso": "3007202601046002881000120020180003483964326898119",

  "fechaEmision": "2026-07-30",

  "receptor": {
    "identificacion": "1790100634001",
    "razonSocial": "ASOCIACION FE Y ALEGRIA ECUADOR",
    "email": "cliente@example.com"
  },

  "totales": {
    "subtotal": 338.01,
    "descuento": 0.00,
    "impuestos": 0.21,
    "total": 338.22
  },

  "autorizacion": {
    "numero": "3007202601046002881000120020180003483964326898119",
    "fecha": "2026-07-30T11:20:30-05:00"
  },

  "archivos": {
    "xmlGenerado": true,
    "xmlFirmado": true,
    "xmlAutorizado": true,
    "ride": true
  },

  "correo": {
    "estado": "ENVIADO"
  }
}
```

---

# 17. Estado del documento

```http
GET /api/v1/documentos/{uuid}/estado
```

Response:

```json
{
  "id": "uuid",
  "estado": "PENDIENTE_AUTORIZACION",
  "etapa": "AUTORIZACION",
  "ultimoCambio": "2026-08-14T13:31:00-05:00",
  "requiereIntervencion": false
}
```

---

# 18. Historial

```http
GET /api/v1/documentos/{uuid}/historial
```

Response:

```json
[
  {
    "estadoAnterior": null,
    "estadoNuevo": "RECIBIDO",
    "descripcion": "Documento recibido",
    "fecha": "2026-08-14T13:30:00-05:00"
  },
  {
    "estadoAnterior": "RECIBIDO",
    "estadoNuevo": "VALIDANDO",
    "descripcion": "Inicio de validación",
    "fecha": "2026-08-14T13:30:01-05:00"
  }
]
```

---

# 19. Errores del documento

```http
GET /api/v1/documentos/{uuid}/errores
```

Response:

```json
[
  {
    "id": 10,
    "etapa": "FIRMA",
    "codigo": "CERTIFICATE_EXPIRED",
    "mensaje": "El certificado digital está expirado",
    "recuperable": false,
    "resuelto": false,
    "fecha": "2026-08-14T13:30:03-05:00"
  }
]
```

---

# 20. Intentos SRI

```http
GET /api/v1/documentos/{uuid}/intentos-sri
```

Response:

```json
[
  {
    "operacion": "RECEPCION",
    "numeroIntento": 1,
    "resultado": "RECIBIDA",
    "duracionMs": 430,
    "fecha": "2026-08-14T13:30:05-05:00"
  }
]
```

---

# 21. MÓDULO ARCHIVOS

## Listar archivos

```http
GET /api/v1/documentos/{uuid}/archivos
```

Response:

```json
[
  {
    "tipo": "XML_GENERADO",
    "nombre": "generado.xml",
    "mimeType": "application/xml",
    "tamanio": 10500,
    "fechaCreacion": "2026-08-14T13:30:02-05:00"
  },
  {
    "tipo": "RIDE_PDF",
    "nombre": "ride.pdf",
    "mimeType": "application/pdf",
    "tamanio": 98500,
    "fechaCreacion": "2026-08-14T13:30:10-05:00"
  }
]
```

---

# 22. Descargar XML generado

```http
GET /api/v1/documentos/{uuid}/xml
```

Response:

```text
Content-Type: application/xml
Content-Disposition: attachment
```

Permiso:

```text
DOCUMENTO_DESCARGAR
```

---

# 23. Descargar XML firmado

```http
GET /api/v1/documentos/{uuid}/xml-firmado
```

---

# 24. Descargar XML autorizado

```http
GET /api/v1/documentos/{uuid}/xml-autorizado
```

Este será normalmente el XML que se entrega al cliente.

---

# 25. Descargar RIDE

```http
GET /api/v1/documentos/{uuid}/ride
```

Response:

```text
Content-Type: application/pdf
```

---

# 26. MÓDULO OPERACIONES

Las operaciones manuales deberán estar protegidas por permisos y auditadas.

---

# 27. Reprocesar

```http
POST /api/v1/documentos/{uuid}/reprocesar
```

Request opcional:

```json
{
  "motivo": "Reprocesamiento solicitado por operador"
}
```

El backend decidirá desde qué etapa continuar.

No reenviar automáticamente todo el ciclo.

---

# 28. Respuesta reprocesamiento

```http
202 Accepted
```

```json
{
  "id": "uuid",
  "estadoAnterior": "ERROR_RIDE",
  "estado": "AUTORIZADO",
  "accion": "REGENERAR_RIDE",
  "mensaje": "El documento fue programado para continuar su procesamiento"
}
```

---

# 29. Consultar autorización manualmente

```http
POST /api/v1/documentos/{uuid}/consultar-autorizacion
```

Permitido cuando el estado sea compatible.

---

# 30. Regenerar RIDE

```http
POST /api/v1/documentos/{uuid}/regenerar-ride
```

No vuelve a enviar el comprobante al SRI.

---

# 31. Reenviar correo

```http
POST /api/v1/documentos/{uuid}/reenviar-correo
```

Request:

```json
{
  "destinatarios": [
    "cliente@example.com"
  ],
  "copias": []
}
```

Si no se envían destinatarios, podrá utilizarse el correo original según la regla configurada.

---

# 32. No crear endpoint peligroso genérico

Evitar:

```text
POST /documentos/{uuid}/reenviar-todo
```

Las operaciones deben ser explícitas.

---

# 33. MÓDULO CORREO

## Historial de correos

```http
GET /api/v1/documentos/{uuid}/correo
```

Response:

```json
[
  {
    "destinatario": "cliente@example.com",
    "estado": "ENVIADO",
    "numeroIntentos": 1,
    "fechaEnvio": "2026-08-14T13:30:15-05:00"
  }
]
```

---

# 34. Correos pendientes

```http
GET /api/v1/monitoreo/correos
```

Observacion:

```text
La implementacion actual consolida la bandeja de correos pendientes dentro del modulo de monitoreo.
Si luego se requiere una bandeja administrativa dedicada, podra exponerse `/api/v1/correos`.
```

---

# 35. MÓDULO DASHBOARD

## Resumen

```http
GET /api/v1/dashboard/resumen
```

Parámetros:

```text
fechaDesde
fechaHasta
empresa
```

Response:

```json
{
  "total": 1254,
  "recibidos": 15,
  "procesando": 32,
  "autorizados": 1187,
  "noAutorizados": 8,
  "errores": 12,
  "correosPendientes": 5
}
```

---

# 36. Documentos por tipo

```http
GET /api/v1/dashboard/documentos-por-tipo
```

```json
[
  {
    "tipo": "FACTURA",
    "cantidad": 850
  },
  {
    "tipo": "RETENCION",
    "cantidad": 302
  }
]
```

---

# 37. Documentos por estado

```http
GET /api/v1/dashboard/documentos-por-estado
```

---

# 38. Documentos por día

```http
GET /api/v1/dashboard/documentos-por-dia
```

Response:

```json
[
  {
    "fecha": "2026-08-10",
    "cantidad": 120
  },
  {
    "fecha": "2026-08-11",
    "cantidad": 150
  }
]
```

---

# 39. Errores por etapa

```http
GET /api/v1/dashboard/errores-por-etapa
```

```json
[
  {
    "etapa": "FIRMA",
    "cantidad": 3
  },
  {
    "etapa": "CORREO",
    "cantidad": 5
  }
]
```

---

# 40. Tiempos de procesamiento

```http
GET /api/v1/dashboard/tiempos
```

Response:

```json
{
  "promedioProcesamientoMs": 3400,
  "promedioAutorizacionMs": 2100
}
```

---

# 41. MÓDULO EMPRESAS

## Listar

```http
GET /api/v1/empresas
```

## Crear

```http
POST /api/v1/empresas
```

Request:

```json
{
  "ruc": "0460028810001",
  "razonSocial": "EMPRESA PUBLICA MUNICIPAL",
  "nombreComercial": "EPMAPA-T",
  "direccionMatriz": "TULCAN",
  "obligadoContabilidad": true
}
```

---

# 42. Consultar empresa

```http
GET /api/v1/empresas/{uuid}
```

---

# 43. Actualizar empresa

```http
PUT /api/v1/empresas/{uuid}
```

No permitir modificar el RUC de una empresa que ya tiene documentos sin una política administrativa especial.

---

# 44. Desactivar empresa

```http
PATCH /api/v1/empresas/{uuid}/estado
```

```json
{
  "activo": false
}
```

No eliminar físicamente.

---

# 45. MÓDULO ESTABLECIMIENTOS

```text
GET  /api/v1/empresas/{empresaId}/establecimientos

POST /api/v1/empresas/{empresaId}/establecimientos

GET  /api/v1/establecimientos/{uuid}

PUT  /api/v1/establecimientos/{uuid}

PATCH /api/v1/establecimientos/{uuid}/estado
```

---

# 46. MÓDULO PUNTOS DE EMISIÓN

```text
GET  /api/v1/establecimientos/{id}/puntos-emision

POST /api/v1/establecimientos/{id}/puntos-emision

GET  /api/v1/puntos-emision/{uuid}

PUT  /api/v1/puntos-emision/{uuid}

PATCH /api/v1/puntos-emision/{uuid}/estado
```

---

# 47. Secuenciales

```http
GET /api/v1/puntos-emision/{uuid}/secuenciales
```

Response:

```json
[
  {
    "tipoDocumento": "FACTURA",
    "valorActual": 348396,
    "activo": true
  }
]
```

---

# 48. Configurar secuencial

```http
PUT /api/v1/puntos-emision/{uuid}/secuenciales/{tipoDocumento}
```

Request:

```json
{
  "valorActual": 348396,
  "activo": true
}
```

Permiso crítico:

```text
SECUENCIAL_ADMINISTRAR
```

Toda modificación deberá auditarse.

---

# 49. MÓDULO CERTIFICADOS

## Listar certificados

```http
GET /api/v1/empresas/{empresaId}/certificados
```

No devolver contraseña.

---

# 50. Cargar certificado

```http
POST /api/v1/empresas/{empresaId}/certificados
Content-Type: multipart/form-data
```

Campos:

```text
file
nombre
```

La contraseña deberá gestionarse por mecanismo seguro definido en infraestructura.

---

# 51. Verificar certificado

```http
POST /api/v1/certificados/{uuid}/verificar
```

Response:

```json
{
  "valido": true,
  "fechaEmision": "2025-01-01T00:00:00-05:00",
  "fechaExpiracion": "2027-01-01T00:00:00-05:00",
  "diasRestantes": 140
}
```

---

# 52. Activar/desactivar certificado

```http
PATCH /api/v1/certificados/{uuid}/estado
```

---

# 53. MÓDULO CONFIGURACIÓN SRI

```http
GET /api/v1/empresas/{empresaId}/configuracion-sri
```

Response administrativo:

```json
{
  "ambiente": 2,
  "timeoutConexionMs": 10000,
  "timeoutRespuestaMs": 30000,
  "maxReintentos": 5,
  "activo": true
}
```

No es necesario exponer URLs internas a usuarios sin permiso técnico.

---

# 54. Actualizar configuración SRI

```http
PUT /api/v1/empresas/{empresaId}/configuracion-sri
```

Permiso:

```text
CONFIGURACION_EDITAR
```

---

# 55. MÓDULO CONFIGURACIÓN DE CORREO

```text
GET /api/v1/empresas/{empresaId}/configuracion-correo

PUT /api/v1/empresas/{empresaId}/configuracion-correo
```

Request:

```json
{
  "remitente": "facturacion@example.com",
  "nombreRemitente": "Facturación Electrónica",
  "enviarXml": true,
  "enviarRide": true,
  "plantillaAsunto": "Comprobante electrónico {numeroDocumento}"
}
```

---

# 56. MÓDULO RECURSOS / LOGOS

```text
GET  /api/v1/empresas/{empresaId}/recursos

POST /api/v1/empresas/{empresaId}/recursos

DELETE/PATCH según política
```

Tipos:

```text
LOGO_PRINCIPAL
LOGO_SECUNDARIO
MARCA_AGUA
```

Preferir desactivación frente a borrado cuando el recurso esté relacionado con configuración histórica.

---

# 57. MÓDULO PLANTILLAS RIDE

```text
GET /api/v1/empresas/{empresaId}/plantillas-ride

POST /api/v1/empresas/{empresaId}/plantillas-ride

PUT /api/v1/plantillas-ride/{uuid}

PATCH /api/v1/plantillas-ride/{uuid}/estado
```

Campos:

```text
tipoDocumento
nombre
versión
predeterminada
activa
```

---

# 58. MÓDULO USUARIOS

## Listar

```http
GET /api/v1/usuarios
```

Filtros:

```text
username
nombre
email
activo
rol
page
size
```

---

# 59. Crear usuario

```http
POST /api/v1/usuarios
```

```json
{
  "username": "operador1",
  "nombre": "Operador Uno",
  "email": "operador@example.com",
  "password": "********",
  "roles": [
    "OPERADOR"
  ]
}
```

La contraseña nunca se devolverá.

---

# 60. Actualizar usuario

```http
PUT /api/v1/usuarios/{uuid}
```

---

# 61. Estado usuario

```http
PATCH /api/v1/usuarios/{uuid}/estado
```

```json
{
  "activo": false
}
```

---

# 62. Cambiar contraseña

```http
POST /api/v1/usuarios/{uuid}/cambiar-password
```

---

# 63. MÓDULO ROLES

```text
GET  /api/v1/roles
POST /api/v1/roles
GET  /api/v1/roles/{id}
PUT  /api/v1/roles/{id}
```

---

# 64. Asignar permisos

```http
PUT /api/v1/roles/{id}/permisos
```

```json
{
  "permisos": [
    "DOCUMENTO_VER",
    "DOCUMENTO_DESCARGAR",
    "DOCUMENTO_REPROCESAR"
  ]
}
```

---

# 65. Catálogo de permisos

```http
GET /api/v1/permisos
```

Agrupar por módulo.

---

# 66. MÓDULO AUDITORÍA

```http
GET /api/v1/auditoria
```

Filtros:

```text
usuario
accion
entidad
fechaDesde
fechaHasta
resultado
page
size
```

---

# 67. Detalle auditoría

```http
GET /api/v1/auditoria/{id}
```

Solo usuarios autorizados.

---

# 68. MÓDULO CATÁLOGOS

```text
GET /api/v1/catalogos/tipos-documento

GET /api/v1/catalogos/estados-documento

GET /api/v1/catalogos/tipos-identificacion

GET /api/v1/catalogos/formas-pago

GET /api/v1/catalogos/impuestos

GET /api/v1/catalogos/codigos-retencion
```

Estos endpoints facilitan formularios y validaciones frontend.

---

# 69. MÓDULO MONITOREO

## Estado general

```http
GET /api/v1/monitoreo/health
```

Response:

```json
{
  "database": "UP",
  "storage": "UP",
  "email": "UP",
  "certificate": "VALID",
  "sriRecepcion": "UP",
  "sriAutorizacion": "UP",
  "timestamp": "2026-08-14T13:30:00-05:00"
}
```

El estado SRI puede representar la última comprobación conocida para evitar llamadas excesivas.

---

# 70. Procesos pendientes

```http
GET /api/v1/monitoreo/pendientes
```

```json
{
  "recibidos": 10,
  "pendientesAutorizacion": 5,
  "correosPendientes": 2,
  "requiereIntervencion": 1
}
```

---

## Resumen de monitoreo

```http
GET /api/v1/monitoreo/resumen
```

La implementacion actual tambien expone:

```http
GET /api/v1/monitoreo/correos
```

para revisar correos pendientes dentro del mismo modulo.

---

# 71. Actuator

Administración técnica:

```text
/actuator/health
```

No exponer públicamente endpoints Actuator innecesarios.

---

# 72. Códigos de error — documento

```text
DOC_VALIDATION_ERROR

DOC_NOT_FOUND

DOC_DUPLICATE

DOC_INVALID_STATE

DOC_TOTAL_MISMATCH

DOC_PROCESSING_ERROR

DOC_REQUIRES_INTERVENTION
```

---

# 73. Códigos — empresa/configuración

```text
COMPANY_NOT_FOUND

ESTABLISHMENT_NOT_FOUND

ISSUING_POINT_NOT_FOUND

ISSUING_POINT_DISABLED

SEQUENCE_NOT_CONFIGURED

SEQUENCE_CONFLICT
```

---

# 74. Códigos — XML

```text
XML_GENERATION_ERROR

XSD_VALIDATION_ERROR

XML_STORAGE_ERROR
```

---

# 75. Códigos — firma

```text
CERTIFICATE_NOT_FOUND

CERTIFICATE_EXPIRED

CERTIFICATE_INVALID

CERTIFICATE_PASSWORD_ERROR

SIGNATURE_ERROR
```

---

# 76. Códigos — SRI

```text
SRI_RECEPTION_TIMEOUT

SRI_RECEPTION_ERROR

SRI_RETURNED

SRI_AUTHORIZATION_PENDING

SRI_AUTHORIZATION_ERROR

SRI_NOT_AUTHORIZED

SRI_SERVICE_UNAVAILABLE
```

---

# 77. Códigos — RIDE

```text
RIDE_TEMPLATE_NOT_FOUND

RIDE_GENERATION_ERROR
```

---

# 78. Códigos — correo

```text
EMAIL_NOT_CONFIGURED

EMAIL_INVALID_RECIPIENT

EMAIL_SEND_ERROR
```

---

# 79. Códigos — seguridad

```text
AUTH_INVALID_CREDENTIALS

AUTH_TOKEN_EXPIRED

AUTH_TOKEN_INVALID

ACCESS_DENIED
```

---

# 80. Validación de estados en operaciones

Ejemplo:

Si se solicita:

```text
regenerar RIDE
```

sobre:

```text
RECIBIDO
```

responder:

```http
409 Conflict
```

```json
{
  "code": "DOC_INVALID_STATE",
  "message": "No es posible generar el RIDE porque el documento todavía no está autorizado"
}
```

---

# 81. Rate limiting

Para endpoints públicos de integración considerar límites por cliente.

Especialmente:

```text
POST /documentos
login
operaciones manuales
```

Respuesta:

```http
429 Too Many Requests
```

No es obligatorio en la primera iteración interna, pero la arquitectura debe contemplarlo.

---

# 82. Búsqueda rápida

Para el frontend:

```http
GET /api/v1/documentos/search?q=...
```

Puede buscar por:

```text
clave acceso
número documento
identificación
razón social
externalId
```

Limitar resultados.

---

# 83. Exportación

Para administración:

```http
GET /api/v1/documentos/export
```

Filtros iguales a la bandeja.

Formatos iniciales:

```text
CSV
```

Opcionalmente XLSX en una etapa posterior.

La exportación no deberá incluir JSON/XML completos por defecto.

---

# 84. Operaciones masivas

No habilitar inicialmente:

```text
reprocesar 10.000 documentos
```

sin controles.

Una futura API podrá ser:

```text
POST /api/v1/documentos/bulk/reprocesar
```

con límites, permisos y confirmación.

---

# 85. Webhooks — evolución futura

Para evitar polling desde los ERP, posteriormente podrá incorporarse:

```text
webhook
```

Ejemplo:

```json
{
  "event": "DOCUMENTO_AUTORIZADO",
  "documentId": "uuid",
  "externalId": "ERP-FACTURA-348396",
  "estado": "AUTORIZADO"
}
```

No es requisito inicial de V1.

---

# 86. OpenAPI

Swagger deberá estar disponible según configuración en:

```text
/swagger-ui.html
/v3/api-docs
```

Agrupar endpoints por tags:

```text
Auth

Documentos

Archivos

Operaciones

Correo

Dashboard

Empresas

Establecimientos

Puntos de emisión

Certificados

Configuración

Usuarios

Roles

Auditoría

Catálogos

Monitoreo
```

---

# 87. Versionado

No modificar destructivamente `/api/v1`.

Cambios incompatibles deberán introducir:

```text
/api/v2
```

Los campos opcionales nuevos sí podrán añadirse a V1 cuando no rompan consumidores.

---

# 88. Deprecación

Cuando se retire un endpoint:

```text
Deprecated: true
Sunset: <fecha>
```

y documentarlo en OpenAPI.

---

# 89. API legacy

Los endpoints existentes bajo:

```text
/api/singsend
```

permanecerán temporalmente.

No agregar nuevas funciones importantes ahí.

El objetivo será migrar consumidores hacia:

```text
/api/v1
```

---

# 90. Permisos mínimos sugeridos

```text
DOCUMENTO_VER
DOCUMENTO_DESCARGAR
DOCUMENTO_REPROCESAR
DOCUMENTO_CONSULTAR_SRI
DOCUMENTO_REGENERAR_RIDE
DOCUMENTO_REENVIAR_CORREO

DASHBOARD_VER

EMPRESA_VER
EMPRESA_ADMINISTRAR

SECUENCIAL_VER
SECUENCIAL_ADMINISTRAR

CERTIFICADO_VER
CERTIFICADO_ADMINISTRAR

CONFIGURACION_VER
CONFIGURACION_EDITAR

USUARIO_VER
USUARIO_ADMINISTRAR

ROL_VER
ROL_ADMINISTRAR

AUDITORIA_VER

MONITOREO_VER
```

---

# 91. Matriz resumida de roles

## SUPER_ADMIN

Acceso completo.

## ADMIN

Administración funcional excepto restricciones reservadas.

## OPERADOR

```text
ver documentos
descargar
consultar SRI
reprocesar según permiso
reenviar correo
```

## CONSULTA

```text
dashboard
consulta
descarga según política
```

## AUDITOR

```text
consulta documental
historial
auditoría
sin modificación
```

---

# 92. Respuestas sensibles

Nunca devolver:

```text
passwordHash
password certificado
JWT secret
credenciales BD
rutas internas sensibles
stack trace
configuración secreta
```

El stack trace quedará en logs técnicos.

---

# 93. Tamaño máximo

Configurar límites para:

```text
JSON
certificados
logos
plantillas
```

Rechazar payloads anormalmente grandes.

---

# 94. CORS

Configurar explícitamente dominios autorizados para frontend.

No utilizar en producción:

```text
*
```

con credenciales.

---

# 95. Flujo completo ERP

```text
ERP
 │
 │ POST /documentos
 ▼
SRI-FILES
 │
 │ 202
 ▼
ERP recibe UUID
 │
 │
 ├── GET /documentos/{uuid}/estado
 │
 │             o
 │
 └── futuro webhook
```

El ERP no espera durante firma/SRI/autorización.

---

# 96. Flujo frontend

```text
LOGIN
  ↓
DASHBOARD
  ↓
BANDEJA
  ↓
DETALLE DOCUMENTO
  │
  ├── RESUMEN
  ├── JSON
  ├── XML
  ├── RIDE
  ├── SRI
  ├── HISTORIAL
  ├── CORREO
  └── AUDITORÍA
```

---

# 97. Criterios de aceptación API V1

- [ ] Existe `/api/v1`.
- [ ] Existe autenticación.
- [ ] Existe endpoint único para recepción.
- [ ] Los seis contratos son aceptables por el endpoint.
- [ ] Existe idempotencia.
- [ ] La recepción responde 202.
- [ ] Existe consulta paginada.
- [ ] Existen filtros.
- [ ] Existe detalle.
- [ ] Existe estado.
- [ ] Existe historial.
- [ ] Existen errores.
- [ ] Existen intentos SRI.
- [ ] Se puede descargar XML generado.
- [ ] Se puede descargar XML firmado.
- [ ] Se puede descargar XML autorizado.
- [ ] Se puede descargar RIDE.
- [ ] Existe reprocesamiento seguro.
- [ ] Existe consulta manual SRI.
- [ ] Existe regeneración RIDE.
- [ ] Existe reenvío correo.
- [ ] Existe dashboard.
- [ ] Existe administración de empresa.
- [ ] Existe administración de establecimientos.
- [ ] Existe administración de puntos de emisión.
- [ ] Existe administración de secuenciales.
- [ ] Existe administración de certificados.
- [ ] Existe configuración SRI.
- [ ] Existe configuración de correo.
- [ ] Existen usuarios.
- [ ] Existen roles/permisos.
- [ ] Existe auditoría.
- [ ] Existen catálogos.
- [ ] Existe monitoreo.
- [ ] Los errores tienen formato uniforme.
- [ ] OpenAPI documenta todos los endpoints.
- [ ] Los endpoints legacy pueden coexistir durante migración.

---

# 98. Prioridad de implementación

## PRIORIDAD 1

```text
POST /documentos
GET /documentos
GET /documentos/{uuid}
GET /documentos/{uuid}/estado
GET /documentos/{uuid}/historial
```

## PRIORIDAD 2

```text
XML
RIDE
errores
intentos SRI
reprocesar
consultar autorización
reenviar correo
```

## PRIORIDAD 3

```text
dashboard
empresa
establecimiento
punto emisión
secuenciales
certificados
```

## PRIORIDAD 4

```text
usuarios
roles
auditoría
monitoring
exportación
```

---

# 99. Próximo documento recomendado

Con:

```text
PLANIFICACION_REESTRUCTURACION_SRI_FILES.md

MODELO_BASE_DATOS_SRI_FILES.md

CONTRATOS_JSON_SRI_FILES.md

ARQUITECTURA_BACKEND_SRI_FILES.md

API_SRI_FILES_V1.md
```

ya existe la especificación principal del backend.

El siguiente documento recomendado es:

```text
PLAN_IMPLEMENTACION_BACKEND_SRI_FILES.md
```

Su objetivo será convertir toda esta arquitectura en tareas ejecutables:

```text
FASE
TAREA
ARCHIVOS A CREAR
ARCHIVOS A MODIFICAR
DEPENDENCIAS
PRUEBAS
CRITERIO DE ACEPTACIÓN
ORDEN DE EJECUCIÓN
```

De esta forma podrá entregarse el plan directamente a un agente de programación y ejecutar la migración paso a paso sin modificar indiscriminadamente el servicio productivo.

---

# 100. Anexo de alineacion con implementacion real

Estado revisado al 2026-08-24 contra el backend actualmente disponible.

## Rutas implementadas con diferencia frente al contrato historico

- monitoreo:
  - documentado historicamente: `/api/v1/monitoring/*`
  - implementado actualmente: `/api/v1/monitoreo/*`
- seguimiento de correo por documento:
  - documentado historicamente: `GET /api/v1/documentos/{uuid}/correos`
  - implementado actualmente: `GET /api/v1/documentos/{uuid}/correo`
- bandeja de correos pendientes:
  - documentado historicamente: `GET /api/v1/correos`
  - implementado actualmente: `GET /api/v1/monitoreo/correos`

## Endpoints aun pendientes de alineacion exacta

- `GET /api/v1/documentos/search?q=...`
- `GET /api/v1/documentos/export`
- `POST /api/v1/documentos/bulk/reprocesar`

Actualmente la busqueda operativa se atiende desde:

```http
GET /api/v1/documentos?empresaUuid=...&tipoDocumento=...&estado=...&busqueda=...&page=0&size=10
```

## Criterio de lectura recomendado

Mientras no se cierre la normalizacion final del contrato, deben considerarse como fuente principal:

1. el backend real en `backend/src/main/java/com/erp/sri_files/controller/`
2. este anexo de alineacion
3. el resto del documento como contrato objetivo
