# MODELO DE BASE DE DATOS — SRI-FILES

**Proyecto:** Plataforma de Administración de Documentos Electrónicos SRI  
**Motor:** PostgreSQL  
**Backend:** Spring Boot + Spring Data JPA  
**Migraciones recomendadas:** Flyway  
**Versión inicial:** 1.0  
**Fecha:** 2026-08-14

---

# 1. Objetivo

Diseñar una base de datos propia para `sri-files` que permita administrar de forma independiente el ciclo completo de los documentos electrónicos:

```text
JSON RECIBIDO
      ↓
DOCUMENTO REGISTRADO
      ↓
VALIDACIÓN
      ↓
XML GENERADO
      ↓
XML FIRMADO
      ↓
ENVÍO SRI
      ↓
AUTORIZACIÓN
      ↓
XML AUTORIZADO
      ↓
RIDE / PDF
      ↓
CORREO
      ↓
FINALIZADO
```

La base deberá proporcionar:

- independencia respecto de la base del ERP;
- trazabilidad;
- auditoría;
- idempotencia;
- control de estados;
- almacenamiento de referencias a archivos;
- administración de empresas;
- establecimientos;
- puntos de emisión;
- secuenciales;
- certificados digitales;
- configuración SRI;
- correo;
- usuarios;
- roles;
- permisos;
- reintentos;
- errores;
- estadísticas para dashboard.

---

# 2. Principios del modelo

## 2.1 Documento electrónico como entidad central

Todo comprobante será representado por:

```text
documento_electronico
```

independientemente de que corresponda a:

```text
FACTURA
LIQUIDACION_COMPRA
NOTA_CREDITO
NOTA_DEBITO
RETENCION
GUIA_REMISION
```

No se crearán seis motores de administración independientes.

---

## 2.2 JSON original inmutable

El JSON recibido deberá conservarse.

Esto permitirá:

- auditoría;
- reprocesamiento;
- diagnóstico;
- comparación;
- reconstrucción del XML.

El JSON original no deberá modificarse después de ser aceptado.

---

## 2.3 Estado actual + historial

`documento_electronico` tendrá:

```text
estado_actual
```

para consultas rápidas.

Además:

```text
documento_estado_historial
```

almacenará todos los cambios.

---

## 2.4 UUID público

Las APIs no deberán depender del ID numérico interno.

Ejemplo:

```text
/api/v1/documentos/550e8400-e29b-41d4-a716-446655440000
```

---

# 3. Esquema general

```text
empresa
   │
   ├── establecimiento
   │       │
   │       └── punto_emision
   │                │
   │                └── secuencial
   │
   ├── certificado_digital
   │
   ├── configuracion_sri
   │
   ├── configuracion_correo
   │
   └── documento_electronico
                │
                ├── documento_estado_historial
                ├── documento_archivo
                ├── documento_error
                ├── documento_intento_sri
                ├── documento_correo
                └── documento_evento

usuario
   │
   └── usuario_rol
           │
           └── rol
                │
                └── rol_permiso
                         │
                         └── permiso

auditoria
```

---

# 4. Catálogos

Se recomienda utilizar catálogos controlados desde Java mediante enums cuando el conjunto sea estable.

## TipoDocumento

```text
FACTURA
LIQUIDACION_COMPRA
NOTA_CREDITO
NOTA_DEBITO
RETENCION
GUIA_REMISION
```

Códigos SRI asociados:

```text
01 FACTURA
03 LIQUIDACION_COMPRA
04 NOTA_CREDITO
05 NOTA_DEBITO
06 GUIA_REMISION
07 RETENCION
```

---

# 5. Estados

```text
RECIBIDO
VALIDANDO
VALIDADO
XML_GENERADO
FIRMADO
ENVIANDO_SRI
RECIBIDO_SRI
PENDIENTE_AUTORIZACION
AUTORIZADO
RIDE_GENERADO
CORREO_PENDIENTE
CORREO_ENVIADO
FINALIZADO

ERROR_VALIDACION
ERROR_XML
ERROR_FIRMA
ERROR_ENVIO_SRI
DEVUELTO_SRI
NO_AUTORIZADO
ERROR_AUTORIZACION
ERROR_RIDE
ERROR_CORREO

REQUIERE_INTERVENCION
CANCELADO
```

---

# 6. Tabla empresa

```sql
CREATE TABLE empresa (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE,

    ruc VARCHAR(13) NOT NULL UNIQUE,
    razon_social VARCHAR(300) NOT NULL,
    nombre_comercial VARCHAR(300),

    direccion_matriz VARCHAR(500),

    obligado_contabilidad BOOLEAN NOT NULL DEFAULT FALSE,
    contribuyente_especial VARCHAR(50),

    activo BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

Una instalación podrá administrar una o varias empresas.

---

# 7. Tabla establecimiento

```sql
CREATE TABLE establecimiento (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE,

    empresa_id BIGINT NOT NULL
        REFERENCES empresa(id),

    codigo VARCHAR(3) NOT NULL,
    nombre VARCHAR(200),
    direccion VARCHAR(500),

    activo BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_establecimiento
        UNIQUE (empresa_id, codigo)
);
```

---

# 8. Tabla punto_emision

```sql
CREATE TABLE punto_emision (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE,

    establecimiento_id BIGINT NOT NULL
        REFERENCES establecimiento(id),

    codigo VARCHAR(3) NOT NULL,
    nombre VARCHAR(200),

    activo BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_punto_emision
        UNIQUE (establecimiento_id, codigo)
);
```

---

# 9. Tabla secuencial

Esta tabla deberá ser especialmente protegida contra concurrencia.

```sql
CREATE TABLE secuencial (
    id BIGSERIAL PRIMARY KEY,

    punto_emision_id BIGINT NOT NULL
        REFERENCES punto_emision(id),

    tipo_documento VARCHAR(30) NOT NULL,

    valor_actual BIGINT NOT NULL DEFAULT 0,

    activo BOOLEAN NOT NULL DEFAULT TRUE,

    version BIGINT NOT NULL DEFAULT 0,

    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_secuencial
        UNIQUE (punto_emision_id, tipo_documento)
);
```

La obtención del siguiente número deberá realizarse dentro de una transacción.

No utilizar:

```text
SELECT valor_actual + 1
```

sin bloqueo.

Se podrá utilizar:

```sql
SELECT *
FROM secuencial
WHERE id = ?
FOR UPDATE;
```

o un `UPDATE ... RETURNING`.

---

# 10. Tabla documento_electronico

Es la tabla central.

```sql
CREATE TABLE documento_electronico (
    id BIGSERIAL PRIMARY KEY,

    uuid UUID NOT NULL UNIQUE,

    empresa_id BIGINT NOT NULL
        REFERENCES empresa(id),

    establecimiento_id BIGINT
        REFERENCES establecimiento(id),

    punto_emision_id BIGINT
        REFERENCES punto_emision(id),

    tipo_documento VARCHAR(30) NOT NULL,

    ambiente SMALLINT NOT NULL,

    estado_actual VARCHAR(40) NOT NULL,

    external_id VARCHAR(150),

    idempotency_key VARCHAR(200),

    codigo_documento VARCHAR(2),

    establecimiento VARCHAR(3),
    punto_emision VARCHAR(3),
    secuencial VARCHAR(9),

    numero_documento VARCHAR(30),

    clave_acceso VARCHAR(49),

    fecha_emision DATE NOT NULL,

    identificacion_receptor VARCHAR(20),
    razon_social_receptor VARCHAR(300),
    email_receptor VARCHAR(320),

    moneda VARCHAR(10),

    subtotal NUMERIC(18,6),
    descuento NUMERIC(18,6),
    impuestos NUMERIC(18,6),
    total NUMERIC(18,6),

    json_original JSONB NOT NULL,

    numero_autorizacion VARCHAR(100),
    fecha_autorizacion TIMESTAMP,

    mensaje_sri TEXT,

    requiere_intervencion BOOLEAN NOT NULL DEFAULT FALSE,

    intentos_procesamiento INTEGER NOT NULL DEFAULT 0,

    fecha_recepcion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_inicio_procesamiento TIMESTAMP,
    fecha_finalizacion TIMESTAMP,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_documento_external
        UNIQUE (empresa_id, external_id),

    CONSTRAINT uk_documento_idempotency
        UNIQUE (empresa_id, idempotency_key),

    CONSTRAINT uk_documento_numero
        UNIQUE (
            empresa_id,
            codigo_documento,
            establecimiento,
            punto_emision,
            secuencial
        )
);
```

---

# 11. Consideraciones sobre JSONB

`json_original` se almacenará como:

```text
JSONB
```

Esto permite conservar exactamente el documento funcional y, si fuera necesario, realizar búsquedas específicas.

Sin embargo:

> JSONB no reemplaza los campos normalizados utilizados frecuentemente para búsquedas.

Por eso se conservan también:

```text
identificacion_receptor
numero_documento
clave_acceso
fecha_emision
estado_actual
total
tipo_documento
```

---

# 12. Tabla documento_estado_historial

```sql
CREATE TABLE documento_estado_historial (
    id BIGSERIAL PRIMARY KEY,

    documento_id BIGINT NOT NULL
        REFERENCES documento_electronico(id),

    estado_anterior VARCHAR(40),
    estado_nuevo VARCHAR(40) NOT NULL,

    descripcion TEXT,

    origen VARCHAR(30),

    usuario_id BIGINT,

    metadata JSONB,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

Ejemplo:

```text
RECIBIDO
    ↓
VALIDANDO
    ↓
VALIDADO
    ↓
XML_GENERADO
```

Cada transición genera un registro.

---

# 13. Tabla documento_archivo

```sql
CREATE TABLE documento_archivo (
    id BIGSERIAL PRIMARY KEY,

    documento_id BIGINT NOT NULL
        REFERENCES documento_electronico(id),

    tipo_archivo VARCHAR(30) NOT NULL,

    nombre_archivo VARCHAR(255) NOT NULL,

    mime_type VARCHAR(100),

    ruta TEXT,

    hash_sha256 VARCHAR(64),

    tamanio BIGINT,

    version INTEGER NOT NULL DEFAULT 1,

    activo BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

Tipos:

```text
JSON_ORIGINAL
XML_GENERADO
XML_FIRMADO
XML_AUTORIZADO
RIDE_PDF
RESPUESTA_SRI
```

---

# 14. Estrategia de almacenamiento

No se recomienda almacenar PDFs grandes directamente en PostgreSQL salvo que exista una necesidad específica.

La base almacenará:

```text
ruta
hash
mime_type
tamaño
metadata
```

Los archivos podrán mantenerse inicialmente en:

```text
/data/sri-files/
```

Ejemplo:

```text
/data/sri-files/
    2026/
      08/
        factura/
          1408202601...
             original.json
             generado.xml
             firmado.xml
             autorizado.xml
             ride.pdf
```

La implementación deberá abstraerse mediante:

```text
StorageService
```

para poder migrar posteriormente a almacenamiento S3/MinIO sin modificar el dominio.

---

# 15. Tabla documento_error

```sql
CREATE TABLE documento_error (
    id BIGSERIAL PRIMARY KEY,

    documento_id BIGINT NOT NULL
        REFERENCES documento_electronico(id),

    etapa VARCHAR(40) NOT NULL,

    codigo VARCHAR(100),

    mensaje TEXT NOT NULL,

    detalle TEXT,

    stack_trace TEXT,

    recuperable BOOLEAN NOT NULL DEFAULT FALSE,

    resuelto BOOLEAN NOT NULL DEFAULT FALSE,

    resuelto_por BIGINT,

    fecha_resolucion TIMESTAMP,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

Ejemplos de etapa:

```text
VALIDACION
XML
FIRMA
ENVIO_SRI
AUTORIZACION
RIDE
CORREO
```

---

# 16. Tabla documento_intento_sri

Cada llamada importante al SRI deberá quedar registrada.

```sql
CREATE TABLE documento_intento_sri (
    id BIGSERIAL PRIMARY KEY,

    documento_id BIGINT NOT NULL
        REFERENCES documento_electronico(id),

    operacion VARCHAR(30) NOT NULL,

    numero_intento INTEGER NOT NULL,

    endpoint TEXT,

    fecha_inicio TIMESTAMP NOT NULL,
    fecha_fin TIMESTAMP,

    duracion_ms BIGINT,

    resultado VARCHAR(30),

    codigo_respuesta VARCHAR(100),

    mensaje TEXT,

    respuesta JSONB,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

Operaciones:

```text
RECEPCION
AUTORIZACION
```

---

# 17. Tabla documento_correo

```sql
CREATE TABLE documento_correo (
    id BIGSERIAL PRIMARY KEY,

    documento_id BIGINT NOT NULL
        REFERENCES documento_electronico(id),

    destinatario VARCHAR(320) NOT NULL,

    copia TEXT,

    asunto VARCHAR(500),

    estado VARCHAR(30) NOT NULL,

    numero_intentos INTEGER NOT NULL DEFAULT 0,

    fecha_programada TIMESTAMP,
    fecha_ultimo_intento TIMESTAMP,
    fecha_envio TIMESTAMP,

    mensaje_error TEXT,

    external_message_id VARCHAR(255),

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

Estados:

```text
PENDIENTE
ENVIANDO
ENVIADO
ERROR
CANCELADO
```

---

# 18. Tabla documento_evento

Esta tabla puede utilizarse para registrar eventos funcionales adicionales que no correspondan exactamente a cambios de estado.

```sql
CREATE TABLE documento_evento (
    id BIGSERIAL PRIMARY KEY,

    documento_id BIGINT NOT NULL
        REFERENCES documento_electronico(id),

    tipo VARCHAR(60) NOT NULL,

    descripcion TEXT,

    metadata JSONB,

    usuario_id BIGINT,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

Ejemplos:

```text
REPROCESAMIENTO_SOLICITADO

XML_DESCARGADO

RIDE_DESCARGADO

CONSULTA_MANUAL_SRI

CORREO_REENVIADO
```

---

# 19. Tabla certificado_digital

```sql
CREATE TABLE certificado_digital (
    id BIGSERIAL PRIMARY KEY,

    uuid UUID NOT NULL UNIQUE,

    empresa_id BIGINT NOT NULL
        REFERENCES empresa(id),

    nombre VARCHAR(200) NOT NULL,

    ruta_archivo TEXT NOT NULL,

    alias_certificado VARCHAR(200),

    fecha_emision TIMESTAMP,
    fecha_expiracion TIMESTAMP NOT NULL,

    huella VARCHAR(255),

    activo BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

## IMPORTANTE

La contraseña del certificado:

```text
NO debe almacenarse en texto plano.
```

Opciones:

- secreto de Docker;
- variable de entorno;
- gestor de secretos;
- valor cifrado con llave externa.

---

# 20. Tabla configuracion_sri

```sql
CREATE TABLE configuracion_sri (
    id BIGSERIAL PRIMARY KEY,

    empresa_id BIGINT NOT NULL
        REFERENCES empresa(id),

    ambiente SMALLINT NOT NULL,

    recepcion_url TEXT,
    autorizacion_url TEXT,

    timeout_conexion_ms INTEGER NOT NULL DEFAULT 10000,
    timeout_respuesta_ms INTEGER NOT NULL DEFAULT 30000,

    max_reintentos INTEGER NOT NULL DEFAULT 5,

    activo BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_configuracion_sri
        UNIQUE (empresa_id, ambiente)
);
```

Ambiente:

```text
1 = PRUEBAS
2 = PRODUCCIÓN
```

---

# 21. Tabla configuracion_correo

```sql
CREATE TABLE configuracion_correo (
    id BIGSERIAL PRIMARY KEY,

    empresa_id BIGINT NOT NULL
        REFERENCES empresa(id),

    remitente VARCHAR(320),

    nombre_remitente VARCHAR(200),

    enviar_xml BOOLEAN NOT NULL DEFAULT TRUE,
    enviar_ride BOOLEAN NOT NULL DEFAULT TRUE,

    plantilla_asunto VARCHAR(500),

    activo BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

Las credenciales del servidor de correo no deberán guardarse en texto plano.

---

# 22. Tabla plantilla_ride

```sql
CREATE TABLE plantilla_ride (
    id BIGSERIAL PRIMARY KEY,

    empresa_id BIGINT NOT NULL
        REFERENCES empresa(id),

    tipo_documento VARCHAR(30) NOT NULL,

    nombre VARCHAR(200) NOT NULL,

    ruta_jasper TEXT NOT NULL,

    version INTEGER NOT NULL DEFAULT 1,

    predeterminada BOOLEAN NOT NULL DEFAULT FALSE,

    activa BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

---

# 23. Tabla recurso_empresa

Para logos y recursos gráficos:

```sql
CREATE TABLE recurso_empresa (
    id BIGSERIAL PRIMARY KEY,

    empresa_id BIGINT NOT NULL
        REFERENCES empresa(id),

    tipo VARCHAR(30) NOT NULL,

    nombre VARCHAR(200),

    ruta TEXT NOT NULL,

    mime_type VARCHAR(100),

    activo BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

Tipos:

```text
LOGO_PRINCIPAL
LOGO_SECUNDARIO
FIRMA_GRAFICA
MARCA_AGUA
```

---

# 24. Usuarios

```sql
CREATE TABLE usuario (
    id BIGSERIAL PRIMARY KEY,

    uuid UUID NOT NULL UNIQUE,

    username VARCHAR(100) NOT NULL UNIQUE,

    nombre VARCHAR(200) NOT NULL,

    email VARCHAR(320),

    password_hash VARCHAR(255) NOT NULL,

    activo BOOLEAN NOT NULL DEFAULT TRUE,

    bloqueado BOOLEAN NOT NULL DEFAULT FALSE,

    ultimo_acceso TIMESTAMP,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

---

# 25. Roles

```sql
CREATE TABLE rol (
    id BIGSERIAL PRIMARY KEY,

    codigo VARCHAR(60) NOT NULL UNIQUE,

    nombre VARCHAR(150) NOT NULL,

    descripcion TEXT,

    activo BOOLEAN NOT NULL DEFAULT TRUE
);
```

Roles iniciales:

```text
SUPER_ADMIN
ADMIN
OPERADOR
CONSULTA
AUDITOR
```

---

# 26. Permisos

```sql
CREATE TABLE permiso (
    id BIGSERIAL PRIMARY KEY,

    codigo VARCHAR(100) NOT NULL UNIQUE,

    nombre VARCHAR(200) NOT NULL,

    modulo VARCHAR(100),

    descripcion TEXT
);
```

Ejemplos:

```text
DOCUMENTO_VER
DOCUMENTO_DESCARGAR
DOCUMENTO_REPROCESAR
DOCUMENTO_REENVIAR_SRI
DOCUMENTO_REENVIAR_CORREO

CONFIGURACION_VER
CONFIGURACION_EDITAR

CERTIFICADO_VER
CERTIFICADO_ADMINISTRAR

USUARIO_VER
USUARIO_ADMINISTRAR

AUDITORIA_VER
```

---

# 27. Relación usuario - rol

```sql
CREATE TABLE usuario_rol (
    usuario_id BIGINT NOT NULL
        REFERENCES usuario(id),

    rol_id BIGINT NOT NULL
        REFERENCES rol(id),

    PRIMARY KEY (usuario_id, rol_id)
);
```

---

# 28. Relación rol - permiso

```sql
CREATE TABLE rol_permiso (
    rol_id BIGINT NOT NULL
        REFERENCES rol(id),

    permiso_id BIGINT NOT NULL
        REFERENCES permiso(id),

    PRIMARY KEY (rol_id, permiso_id)
);
```

---

# 29. Auditoría

```sql
CREATE TABLE auditoria (
    id BIGSERIAL PRIMARY KEY,

    usuario_id BIGINT,

    accion VARCHAR(100) NOT NULL,

    entidad VARCHAR(100),

    entidad_id VARCHAR(150),

    ip VARCHAR(50),

    user_agent TEXT,

    datos_anteriores JSONB,

    datos_nuevos JSONB,

    resultado VARCHAR(30),

    descripcion TEXT,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

---

# 30. Índices fundamentales

La tabla documental crecerá rápidamente.

Crear:

```sql
CREATE INDEX idx_documento_fecha
ON documento_electronico(fecha_emision);

CREATE INDEX idx_documento_estado
ON documento_electronico(estado_actual);

CREATE INDEX idx_documento_tipo
ON documento_electronico(tipo_documento);

CREATE INDEX idx_documento_identificacion
ON documento_electronico(identificacion_receptor);

CREATE INDEX idx_documento_clave
ON documento_electronico(clave_acceso);

CREATE INDEX idx_documento_numero
ON documento_electronico(numero_documento);

CREATE INDEX idx_documento_recepcion
ON documento_electronico(fecha_recepcion);

CREATE INDEX idx_documento_empresa_estado
ON documento_electronico(empresa_id, estado_actual);

CREATE INDEX idx_documento_empresa_fecha
ON documento_electronico(empresa_id, fecha_emision);

CREATE INDEX idx_historial_documento
ON documento_estado_historial(documento_id, created_at);

CREATE INDEX idx_error_documento
ON documento_error(documento_id);

CREATE INDEX idx_intento_documento
ON documento_intento_sri(documento_id);

CREATE INDEX idx_correo_documento
ON documento_correo(documento_id);

CREATE INDEX idx_correo_estado
ON documento_correo(estado);
```

---

# 31. Índices para procesamiento

Muy importantes para schedulers/workers:

```sql
CREATE INDEX idx_documento_procesamiento
ON documento_electronico(estado_actual, fecha_recepcion)
WHERE estado_actual IN (
    'RECIBIDO',
    'PENDIENTE_AUTORIZACION',
    'CORREO_PENDIENTE'
);
```

Esto evita recorrer toda la tabla.

---

# 32. Idempotencia

Se utilizarán:

```text
external_id
idempotency_key
```

Ejemplo:

```text
empresa:
0460028810001

external_id:
ERP-FACTURA-348396
```

Una segunda petición con el mismo identificador no deberá crear un documento nuevo.

Respuesta esperada:

```json
{
  "id": "uuid-existente",
  "estado": "AUTORIZADO",
  "duplicado": true
}
```

---

# 33. Clave de acceso

Crear restricción lógica para evitar duplicados.

Cuando la clave exista:

```sql
CREATE UNIQUE INDEX uk_documento_clave_acceso
ON documento_electronico(clave_acceso)
WHERE clave_acceso IS NOT NULL;
```

---

# 34. Control de concurrencia

Problemas que debemos evitar:

```text
dos workers procesando el mismo documento;

dos solicitudes obteniendo el mismo secuencial;

dos schedulers consultando la misma autorización;

dos correos enviados simultáneamente.
```

Utilizar:

```text
transacciones
optimistic locking
pessimistic locking cuando sea necesario
SELECT ... FOR UPDATE SKIP LOCKED
```

Ejemplo para workers:

```sql
SELECT id
FROM documento_electronico
WHERE estado_actual = 'RECIBIDO'
ORDER BY fecha_recepcion
FOR UPDATE SKIP LOCKED
LIMIT 10;
```

---

# 35. Dashboard

No crear inicialmente tablas duplicadas únicamente para estadísticas.

Las consultas pueden utilizar:

```text
documento_electronico
documento_error
documento_correo
```

Ejemplos:

## Documentos de hoy

```sql
SELECT COUNT(*)
FROM documento_electronico
WHERE fecha_recepcion::date = CURRENT_DATE;
```

## Por estado

```sql
SELECT estado_actual, COUNT(*)
FROM documento_electronico
WHERE fecha_recepcion::date = CURRENT_DATE
GROUP BY estado_actual;
```

## Por tipo

```sql
SELECT tipo_documento, COUNT(*)
FROM documento_electronico
WHERE fecha_recepcion::date = CURRENT_DATE
GROUP BY tipo_documento;
```

---

# 36. Vistas recomendadas

## Vista resumen documento

```sql
CREATE VIEW vw_documento_resumen AS
SELECT
    d.uuid,
    d.tipo_documento,
    d.numero_documento,
    d.clave_acceso,
    d.fecha_emision,
    d.identificacion_receptor,
    d.razon_social_receptor,
    d.total,
    d.estado_actual,
    d.numero_autorizacion,
    d.fecha_autorizacion,
    d.fecha_recepcion,
    d.fecha_finalizacion
FROM documento_electronico d;
```

---

# 37. Retención de datos

No eliminar automáticamente documentos electrónicos autorizados.

Se deberá definir una política formal para:

```text
JSON
XML
XML firmado
XML autorizado
RIDE
auditoría
logs técnicos
```

Los logs técnicos podrán tener menor tiempo de conservación.

La política definitiva deberá alinearse posteriormente con las obligaciones tributarias, contables y documentales aplicables.

---

# 38. Flyway

Estructura recomendada:

```text
src/main/resources/db/migration/

V1__crear_empresa.sql
V2__crear_establecimientos.sql
V3__crear_documentos.sql
V4__crear_historial.sql
V5__crear_archivos.sql
V6__crear_integracion_sri.sql
V7__crear_correo.sql
V8__crear_seguridad.sql
V9__crear_auditoria.sql
V10__crear_indices.sql
V11__datos_iniciales.sql
```

No crear toda la base manualmente en producción.

Las modificaciones deberán quedar versionadas.

---

# 39. Configuración JPA recomendada

En producción:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
```

No utilizar:

```yaml
ddl-auto: create
```

ni:

```yaml
ddl-auto: update
```

como estrategia de evolución productiva.

Flyway será responsable del esquema.

---

# 40. Relaciones principales

```text
EMPRESA
  │
  ├──── ESTABLECIMIENTO
  │        │
  │        └──── PUNTO_EMISION
  │                  │
  │                  └──── SECUENCIAL
  │
  ├──── CERTIFICADO_DIGITAL
  │
  ├──── CONFIGURACION_SRI
  │
  ├──── CONFIGURACION_CORREO
  │
  ├──── PLANTILLA_RIDE
  │
  └──── DOCUMENTO_ELECTRONICO
             │
             ├──── DOCUMENTO_ESTADO_HISTORIAL
             │
             ├──── DOCUMENTO_ARCHIVO
             │
             ├──── DOCUMENTO_ERROR
             │
             ├──── DOCUMENTO_INTENTO_SRI
             │
             ├──── DOCUMENTO_CORREO
             │
             └──── DOCUMENTO_EVENTO
```

---

# 41. Estrategia para detalles de cada comprobante

En la primera versión se recomienda:

```text
guardar JSON original completo
+
normalizar solamente campos administrativos y de búsqueda
```

No es obligatorio duplicar inmediatamente todos los detalles tributarios en decenas de tablas como:

```text
factura_detalle
factura_impuesto
retencion_detalle
guia_destinatario
etc.
```

porque `sri-files` tiene como responsabilidad principal:

```text
PROCESAR
ADMINISTRAR
TRAZAR
AUTORIZAR
GENERAR RIDE
ENVIAR
```

y no sustituir al ERP contable.

Si posteriormente se requiere reportería tributaria avanzada, se podrá crear un modelo analítico adicional.

---

# 42. Qué NO almacenar en esta base

`sri-files` no deberá convertirse en un ERP paralelo.

Evitar administrar aquí:

```text
inventario
productos maestros
clientes maestros
contabilidad
cartera
cuentas por cobrar
compras
proveedores maestros
movimientos bancarios
```

El JSON contiene la información necesaria para generar el documento, pero el sistema origen continúa siendo propietario de sus datos comerciales.

---

# 43. Estrategia de integración con ERP

El ERP únicamente deberá conocer:

```text
external_id
uuid sri-files
estado
clave_acceso
numero_autorizacion
```

Opcionalmente podrá consultar:

```text
URL XML
URL RIDE
```

No deberá conocer:

```text
ruta física
WSDL
SOAP
certificado
contraseña
Jasper
reintentos internos
```

---

# 44. Ejemplo de ciclo completo en base de datos

## Paso 1

ERP envía factura.

Se crea:

```text
documento_electronico

estado_actual = RECIBIDO
```

## Paso 2

Se registra:

```text
documento_estado_historial

NULL → RECIBIDO
```

## Paso 3

Worker procesa.

```text
RECIBIDO → VALIDANDO
```

## Paso 4

XML generado.

Se crea:

```text
documento_archivo
tipo = XML_GENERADO
```

## Paso 5

XML firmado.

```text
documento_archivo
tipo = XML_FIRMADO
```

## Paso 6

Se envía al SRI.

```text
documento_intento_sri
operacion = RECEPCION
```

## Paso 7

Autorización.

```text
documento_intento_sri
operacion = AUTORIZACION
```

y:

```text
documento_electronico.numero_autorizacion
documento_electronico.fecha_autorizacion
```

## Paso 8

XML autorizado.

```text
documento_archivo
tipo = XML_AUTORIZADO
```

## Paso 9

RIDE.

```text
documento_archivo
tipo = RIDE_PDF
```

## Paso 10

Correo.

```text
documento_correo
estado = ENVIADO
```

## Paso 11

Documento:

```text
estado_actual = FINALIZADO
```

---

# 45. Backups

La estrategia de backup deberá considerar dos elementos:

```text
PostgreSQL
+
almacenamiento documental
```

No sirve respaldar únicamente PostgreSQL si los XML y PDF están en disco.

Se deberá respaldar:

```text
base de datos
/data/sri-files
configuraciones
plantillas Jasper
```

Los certificados deberán tener un procedimiento seguro independiente.

---

# 46. Criterios de aceptación del modelo

- [ ] Existe una base independiente para `sri-files`.
- [ ] Una empresa puede tener múltiples establecimientos.
- [ ] Un establecimiento puede tener múltiples puntos de emisión.
- [ ] Los secuenciales están separados por tipo documental.
- [ ] No pueden generarse secuenciales duplicados por concurrencia.
- [ ] Los seis tipos documentales utilizan la entidad central.
- [ ] Se conserva el JSON original.
- [ ] Existe idempotencia.
- [ ] Se conserva historial completo de estados.
- [ ] Se registran errores por etapa.
- [ ] Se registran intentos SRI.
- [ ] Se registran correos.
- [ ] Se administran referencias a XML/PDF.
- [ ] Existe control de certificados.
- [ ] Existe configuración SRI.
- [ ] Existen usuarios, roles y permisos.
- [ ] Existe auditoría.
- [ ] Existen índices para búsquedas frecuentes.
- [ ] Existen índices específicos para workers.
- [ ] El esquema se administra mediante Flyway.
- [ ] JPA valida el esquema en producción.
- [ ] La BD no intenta reemplazar los módulos comerciales del ERP.

---

# 47. Siguiente etapa

Una vez aprobado este modelo, el siguiente documento deberá ser:

```text
CONTRATOS_JSON_SRI_FILES.md
```

Allí se definirá exactamente qué JSON debe recibir la API para:

```text
01 Factura
03 Liquidación de compra
04 Nota de crédito
05 Nota de débito
06 Guía de remisión
07 Comprobante de retención
```

Cada contrato deberá incluir:

- campos obligatorios;
- campos opcionales;
- tipos;
- validaciones;
- estructura de detalles;
- impuestos;
- información adicional;
- correo;
- referencias a documentos modificados;
- ejemplos completos;
- respuesta de aceptación;
- errores de validación.

Después se podrá generar:

```text
V1__crear_empresa.sql
...
V11__datos_iniciales.sql
```

y las entidades JPA correspondientes.

---

# 48. Decisión arquitectónica final

La base de datos de `sri-files` será una **base operacional de documentos electrónicos**, no una copia de la base comercial del ERP.

Su función será responder de forma confiable a cinco preguntas:

```text
1. ¿Qué documento recibimos?

2. ¿En qué estado se encuentra?

3. ¿Qué ocurrió durante su procesamiento?

4. ¿Qué archivos y respuestas produjo?

5. ¿Quién o qué realizó cada acción?
```

Si el modelo puede responder esas cinco preguntas de forma íntegra, trazable y auditable, estará cumpliendo correctamente su función.
