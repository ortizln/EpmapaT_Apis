# Guía Jasper RIDE - SRI Files

## Objetivo

Esta guía documenta cómo diseñar, validar y publicar plantillas RIDE en formato `.jrxml` para `SRI Files`, utilizando JasperReports y los endpoints del backend ya implementados.

Fecha de referencia: `2026-08-24`.

---

## Endpoints disponibles

### Plantillas base

Descarga una plantilla base por tipo de documento:

```http
GET /api/v1/plantillas-ride/base/{tipoDocumento}
```

Valores soportados:

- `FACTURA`
- `LIQUIDACION_COMPRA`
- `NOTA_CREDITO`
- `NOTA_DEBITO`
- `GUIA_REMISION`
- `RETENCION`

---

### Crear plantilla JRXML

```http
POST /api/v1/empresas/{empresaId}/plantillas-ride
Content-Type: multipart/form-data
```

Partes:

- `data`: JSON con metadatos
- `file`: archivo `.jrxml`

Ejemplo del campo `data`:

```json
{
  "tipoDocumento": "FACTURA",
  "nombre": "Factura corporativa A4",
  "version": "1.0.0",
  "predeterminada": true,
  "activa": true
}
```

---

### Actualizar plantilla JRXML

```http
PUT /api/v1/plantillas-ride/{uuid}
Content-Type: multipart/form-data
```

Se puede enviar un nuevo `file` `.jrxml` o solo actualizar metadatos.

---

### Verificar compilación JRXML

```http
POST /api/v1/plantillas-ride/{uuid}/verificar
```

Valida que el `.jrxml` compile correctamente en JasperReports.

---

### Vista previa contra documento real

```http
GET /api/v1/plantillas-ride/{uuid}/preview/{documentoUuid}
```

Genera un PDF de vista previa usando:

- la plantilla seleccionada
- el XML autorizado del documento
- la empresa dueña del documento

Restricciones:

- la plantilla y el documento deben pertenecer a la misma empresa
- el tipo de documento debe coincidir
- el documento debe tener `XML_AUTORIZADO`

---

### Uso automático en emisión/regeneración

Cuando existe una plantilla:

- `activa = true`
- `predeterminada = true`
- del mismo `tipoDocumento`
- para la misma empresa

el backend la utilizará automáticamente durante la regeneración del RIDE.

Si no existe plantilla activa/predeterminada, el sistema usa el generador legado actual.

---

## Cómo obtiene datos Jasper el backend

El renderer actual:

1. Lee el `XML_AUTORIZADO`.
2. Si existe nodo `<comprobante>`, parsea el XML interno.
3. Extrae nodos hoja del XML y los expone como parámetros Jasper.
4. Construye una colección principal para el `detail`, según el tipo de documento.

Servicio base:

- `backend/src/main/java/com/erp/sri_files/service/JasperRideTemplateRenderer.java`

---

## Parámetros globales siempre disponibles

Estos parámetros pueden usarse en cualquier `.jrxml`:

- `TipoDocumento`
- `EmpresaRuc`
- `EmpresaRazonSocial`
- `EmpresaNombreComercial`
- `EmpresaDireccionMatriz`
- `RazonSocial`
- `Ruc`
- `NombreComercial`
- `DireccionMatriz`
- `FechaEmision`
- `ClaveAcceso`
- `NumeroAutorizacion`
- `FechaAutorizacion`
- `Ambiente`
- `numeroDocumento`
- `NumeroDocumento`
- `NroFactura`

Notas:

- `NumeroDocumento` y `numeroDocumento` se calculan a partir de `estab + "-" + ptoEmi + "-" + secuencial` si no vinieran directamente.
- `NroFactura` hoy se alinea con `numeroDocumento`.

---

## Parámetros dinámicos desde el XML

Además de los parámetros globales, el renderer expone cualquier nodo hoja del XML con su nombre literal.

Ejemplos frecuentes:

- `razonSocial`
- `ruc`
- `claveAcceso`
- `estab`
- `ptoEmi`
- `secuencial`
- `fechaEmision`
- `dirMatriz`
- `razonSocialComprador`
- `identificacionComprador`
- `importeTotal`
- `totalSinImpuestos`
- `obligadoContabilidad`
- `dirEstablecimiento`
- `guiaRemision`
- `fechaIniTransporte`
- `fechaFinTransporte`
- `razonSocialSujetoRetenido`
- `identificacionSujetoRetenido`

Recomendación:

- en JasperStudio primero diseña usando los parámetros globales
- luego agrega parámetros adicionales específicos del XML real que manejes

---

## Recursos gráficos disponibles

Si la empresa tiene recursos activos cargados, el renderer expone:

- `LOGO_PRINCIPAL_PATH`
- `LOGO_PRINCIPAL_NAME`
- `LOGO_SECUNDARIO_PATH`
- `LOGO_SECUNDARIO_NAME`
- `MARCA_AGUA_PATH`
- `MARCA_AGUA_NAME`

Uso recomendado en Jasper:

- declara un parámetro `java.lang.String`
- úsalo en un `imageExpression`, por ejemplo:

```java
new java.io.File($P{LOGO_PRINCIPAL_PATH})
```

Si el recurso no existe, el parámetro puede no venir informado.

---

## Colección principal para Detail

El renderer usa una colección principal distinta según el tipo de documento.

### FACTURA

Nodo iterado:

- `detalle`

Campos usuales:

- `codigoPrincipal`
- `descripcion`
- `cantidad`
- `precioUnitario`
- `descuento`
- `precioTotalSinImpuesto`

### LIQUIDACION_COMPRA

Nodo iterado:

- `detalle`

Campos usuales:

- `codigoPrincipal`
- `descripcion`
- `cantidad`
- `precioUnitario`
- `precioTotalSinImpuesto`

### NOTA_CREDITO

Nodo iterado:

- `detalle`

Campos usuales:

- `codigoPrincipal`
- `descripcion`
- `cantidad`
- `precioUnitario`
- `precioTotalSinImpuesto`

### NOTA_DEBITO

Nodo iterado:

- `detalle`

Campos usuales:

- `codigoInterno`
- `descripcion`
- `cantidad`
- `precioUnitario`
- `precioTotalSinImpuesto`

Si el XML trae motivos o estructura distinta, se recomienda inspeccionar el XML autorizado real y ajustar el `.jrxml`.

### GUIA_REMISION

Nodo iterado:

- `destinatario`

Campos usuales:

- `identificacionDestinatario`
- `razonSocialDestinatario`
- `dirDestinatario`
- `motivoTraslado`
- `docAduaneroUnico`
- `codEstabDestino`
- `ruta`

### RETENCION

Nodo iterado:

- `impuesto`

Campos usuales:

- `codigo`
- `codigoRetencion`
- `baseImponible`
- `porcentajeRetener`
- `valorRetenido`
- `codDocSustento`
- `numDocSustento`
- `fechaEmisionDocSustento`

---

## Caso fallback del detail

Si el renderer no encuentra nodos para la colección principal:

- crea una fila única con los nodos hoja del documento completo

Esto evita que Jasper falle por un dataset vacío, pero no reemplaza un diseño correcto.

---

## Recomendación de estructura en JasperStudio

### Para encabezado

Usar parámetros:

- `$P{RazonSocial}`
- `$P{Ruc}`
- `$P{NumeroDocumento}`
- `$P{FechaEmision}`
- `$P{ClaveAcceso}`

### Para pie o resumen

Usar parámetros del XML:

- `$P{importeTotal}`
- `$P{totalSinImpuestos}`
- `$P{totalDescuento}`
- `$P{razonSocialComprador}`

### Para detalle

Usar fields según el tipo:

- factura: `$F{descripcion}`, `$F{cantidad}`, `$F{precioUnitario}`
- retención: `$F{codigoRetencion}`, `$F{baseImponible}`, `$F{valorRetenido}`
- guía: `$F{razonSocialDestinatario}`, `$F{dirDestinatario}`

---

## Convenciones recomendadas

- usar solo `.jrxml`, no `.jasper`
- mantener una versión semántica en el campo `version`
- marcar solo una plantilla como `predeterminada` por empresa y tipo
- probar siempre con `preview` antes de activar en producción
- si usas imágenes, cargarlas como recursos de empresa y no hardcodear rutas locales

---

## Flujo recomendado de trabajo

1. Descargar plantilla base por tipo.
2. Abrirla en JasperSoft Studio.
3. Ajustar layout, branding y expresiones.
4. Subir el `.jrxml` al backend.
5. Ejecutar `verificar`.
6. Ejecutar `preview` contra un documento real autorizado.
7. Marcarla como `predeterminada` y `activa`.
8. Regenerar RIDE desde el módulo de documentos para validar el flujo completo.

---

## Limitaciones actuales

- El renderer actual trabaja con un `JRMapCollectionDataSource`.
- Los parámetros disponibles dependen de los nodos hoja del XML autorizado real.
- No existe aún un catálogo API que enumere automáticamente todos los parámetros detectados por documento.
- El preview requiere que el documento ya tenga XML autorizado almacenado.

---

## Próxima mejora recomendada

La siguiente mejora ideal es agregar un endpoint como:

```http
GET /api/v1/documentos/{uuid}/ride/contrato
```

para devolver:

- parámetros disponibles
- fields detectados para el `detail`
- recursos gráficos activos

Así el frontend podría mostrar al diseñador del reporte exactamente qué variables tiene cada documento real.
