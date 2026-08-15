# CONTRATOS JSON — SRI-FILES

**Proyecto:** Plataforma de Administración de Documentos Electrónicos SRI  
**API objetivo:** `/api/v1/documentos`  
**Formato:** JSON / UTF-8  
**Fecha:** 2026-08-14  
**Versión del contrato:** 1.0

---

# 1. Objetivo

Definir un contrato JSON estable entre los sistemas consumidores y `sri-files`.

El sistema consumidor deberá entregar únicamente la información funcional del comprobante. `sri-files` será responsable de:

```text
JSON
 ↓
validación
 ↓
generación de clave de acceso
 ↓
generación XML
 ↓
validación XSD
 ↓
firma electrónica
 ↓
envío al SRI
 ↓
consulta de autorización
 ↓
almacenamiento XML autorizado
 ↓
generación RIDE/PDF
 ↓
envío por correo
```

El consumidor no deberá construir XML ni conocer detalles de SOAP, XAdES, JasperReports o endpoints internos del SRI.

---

# 2. Alcance

Contratos para:

```text
01 FACTURA
03 LIQUIDACION_COMPRA
04 NOTA_CREDITO
05 NOTA_DEBITO
06 GUIA_REMISION
07 RETENCION
```

La estructura JSON es un contrato interno de `sri-files`. El XML final deberá generarse conforme al esquema XSD y versión configurada para cada comprobante.

---

# 3. Principio fundamental

El JSON NO será una copia literal del XML.

Debe ser:

- claro;
- legible;
- desacoplado de JAXB;
- versionable;
- fácil de consumir;
- validable;
- suficientemente completo para producir el XML SRI.

`sri-files` será responsable de transformar:

```text
Contrato JSON interno
        ↓
Modelo de dominio
        ↓
Modelo XML correspondiente
        ↓
XML SRI
```

---

# 4. Endpoint común

```http
POST /api/v1/documentos
Content-Type: application/json
Authorization: Bearer <token>
Idempotency-Key: <opcional>
```

Todos los comprobantes ingresarán por este endpoint.

El campo:

```json
"tipoDocumento": "FACTURA"
```

determinará el procesador correspondiente.

---

# 5. Envelope común

```json
{
  "version": "1.0",
  "tipoDocumento": "FACTURA",
  "externalId": "ERP-FACTURA-348396",

  "emisor": {},
  "documento": {},
  "receptor": {},

  "detalles": [],

  "impuestos": [],

  "informacionAdicional": [],

  "correo": {}
}
```

No todos los bloques aplican a todos los documentos.

---

# 6. Campos administrativos comunes

## version

```json
"version": "1.0"
```

Versión del contrato JSON, no versión del XML SRI.

Obligatorio.

---

## tipoDocumento

Valores:

```text
FACTURA
LIQUIDACION_COMPRA
NOTA_CREDITO
NOTA_DEBITO
GUIA_REMISION
RETENCION
```

Obligatorio.

---

## externalId

```json
"externalId": "ERP-FACTURA-348396"
```

Identificador único generado por el sistema origen.

Obligatorio.

Se utilizará para idempotencia.

No deberá reutilizarse para documentos diferentes dentro de la misma empresa.

---

# 7. Emisor

Ejemplo:

```json
"emisor": {
  "ruc": "0460028810001",
  "establecimiento": "002",
  "puntoEmision": "018"
}
```

## Campos

| Campo | Tipo | Obligatorio | Descripción |
|---|---|---:|---|
| ruc | string | Sí | RUC del emisor |
| establecimiento | string(3) | Sí | Código establecimiento |
| puntoEmision | string(3) | Sí | Punto de emisión |

La razón social, nombre comercial, dirección matriz, certificado, ambiente y otras configuraciones del emisor deberán recuperarse de la base de `sri-files`.

Esto evita enviar datos repetidos y potencialmente inconsistentes en cada petición.

---

# 8. Secuencial

Se soportarán dos modos.

## AUTO

```json
"secuencial": {
  "modo": "AUTO"
}
```

`sri-files` asigna el siguiente secuencial disponible.

## EXTERNO

```json
"secuencial": {
  "modo": "EXTERNO",
  "valor": "000348396"
}
```

El sistema origen proporciona el secuencial.

La configuración de la empresa determinará qué modalidad está permitida.

No se deberá permitir que AUTO y EXTERNO compitan sobre la misma serie sin una estrategia explícita.

---

# 9. Receptor

Estructura común:

```json
"receptor": {
  "tipoIdentificacion": "04",
  "identificacion": "1790100634001",
  "razonSocial": "CLIENTE DE EJEMPLO",
  "direccion": "QUITO",
  "email": "cliente@example.com"
}
```

Los campos exactos obligatorios dependerán del comprobante.

---

# 10. Información adicional

```json
"informacionAdicional": [
  {
    "nombre": "Dirección",
    "valor": "Tulcán - Carchi"
  },
  {
    "nombre": "Teléfono",
    "valor": "062XXXXXX"
  }
]
```

El generador XML deberá transformar estos valores al bloque de información adicional admitido por el esquema correspondiente.

---

# 11. Correo

```json
"correo": {
  "enviar": true,
  "destinatarios": [
    "cliente@example.com"
  ],
  "copias": [],
  "adjuntarXml": true,
  "adjuntarRide": true
}
```

## Reglas

El correo NO forma parte del XML tributario.

Es una instrucción administrativa para `sri-files`.

Si:

```json
"enviar": false
```

el documento podrá finalizar correctamente sin correo.

---

# 12. CONTRATO — FACTURA

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
    "fechaEmision": "2026-07-30",
    "moneda": "DOLAR",
    "propina": 0.00
  },

  "receptor": {
    "tipoIdentificacion": "04",
    "identificacion": "1790100634001",
    "razonSocial": "ASOCIACION FE Y ALEGRIA ECUADOR",
    "direccion": "ASUNCION OE2-38 Y MANUEL LARREA",
    "email": "cliente@example.com"
  },

  "detalles": [
    {
      "codigoPrincipal": "SERV-001",
      "codigoAuxiliar": null,
      "descripcion": "SERVICIO",
      "cantidad": 1.000000,
      "precioUnitario": 196.830000,
      "descuento": 0.000000,
      "precioTotalSinImpuesto": 196.830000,

      "impuestos": [
        {
          "codigo": "2",
          "codigoPorcentaje": "0",
          "tarifa": 0.00,
          "baseImponible": 196.83,
          "valor": 0.00
        }
      ]
    },
    {
      "codigoPrincipal": "SERV-002",
      "descripcion": "OTRO SERVICIO",
      "cantidad": 1.000000,
      "precioUnitario": 141.180000,
      "descuento": 0.000000,
      "precioTotalSinImpuesto": 141.180000,

      "impuestos": [
        {
          "codigo": "2",
          "codigoPorcentaje": "4",
          "tarifa": 0.15,
          "baseImponible": 141.18,
          "valor": 0.21
        }
      ]
    }
  ],

  "pagos": [
    {
      "formaPago": "20",
      "total": 338.22,
      "plazo": 0,
      "unidadTiempo": "dias"
    }
  ],

  "informacionAdicional": [
    {
      "nombre": "Email",
      "valor": "cliente@example.com"
    }
  ],

  "correo": {
    "enviar": true,
    "destinatarios": [
      "cliente@example.com"
    ],
    "adjuntarXml": true,
    "adjuntarRide": true
  }
}
```

---

# 13. Validaciones de factura

Validar como mínimo:

```text
fechaEmision
receptor
detalles no vacíos
cantidad > 0
precioUnitario >= 0
descuento >= 0
precioTotalSinImpuesto
impuestos
pagos cuando corresponda
```

Los totales deberán recalcularse en backend.

No confiar ciegamente en totales proporcionados por el ERP.

Calcular:

```text
subtotal
descuentos
bases imponibles
impuestos
importe total
```

y comparar con los valores recibidos cuando el contrato incluya valores de control.

---

# 14. Totales calculados

Por defecto el consumidor no necesita enviar:

```json
"totalSinImpuestos": 338.01,
"totalDescuento": 0.00,
"importeTotal": 338.22
```

porque pueden derivarse de los detalles.

`sri-files` los calculará antes de generar el XML.

Si posteriormente se decide recibirlos como control, deberán considerarse valores de comparación, no fuente absoluta de verdad.

---

# 15. CONTRATO — LIQUIDACIÓN DE COMPRA

```json
{
  "version": "1.0",
  "tipoDocumento": "LIQUIDACION_COMPRA",
  "externalId": "ERP-LIQ-000001",

  "emisor": {
    "ruc": "0460028810001",
    "establecimiento": "002",
    "puntoEmision": "018"
  },

  "secuencial": {
    "modo": "AUTO"
  },

  "documento": {
    "fechaEmision": "2026-08-14",
    "moneda": "DOLAR"
  },

  "receptor": {
    "tipoIdentificacion": "05",
    "identificacion": "0400000000",
    "razonSocial": "PROVEEDOR EJEMPLO",
    "direccion": "TULCAN"
  },

  "detalles": [
    {
      "codigoPrincipal": "COMP-001",
      "descripcion": "ADQUISICION DE BIEN",
      "cantidad": 10,
      "precioUnitario": 5.00,
      "descuento": 0.00,
      "precioTotalSinImpuesto": 50.00,

      "impuestos": [
        {
          "codigo": "2",
          "codigoPorcentaje": "4",
          "tarifa": 15.00,
          "baseImponible": 50.00,
          "valor": 7.50
        }
      ]
    }
  ],

  "pagos": [
    {
      "formaPago": "20",
      "total": 57.50
    }
  ],

  "informacionAdicional": [],

  "correo": {
    "enviar": false
  }
}
```

La transformación final deberá respetar el XSD configurado para liquidaciones.

---

# 16. CONTRATO — NOTA DE CRÉDITO

Las notas de crédito requieren referencia al comprobante modificado.

```json
{
  "version": "1.0",
  "tipoDocumento": "NOTA_CREDITO",
  "externalId": "ERP-NC-000001",

  "emisor": {
    "ruc": "0460028810001",
    "establecimiento": "002",
    "puntoEmision": "018"
  },

  "secuencial": {
    "modo": "AUTO"
  },

  "documento": {
    "fechaEmision": "2026-08-14",
    "moneda": "DOLAR",
    "motivo": "ANULACION DE VALORES FACTURADOS"
  },

  "documentoModificado": {
    "tipoDocumento": "01",
    "numeroDocumento": "002-018-000348396",
    "fechaEmision": "2026-07-30"
  },

  "receptor": {
    "tipoIdentificacion": "04",
    "identificacion": "1790100634001",
    "razonSocial": "ASOCIACION FE Y ALEGRIA ECUADOR"
  },

  "detalles": [
    {
      "codigoInterno": "SERV-001",
      "descripcion": "REVERSO DE SERVICIO",
      "cantidad": 1,
      "precioUnitario": 50.00,
      "descuento": 0.00,
      "precioTotalSinImpuesto": 50.00,

      "impuestos": [
        {
          "codigo": "2",
          "codigoPorcentaje": "4",
          "tarifa": 15.00,
          "baseImponible": 50.00,
          "valor": 7.50
        }
      ]
    }
  ],

  "informacionAdicional": [],

  "correo": {
    "enviar": true,
    "destinatarios": [
      "cliente@example.com"
    ],
    "adjuntarXml": true,
    "adjuntarRide": true
  }
}
```

---

# 17. Validaciones nota de crédito

Obligatorio:

```text
documentoModificado.tipoDocumento
documentoModificado.numeroDocumento
documentoModificado.fechaEmision
motivo
detalles
```

Cuando la factura original exista en `sri-files`, se recomienda validar:

```text
empresa
receptor
documento original autorizado
valores máximos permitidos
```

Cuando sea un documento histórico externo, permitir la referencia sin exigir que exista localmente, siempre que la política configurada lo permita.

---

# 18. CONTRATO — NOTA DE DÉBITO

```json
{
  "version": "1.0",
  "tipoDocumento": "NOTA_DEBITO",
  "externalId": "ERP-ND-000001",

  "emisor": {
    "ruc": "0460028810001",
    "establecimiento": "002",
    "puntoEmision": "018"
  },

  "secuencial": {
    "modo": "AUTO"
  },

  "documento": {
    "fechaEmision": "2026-08-14"
  },

  "documentoModificado": {
    "tipoDocumento": "01",
    "numeroDocumento": "002-018-000348396",
    "fechaEmision": "2026-07-30"
  },

  "receptor": {
    "tipoIdentificacion": "04",
    "identificacion": "1790100634001",
    "razonSocial": "CLIENTE EJEMPLO"
  },

  "motivos": [
    {
      "razon": "INTERESES",
      "valor": 10.00
    }
  ],

  "impuestos": [
    {
      "codigo": "2",
      "codigoPorcentaje": "4",
      "tarifa": 15.00,
      "baseImponible": 10.00,
      "valor": 1.50
    }
  ],

  "pagos": [
    {
      "formaPago": "20",
      "total": 11.50
    }
  ],

  "informacionAdicional": [],

  "correo": {
    "enviar": true,
    "destinatarios": [
      "cliente@example.com"
    ]
  }
}
```

---

# 19. Validaciones nota de débito

Validar:

```text
documento modificado
fecha
receptor
al menos un motivo
valor del motivo > 0
impuestos
```

---

# 20. CONTRATO — COMPROBANTE DE RETENCIÓN

La retención tendrá un contrato propio debido a su naturaleza tributaria.

```json
{
  "version": "1.0",
  "tipoDocumento": "RETENCION",
  "externalId": "ERP-RET-000001",

  "emisor": {
    "ruc": "0460028810001",
    "establecimiento": "002",
    "puntoEmision": "018"
  },

  "secuencial": {
    "modo": "AUTO"
  },

  "documento": {
    "fechaEmision": "2026-08-14",
    "periodoFiscal": "08/2026"
  },

  "receptor": {
    "tipoIdentificacion": "04",
    "identificacion": "1790000000001",
    "razonSocial": "PROVEEDOR EJEMPLO"
  },

  "documentosSustento": [
    {
      "tipoDocumento": "01",
      "numeroDocumento": "001-001-000000123",
      "fechaEmision": "2026-08-10",

      "numeroAutorizacion": "1408202601179000000000120010010000001231234567811",

      "pago": {
        "fecha": "2026-08-14",
        "formaPago": "20",
        "total": 1000.00
      },

      "retenciones": [
        {
          "codigoImpuesto": "1",
          "codigoRetencion": "303",
          "baseImponible": 1000.00,
          "porcentajeRetener": 10.00,
          "valorRetenido": 100.00
        },
        {
          "codigoImpuesto": "2",
          "codigoRetencion": "1",
          "baseImponible": 150.00,
          "porcentajeRetener": 30.00,
          "valorRetenido": 45.00
        }
      ]
    }
  ],

  "informacionAdicional": [],

  "correo": {
    "enviar": true,
    "destinatarios": [
      "proveedor@example.com"
    ],
    "adjuntarXml": true,
    "adjuntarRide": true
  }
}
```

---

# 21. Retención — diseño

Se utilizará:

```text
documentosSustento[]
```

para soportar la estructura moderna de retención y evitar diseñar el contrato alrededor de una única factura.

Cada documento sustento podrá contener sus propias retenciones.

La versión XML concreta deberá definirse en configuración.

---

# 22. Validaciones retención

Validar:

```text
periodoFiscal
receptor
documentosSustento no vacío
tipoDocumento sustento
numeroDocumento
fechaEmision
retenciones no vacías
baseImponible >= 0
porcentajeRetener >= 0
valorRetenido >= 0
```

Los códigos tributarios deberán validarse contra catálogos soportados por la versión configurada.

No codificar estos catálogos de manera dispersa en controladores.

---

# 23. CONTRATO — GUÍA DE REMISIÓN

```json
{
  "version": "1.0",
  "tipoDocumento": "GUIA_REMISION",
  "externalId": "ERP-GR-000001",

  "emisor": {
    "ruc": "0460028810001",
    "establecimiento": "002",
    "puntoEmision": "018"
  },

  "secuencial": {
    "modo": "AUTO"
  },

  "documento": {
    "fechaEmision": "2026-08-14"
  },

  "transporte": {
    "direccionPartida": "TULCAN",
    "razonSocialTransportista": "TRANSPORTISTA EJEMPLO",
    "tipoIdentificacionTransportista": "05",
    "identificacionTransportista": "0400000000",
    "placa": "ABC1234",
    "fechaInicioTransporte": "2026-08-14",
    "fechaFinTransporte": "2026-08-14"
  },

  "destinatarios": [
    {
      "identificacion": "1790100634001",
      "razonSocial": "CLIENTE EJEMPLO",
      "direccionDestino": "QUITO",
      "motivoTraslado": "ENTREGA DE MERCADERIA",

      "documentoSustento": {
        "tipoDocumento": "01",
        "numeroDocumento": "002-018-000348396",
        "numeroAutorizacion": "3007202601046002881000120020180003483964326898119",
        "fechaEmision": "2026-07-30"
      },

      "detalles": [
        {
          "codigoInterno": "PROD-001",
          "codigoAdicional": null,
          "descripcion": "PRODUCTO EJEMPLO",
          "cantidad": 10
        }
      ]
    }
  ],

  "informacionAdicional": [],

  "correo": {
    "enviar": true,
    "destinatarios": [
      "cliente@example.com"
    ]
  }
}
```

---

# 24. Validaciones guía de remisión

Validar:

```text
direccionPartida
transportista
identificación transportista
placa
fechaInicioTransporte
fechaFinTransporte
destinatarios no vacío
dirección destino
motivo traslado
detalles
cantidad > 0
```

La fecha final no podrá ser anterior a la inicial.

---

# 25. Campos derivados por sri-files

El consumidor NO deberá enviar:

```text
claveAcceso
digitoVerificador
tipoEmision
numeroAutorizacion
fechaAutorizacion
estadoSRI
xml
xmlFirmado
xmlAutorizado
rutaRide
```

Son responsabilidad del backend.

---

# 26. Información tributaria del emisor

Los siguientes datos deberán obtenerse de configuración:

```text
ambiente
tipoEmision
razonSocial
nombreComercial
ruc
direccionMatriz
obligadoContabilidad
contribuyenteEspecial
agenteRetencion
regimen
```

El consumidor únicamente identifica:

```text
RUC
establecimiento
puntoEmision
```

Esto garantiza consistencia.

---

# 27. Fechas

El contrato JSON utilizará ISO 8601:

```text
YYYY-MM-DD
```

Ejemplo:

```json
"fechaEmision": "2026-08-14"
```

El generador XML transformará internamente al formato exigido por el XSD.

No obligar al ERP a utilizar el formato visual del XML.

---

# 28. Valores monetarios

Enviar como JSON number:

```json
"total": 100.25
```

En Java utilizar:

```text
BigDecimal
```

Nunca:

```text
float
double
```

para cálculos tributarios.

La serialización XML aplicará las escalas requeridas.

---

# 29. Validación de totales

Ejemplo factura:

```text
cantidad × precioUnitario
        ↓
subtotal línea
        ↓
- descuento
        ↓
base
        ↓
impuesto
```

El backend deberá detectar inconsistencias.

Ejemplo error:

```json
{
  "code": "DOC_TOTAL_MISMATCH",
  "message": "Los valores calculados no coinciden",
  "details": [
    {
      "field": "detalles[0].precioTotalSinImpuesto",
      "expected": 100.00,
      "received": 90.00
    }
  ]
}
```

---

# 30. Respuesta de recepción

```http
HTTP 202 Accepted
```

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "externalId": "ERP-FACTURA-348396",
  "tipoDocumento": "FACTURA",
  "estado": "RECIBIDO",
  "fechaRecepcion": "2026-08-14T10:31:02-05:00",
  "mensaje": "Documento recibido para procesamiento"
}
```

---

# 31. Respuesta por idempotencia

Si ya existe:

```http
HTTP 200 OK
```

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "externalId": "ERP-FACTURA-348396",
  "tipoDocumento": "FACTURA",
  "estado": "AUTORIZADO",
  "duplicado": true,
  "mensaje": "El documento ya había sido recibido"
}
```

No crear un nuevo comprobante.

---

# 32. Error de validación

```http
HTTP 400 Bad Request
```

```json
{
  "timestamp": "2026-08-14T10:31:02-05:00",
  "status": 400,
  "code": "DOC_VALIDATION_ERROR",
  "message": "El documento contiene errores de validación",
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

# 33. Error de empresa

```json
{
  "code": "COMPANY_NOT_FOUND",
  "message": "No existe configuración para el RUC indicado"
}
```

---

# 34. Error de punto de emisión

```json
{
  "code": "ISSUING_POINT_NOT_FOUND",
  "message": "El establecimiento/punto de emisión no está configurado"
}
```

---

# 35. Error de certificado

```json
{
  "code": "CERTIFICATE_NOT_AVAILABLE",
  "message": "No existe un certificado digital vigente para el emisor"
}
```

Este error puede producirse después de la recepción asíncrona.

En ese caso deberá registrarse en:

```text
documento_error
```

y actualizar:

```text
ERROR_FIRMA
```

---

# 36. Consulta del documento

```http
GET /api/v1/documentos/{uuid}
```

Respuesta:

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "externalId": "ERP-FACTURA-348396",
  "tipoDocumento": "FACTURA",
  "estado": "AUTORIZADO",

  "numeroDocumento": "002-018-000348396",

  "claveAcceso": "3007202601046002881000120020180003483964326898119",

  "numeroAutorizacion": "3007202601046002881000120020180003483964326898119",

  "fechaAutorizacion": "2026-07-30T11:20:30-05:00",

  "receptor": {
    "identificacion": "1790100634001",
    "razonSocial": "ASOCIACION FE Y ALEGRIA ECUADOR"
  },

  "archivos": {
    "xmlAutorizado": true,
    "ride": true
  },

  "correo": {
    "estado": "ENVIADO"
  }
}
```

---

# 37. Consulta de estado

```http
GET /api/v1/documentos/{uuid}/estado
```

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "estado": "PENDIENTE_AUTORIZACION",
  "etapa": "AUTORIZACION",
  "ultimoCambio": "2026-08-14T10:31:15-05:00"
}
```

---

# 38. Historial

```http
GET /api/v1/documentos/{uuid}/historial
```

```json
[
  {
    "estado": "RECIBIDO",
    "fecha": "2026-08-14T10:31:02-05:00"
  },
  {
    "estado": "VALIDANDO",
    "fecha": "2026-08-14T10:31:03-05:00"
  },
  {
    "estado": "XML_GENERADO",
    "fecha": "2026-08-14T10:31:04-05:00"
  }
]
```

---

# 39. Reprocesamiento

```http
POST /api/v1/documentos/{uuid}/reprocesar
```

No recibir nuevamente el JSON salvo que se esté creando un documento distinto.

El reprocesamiento utilizará:

```text
json_original
```

almacenado.

---

# 40. Catálogos

Crear APIs administrativas para catálogos necesarios.

Ejemplo:

```text
GET /api/v1/catalogos/tipos-identificacion

GET /api/v1/catalogos/formas-pago

GET /api/v1/catalogos/impuestos

GET /api/v1/catalogos/codigos-retencion
```

Los catálogos deberán versionarse o asociarse a la versión documental cuando corresponda.

---

# 41. DTOs backend sugeridos

```text
DocumentoRequest

EmisorRequest

SecuencialRequest

ReceptorRequest

CorreoRequest

InformacionAdicionalRequest
```

Especializados:

```text
FacturaRequest
FacturaDetalleRequest
FacturaImpuestoRequest
PagoRequest

LiquidacionCompraRequest

NotaCreditoRequest
NotaCreditoDetalleRequest

NotaDebitoRequest
MotivoNotaDebitoRequest

RetencionRequest
DocumentoSustentoRequest
RetencionDetalleRequest

GuiaRemisionRequest
TransporteRequest
DestinatarioGuiaRequest
DetalleGuiaRequest
```

---

# 42. No utilizar Map<String,Object> como dominio principal

Evitar:

```java
@PostMapping
public ResponseEntity<?> crear(
    @RequestBody Map<String,Object> json
)
```

Utilizar DTOs tipados.

Esto permitirá:

```text
Bean Validation
OpenAPI
autocompletado
tests
refactorización segura
mensajes de error precisos
```

---

# 43. Bean Validation

Ejemplo conceptual:

```java
@NotBlank
private String externalId;

@NotNull
private LocalDate fechaEmision;

@NotEmpty
@Valid
private List<FacturaDetalleRequest> detalles;
```

y validadores de negocio adicionales:

```text
FacturaBusinessValidator
RetencionBusinessValidator
GuiaRemisionBusinessValidator
```

---

# 44. Estrategia de versionado

Existen dos versiones diferentes que no deben confundirse.

## API

```text
/api/v1
```

## Contrato

```json
"version": "1.0"
```

## XML SRI

Ejemplo:

```text
factura 1.1.0
factura 2.x
retención 2.0.0
```

La versión XML será configuración interna.

El ERP no debería seleccionar arbitrariamente la versión XSD.

---

# 45. Compatibilidad

Si el SRI publica una nueva versión:

```text
NO modificar inmediatamente el JSON de todos los consumidores.
```

Primero evaluar si el nuevo campo puede:

```text
derivarse
configurarse
hacerse opcional
```

Solo crear:

```text
contrato JSON 2.0
```

cuando exista una incompatibilidad real.

---

# 46. Seguridad

Nunca permitir desde JSON:

```text
rutaCertificado
passwordCertificado
endpointSRI
ambiente arbitrario
usuarioBD
passwordBD
rutaJasper
```

Son configuraciones internas.

---

# 47. Protección contra manipulación

El backend deberá verificar que:

```text
emisor.ruc
establecimiento
puntoEmision
```

correspondan a configuraciones autorizadas para el consumidor autenticado.

Un token de una empresa no deberá poder emitir comprobantes para otra.

---

# 48. Request ID

Además del `externalId`, cada petición deberá recibir/generar:

```text
X-Request-Id
```

para correlacionar logs.

Ejemplo:

```text
ERP
 ↓
API
 ↓
worker
 ↓
firma
 ↓
SRI
 ↓
correo
```

con el mismo identificador de trazabilidad.

---

# 49. Contratos que deben implementarse primero

Orden recomendado:

```text
1. FACTURA

2. RETENCION

3. NOTA_CREDITO

4. NOTA_DEBITO

5. LIQUIDACION_COMPRA

6. GUIA_REMISION
```

Factura y retención primero porque el servicio actual ya contiene funcionalidad relacionada que puede refactorizarse y reutilizarse.

---

# 50. Pruebas contractuales

Por cada documento crear:

```text
JSON válido mínimo

JSON válido completo

campo requerido faltante

identificación inválida

detalle vacío

impuesto inválido

totales inconsistentes

secuencial duplicado

externalId duplicado

punto de emisión inexistente
```

Además:

```text
JSON → XML esperado
```

para comprobar que una modificación futura no cambie accidentalmente la estructura tributaria.

---

# 51. Fixtures

Crear:

```text
src/test/resources/contracts/

factura/
    factura-minima.json
    factura-completa.json
    factura-invalida.json

retencion/
    retencion-minima.json
    retencion-completa.json

nota-credito/

nota-debito/

liquidacion/

guia-remision/
```

---

# 52. Validación XSD obligatoria

Después de:

```text
JSON → XML
```

ejecutar:

```text
XML → validación XSD
```

antes de:

```text
FIRMA
```

Si falla:

```text
estado = ERROR_XML
```

registrar:

```text
XSD_VALIDATION_ERROR
```

con detalles técnicos en `documento_error`.

---

# 53. Separación de responsabilidades

El ERP dice:

```text
"Quiero emitir esta factura."
```

`sri-files` decide:

```text
qué XML generar
qué versión usar
qué certificado usar
qué endpoint SRI usar
cómo firmar
cuándo reintentar
cómo obtener autorización
cómo generar RIDE
cómo enviar correo
```

Este desacoplamiento es el objetivo principal del nuevo contrato.

---

# 54. Consideraciones para datos tributarios cambiantes

Tarifas, códigos y reglas tributarias pueden cambiar.

Por ello:

```text
NO asumir que una tarifa determinada será permanente.
```

Los contratos reciben:

```text
codigo
codigoPorcentaje
tarifa
```

y el backend valida estos valores contra catálogos/reglas vigentes para la fecha de emisión y la versión documental configurada.

---

# 55. Criterios de aceptación

- [ ] Existe un endpoint único de recepción.
- [ ] Los seis documentos tienen contrato definido.
- [ ] El contrato es independiente del XML.
- [ ] `externalId` es obligatorio.
- [ ] Existe idempotencia.
- [ ] El emisor se resuelve desde configuración.
- [ ] El consumidor no envía certificados.
- [ ] El consumidor no envía endpoints SRI.
- [ ] El consumidor no genera clave de acceso.
- [ ] El consumidor no genera XML.
- [ ] Las fechas utilizan ISO 8601.
- [ ] Los valores monetarios utilizan BigDecimal en backend.
- [ ] Los totales se validan/recalculan.
- [ ] Los errores tienen estructura uniforme.
- [ ] Los DTOs son tipados.
- [ ] Existe Bean Validation.
- [ ] Existe validación de negocio.
- [ ] Existe validación XSD.
- [ ] Los contratos tienen fixtures de prueba.
- [ ] Existe versionado.
- [ ] El contrato permite evolucionar sin acoplar el ERP al SRI.

---

# 56. Advertencia técnica importante

Los ejemplos de este documento definen el **contrato de integración interno propuesto para `sri-files`**.

Antes de considerar productivo cada generador:

```text
FacturaXmlGenerator
LiquidacionXmlGenerator
NotaCreditoXmlGenerator
NotaDebitoXmlGenerator
RetencionXmlGenerator
GuiaRemisionXmlGenerator
```

deberá verificarse campo por campo contra:

1. la ficha técnica vigente del SRI;
2. el XSD correspondiente;
3. la versión XML seleccionada;
4. los catálogos tributarios aplicables;
5. pruebas reales en ambiente de certificación.

El JSON no debe convertirse en una fuente alternativa de reglas tributarias.

---

# 57. Próxima etapa

Con la planificación, el modelo de base y los contratos JSON definidos, el siguiente entregable recomendado es:

```text
ARQUITECTURA_BACKEND_SRI_FILES.md
```

Debe especificar:

```text
paquetes Spring Boot
entidades JPA
repositorios
DTOs
mappers
servicios
procesadores
máquina de estados
workers
schedulers
StorageService
firma
SOAP
autorización
RIDE
correo
seguridad
auditoría
exception handling
Flyway
tests
```

Después podrá construirse:

```text
API_SRI_FILES_V1.md
```

con todos los endpoints, requests, responses y códigos HTTP definitivos.
