# PLANIFICACIÓN DE REESTRUCTURACIÓN — SRI-FILES

**Proyecto:** Plataforma de Administración de Documentos Electrónicos SRI  
**Base actual:** `sri-files`  
**Backend:** Java 17 + Spring Boot 3.4.x  
**Base de datos:** PostgreSQL  
**Frontend propuesto:** Angular  
**Fecha:** 2026-08-14

---

## 1. Objetivo general

Reestructurar `sri-files` para convertirlo en una plataforma autónoma, desacoplada del ERP y orientada exclusivamente a la recepción, procesamiento, administración, trazabilidad y consulta de documentos electrónicos del SRI.

La plataforma deberá recibir un JSON por documento y administrar internamente todo su ciclo:

```text
JSON
  ↓
VALIDACIÓN
  ↓
REGISTRO EN BASE DE DATOS
  ↓
GENERACIÓN XML
  ↓
FIRMA ELECTRÓNICA
  ↓
ENVÍO AL SRI
  ↓
CONSULTA DE AUTORIZACIÓN
  ↓
XML AUTORIZADO
  ↓
GENERACIÓN RIDE / PDF
  ↓
ENVÍO POR CORREO
  ↓
FINALIZADO
```

El sistema origen no deberá controlar internamente estos pasos. Su responsabilidad terminará al entregar correctamente el JSON y conservar el identificador retornado por `sri-files`.

---

## 2. Principios de la reestructuración

1. No reescribir componentes funcionales sin necesidad.
2. Reutilizar la integración SOAP existente con el SRI.
3. Reutilizar los mecanismos actuales de firma electrónica.
4. Reutilizar JasperReports y los componentes existentes de PDF cuando sean adecuados.
5. Separar controladores, lógica de negocio, persistencia e infraestructura.
6. Crear una base de datos propia para documentos electrónicos.
7. Eliminar la dependencia funcional del estado interno del ERP.
8. Implementar trazabilidad completa.
9. Implementar procesamiento asíncrono y reintentos controlados.
10. Diseñar una arquitectura extensible para todos los comprobantes.
11. Mantener compatibilidad temporal con los procesos actuales durante la migración.

---

## 3. Documentos electrónicos incluidos

La nueva arquitectura deberá soportar:

- Facturas.
- Liquidaciones de compra.
- Notas de crédito.
- Notas de débito.
- Comprobantes de retención.
- Guías de remisión.

Todos utilizarán un motor común de procesamiento, pero cada documento tendrá validadores, generadores XML y generadores RIDE especializados cuando corresponda.

---

## 4. Arquitectura objetivo

```text
              ERP / SISTEMA CONTABLE / SISTEMA EXTERNO
                              │
                              │ JSON
                              ▼
                  ┌───────────────────────┐
                  │ REST API SRI-FILES    │
                  │ /api/v1/documentos    │
                  └───────────┬───────────┘
                              │
                              ▼
                  ┌───────────────────────┐
                  │ PostgreSQL SRI-FILES  │
                  └───────────┬───────────┘
                              │
                              ▼
                  ┌───────────────────────┐
                  │ Motor Procesamiento   │
                  └───────────┬───────────┘
                              │
          ┌───────────────────┼────────────────────┐
          ▼                   ▼                    ▼
    Generador XML        Firma XAdES         Servicios SRI
                                                  │
                                                  ▼
                                            Autorización
                                                  │
                           ┌──────────────────────┼─────────────┐
                           ▼                      ▼             ▼
                    XML autorizado             RIDE          Historial
                           │                      │
                           └───────────┬──────────┘
                                       ▼
                                  Servicio Email
                                       │
                                       ▼
                                    FINALIZADO
```

---

## 5. FASE 0 — Estabilización del proyecto actual

Antes de modificar el flujo productivo se deberá crear una línea base estable.

### 5.1 Pruebas

Agregar al `pom.xml` la dependencia de pruebas correspondiente a Spring Boot y conseguir que:

```bash
./mvnw test
```

finalice correctamente.

Crear inicialmente pruebas para:

- carga del contexto;
- generación de clave de acceso;
- validaciones básicas;
- generación XML;
- servicios de firma encapsulados;
- interpretación de respuestas SRI.

### 5.2 Seguridad de configuración

Eliminar secretos reales del repositorio.

No versionar:

```text
.env
*.p12
*.pfx
certificados/
secrets/
```

Utilizar variables de entorno para:

```text
SPRING_DATASOURCE_URL
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD

SRI_AMBIENTE

EMAIL_MS_BASE_URL

JWT_SECRET

CERTIFICATE_PATH
CERTIFICATE_PASSWORD
```

### 5.3 Estado actual

Antes de retirar los códigos:

```text
I
P
A
O
C
N
M
```

documentar su significado y crear una tabla de equivalencias con los nuevos estados.

---

# 6. FASE 1 — Nueva base de datos

## 6.1 Entidades principales

Crear como mínimo:

```text
empresa
establecimiento
punto_emision
secuencial

documento_electronico
documento_estado_historial
documento_archivo
documento_error
documento_intento_sri
documento_correo

certificado_digital
configuracion_sri
configuracion_correo
plantilla_ride

usuario
rol
permiso
usuario_rol
rol_permiso

auditoria
```

---

## 6.2 Tabla documento_electronico

Entidad principal del sistema.

Campos sugeridos:

```text
id
uuid

tipo_documento
estado_actual

empresa_id
establecimiento_id
punto_emision_id

ambiente

secuencial
numero_documento
clave_acceso

fecha_emision

identificacion_receptor
razon_social_receptor
email_receptor

subtotal
impuestos
total

json_original

numero_autorizacion
fecha_autorizacion

mensaje_sri

fecha_recepcion
fecha_finalizacion

created_at
updated_at
```

`uuid` será el identificador público recomendado para APIs.

---

## 6.3 Historial de estados

Tabla:

```text
documento_estado_historial
```

Campos:

```text
id
documento_id
estado_anterior
estado_nuevo
descripcion
origen
usuario_id
fecha
```

Nunca depender exclusivamente de `estado_actual`.

`estado_actual` permitirá consultas rápidas y el historial permitirá reconstruir todo el procesamiento.

---

# 7. Estados del documento

Estados principales:

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
```

Estados de error:

```text
ERROR_VALIDACION

ERROR_XML

ERROR_FIRMA

DEVUELTO_SRI

NO_AUTORIZADO

ERROR_ENVIO_SRI

ERROR_AUTORIZACION

ERROR_RIDE

ERROR_CORREO
```

No utilizar un único estado `ERROR`.

El estado debe permitir identificar inmediatamente qué etapa falló.

---

# 8. Historial de procesamiento

Ejemplo:

```text
10:30:01 RECIBIDO
10:30:01 VALIDANDO
10:30:02 VALIDADO
10:30:02 XML_GENERADO
10:30:03 FIRMADO
10:30:04 ENVIANDO_SRI
10:30:05 RECIBIDO_SRI
10:30:05 PENDIENTE_AUTORIZACION
10:30:09 AUTORIZADO
10:30:10 RIDE_GENERADO
10:30:11 CORREO_PENDIENTE
10:30:13 CORREO_ENVIADO
10:30:13 FINALIZADO
```

---

# 9. FASE 2 — Refactorización backend

El controlador actual no deberá continuar concentrando el proceso completo.

Estructura propuesta:

```text
src/main/java/.../

controller/
    DocumentoController
    DashboardController
    ConfiguracionController
    EmpresaController
    UsuarioController

service/
    DocumentoService
    ProcesamientoDocumentoService
    XmlService
    FirmaElectronicaService
    SriRecepcionService
    SriAutorizacionService
    RideService
    CorreoDocumentoService
    ReintentoService
    AuditoriaService

processor/
    DocumentoProcessor
    FacturaProcessor
    LiquidacionCompraProcessor
    NotaCreditoProcessor
    NotaDebitoProcessor
    RetencionProcessor
    GuiaRemisionProcessor

validator/
    FacturaValidator
    LiquidacionValidator
    NotaCreditoValidator
    NotaDebitoValidator
    RetencionValidator
    GuiaRemisionValidator

xml/
    FacturaXmlGenerator
    LiquidacionXmlGenerator
    NotaCreditoXmlGenerator
    NotaDebitoXmlGenerator
    RetencionXmlGenerator
    GuiaRemisionXmlGenerator

repository/

entity/

dto/
    request/
    response/

security/

exception/

config/

scheduler/

audit/
```

---

# 10. Procesador común

Definir una abstracción común.

Ejemplo conceptual:

```java
public interface DocumentoProcessor<T> {

    void validar(T documento);

    String generarXml(T documento);

    String firmar(String xml);

    RecepcionSriResponse enviarSri(String xmlFirmado);

    AutorizacionSriResponse consultarAutorizacion(String claveAcceso);

    byte[] generarRide(String xmlAutorizado);
}
```

Cada tipo documental implementará únicamente su comportamiento especializado.

---

# 11. API principal

## Recepción

```http
POST /api/v1/documentos
```

El endpoint deberá:

1. autenticar al consumidor;
2. validar estructura mínima;
3. determinar el tipo documental;
4. verificar idempotencia;
5. registrar JSON original;
6. generar UUID;
7. registrar estado `RECIBIDO`;
8. encolar procesamiento;
9. responder sin esperar al SRI.

Respuesta recomendada:

```http
HTTP 202 Accepted
```

```json
{
  "id": "6b80a443-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
  "tipoDocumento": "FACTURA",
  "estado": "RECIBIDO",
  "mensaje": "Documento recibido para procesamiento"
}
```

---

# 12. Idempotencia

Este punto será obligatorio.

El ERP puede repetir una solicitud debido a:

- timeout;
- pérdida de conexión;
- reinicio;
- error HTTP;
- reintento manual.

Por ello deberá aceptarse una llave:

```http
Idempotency-Key
```

o un identificador externo:

```json
{
  "externalId": "ERP-FACTURA-348396"
}
```

No se deberá generar dos veces el mismo comprobante por un reintento de red.

---

# 13. APIs de consulta

Implementar:

```text
GET /api/v1/documentos

GET /api/v1/documentos/{uuid}

GET /api/v1/documentos/{uuid}/estado

GET /api/v1/documentos/{uuid}/historial

GET /api/v1/documentos/{uuid}/xml

GET /api/v1/documentos/{uuid}/xml-firmado

GET /api/v1/documentos/{uuid}/xml-autorizado

GET /api/v1/documentos/{uuid}/ride
```

Filtros:

```text
tipo
estado
fechaDesde
fechaHasta
identificacion
claveAcceso
numeroDocumento
establecimiento
puntoEmision
email
```

---

# 14. APIs operativas

Según permisos:

```text
POST /api/v1/documentos/{uuid}/reprocesar

POST /api/v1/documentos/{uuid}/reenviar-sri

POST /api/v1/documentos/{uuid}/consultar-autorizacion

POST /api/v1/documentos/{uuid}/regenerar-ride

POST /api/v1/documentos/{uuid}/reenviar-correo
```

Todas estas acciones deberán generar auditoría.

---

# 15. Contrato JSON

Crear un envelope común:

```json
{
  "tipoDocumento": "FACTURA",
  "externalId": "ERP-FACTURA-348396",
  "emisor": {},
  "receptor": {},
  "documento": {},
  "detalles": [],
  "impuestos": [],
  "informacionAdicional": {},
  "correo": {
    "enviar": true,
    "destinatarios": []
  }
}
```

El contrato específico deberá estar versionado.

Ejemplo:

```text
/api/v1/documentos
/api/v2/documentos
```

Nunca cambiar destructivamente un contrato utilizado por sistemas externos.

---

# 16. Validación

Separar tres niveles:

## Nivel 1 — JSON

- campos requeridos;
- formatos;
- tipos;
- longitudes.

## Nivel 2 — Reglas de negocio

Ejemplos:

- identificación válida;
- totales;
- impuestos;
- secuenciales;
- referencias documentales;
- fechas;
- establecimientos.

## Nivel 3 — XML SRI

Validar el XML contra el esquema correspondiente antes de firmarlo.

No enviar XML estructuralmente inválido al SRI.

---

# 17. Archivos

Administrar como mínimo:

```text
JSON_ORIGINAL

XML_GENERADO

XML_FIRMADO

XML_AUTORIZADO

RIDE_PDF
```

Tabla:

```text
documento_archivo
```

Campos sugeridos:

```text
id
documento_id
tipo
nombre
mime_type
ruta
hash_sha256
tamanio
fecha_creacion
```

La base puede almacenar metadatos y los archivos pueden mantenerse en almacenamiento configurable.

---

# 18. Firma electrónica

Encapsular completamente la implementación actual en:

```text
FirmaElectronicaService
```

Responsabilidades:

- cargar certificado;
- validar vigencia;
- firmar XML;
- detectar contraseña incorrecta;
- detectar certificado expirado;
- validar resultado;
- registrar error.

El frontend no deberá mostrar la contraseña almacenada.

---

# 19. Certificados

Crear módulo:

```text
Configuración
   └── Certificados digitales
```

Funciones:

- cargar certificado;
- indicar empresa;
- fecha inicio;
- fecha expiración;
- estado;
- verificar certificado;
- reemplazar;
- desactivar.

Mostrar alertas:

```text
CERTIFICADO EXPIRA EN 30 DÍAS
CERTIFICADO EXPIRA EN 15 DÍAS
CERTIFICADO EXPIRA EN 7 DÍAS
CERTIFICADO EXPIRADO
```

---

# 20. Integración SRI

Separar:

```text
SriRecepcionService

SriAutorizacionService
```

Registrar cada interacción.

Tabla:

```text
documento_intento_sri
```

Campos:

```text
id
documento_id
tipo_operacion
numero_intento
fecha_inicio
fecha_fin
resultado
codigo
mensaje
respuesta
```

---

# 21. Reintentos

No realizar reintentos infinitos.

Configuración ejemplo:

```text
Intento 1 → inmediato

Intento 2 → 30 segundos

Intento 3 → 1 minuto

Intento 4 → 5 minutos

Intento 5 → 15 minutos
```

Después:

```text
REQUIERE_INTERVENCION
```

Los errores funcionales del documento no deben reintentarse automáticamente.

---

# 22. Procesamiento asíncrono

La petición HTTP no deberá esperar todo el ciclo SRI.

Flujo:

```text
HTTP
 │
 ▼
Guardar documento
 │
 ▼
202 Accepted
 │
 ▼
Procesamiento asíncrono
```

Inicialmente puede implementarse con componentes Spring y persistencia PostgreSQL.

La arquitectura deberá permitir posteriormente incorporar un broker como RabbitMQ si el volumen lo requiere.

---

# 23. Recuperación ante reinicio

Nunca depender exclusivamente de memoria.

Si el contenedor se reinicia:

```text
PENDIENTE_AUTORIZACION
```

deberá continuar después del arranque.

Los schedulers deberán consultar la base de datos y recuperar procesos pendientes.

---

# 24. Correo electrónico

Crear una capa:

```text
CorreoDocumentoService
```

Registrar:

```text
destinatario
fecha_programada
fecha_envio
estado
numero_intentos
mensaje_error
```

Estados:

```text
PENDIENTE
ENVIANDO
ENVIADO
ERROR
```

Un fallo de correo no deberá invalidar un comprobante que ya está autorizado.

---

# 25. FASE 3 — Frontend administrativo

Crear una aplicación administrativa completa.

Menú:

```text
Dashboard

Documentos electrónicos
    Todos
    Facturas
    Liquidaciones de compra
    Notas de crédito
    Notas de débito
    Retenciones
    Guías de remisión

Procesamiento
    Pendientes
    En proceso
    Autorizados
    No autorizados
    Errores
    Reintentos

Correo
    Pendientes
    Enviados
    Errores

Configuración
    Empresa
    Establecimientos
    Puntos de emisión
    Secuenciales
    Certificados
    SRI
    Correo
    Logos
    Plantillas RIDE

Administración
    Usuarios
    Roles
    Permisos
    Auditoría

Monitoreo
    Servicios SRI
    Procesos pendientes
    Errores
    Logs funcionales
```

---

# 26. Dashboard

Tarjetas:

```text
Documentos hoy

Procesando

Autorizados

No autorizados

Con errores

Correos pendientes
```

Gráficos:

- documentos por día;
- documentos por tipo;
- documentos por estado;
- autorizados vs no autorizados;
- errores por etapa;
- tiempo promedio de autorización.

Filtros:

```text
Hoy
7 días
30 días
Mes
Rango personalizado
```

---

# 27. Bandeja de documentos

Columnas:

```text
Fecha
Tipo
Documento
Clave de acceso
Identificación
Cliente
Total
Estado
Correo
Acciones
```

Permitir:

- paginación;
- filtros;
- búsqueda;
- ordenamiento;
- exportación;
- acceso al detalle.

---

# 28. Detalle del comprobante

Crear pestañas:

```text
Resumen

JSON

XML generado

XML firmado

XML autorizado

RIDE

Respuesta SRI

Historial

Correo

Auditoría
```

Acciones condicionadas por permisos:

```text
Descargar XML

Descargar RIDE

Consultar SRI

Reprocesar

Regenerar RIDE

Reenviar correo
```

---

# 29. Visualización de estados

Usar badges claramente diferenciados:

```text
RECIBIDO
PROCESANDO
AUTORIZADO
FINALIZADO
NO AUTORIZADO
ERROR
```

Mostrar además la etapa exacta.

Ejemplo:

```text
ERROR
Firma electrónica
```

en lugar de únicamente:

```text
ERROR
```

---

# 30. Seguridad

Implementar:

```text
JWT
RBAC
```

Roles iniciales:

```text
SUPER_ADMIN

ADMIN

OPERADOR

CONSULTA

AUDITOR
```

Permisos independientes:

```text
DOCUMENTO_VER

DOCUMENTO_DESCARGAR

DOCUMENTO_REPROCESAR

DOCUMENTO_REENVIAR_SRI

DOCUMENTO_REENVIAR_CORREO

CONFIGURACION_VER

CONFIGURACION_EDITAR

CERTIFICADO_ADMINISTRAR

USUARIO_ADMINISTRAR

AUDITORIA_VER
```

---

# 31. Auditoría

Registrar:

```text
usuario
fecha
IP
acción
entidad
identificador
datos anteriores
datos posteriores
resultado
```

Ejemplos:

```text
DOCUMENTO_REPROCESADO

CORREO_REENVIADO

CERTIFICADO_REEMPLAZADO

CONFIGURACION_MODIFICADA

USUARIO_CREADO

ROL_MODIFICADO
```

---

# 32. Secuenciales

La plataforma deberá administrar:

```text
empresa
establecimiento
punto emisión
tipo documento
secuencial actual
```

La generación debe ser transaccional para impedir duplicados.

Aplicar bloqueo/control de concurrencia.

---

# 33. Logs

Diferenciar:

```text
LOG TÉCNICO
```

de:

```text
HISTORIAL FUNCIONAL
```

El administrador normalmente deberá consultar historial funcional.

Los logs técnicos se utilizarán para diagnóstico.

Nunca registrar:

- contraseñas;
- JWT completos;
- contraseña del certificado;
- secretos;
- información sensible innecesaria.

---

# 34. Manejo global de excepciones

Crear:

```text
GlobalExceptionHandler
```

Respuesta uniforme:

```json
{
  "timestamp": "2026-08-14T10:30:00",
  "status": 400,
  "code": "DOC_VALIDATION_ERROR",
  "message": "El documento contiene errores",
  "details": []
}
```

---

# 35. Observabilidad

Crear endpoint:

```text
/actuator/health
```

Supervisar:

```text
PostgreSQL

servicio recepción SRI

servicio autorización SRI

servicio de correo

almacenamiento

certificado
```

El dashboard podrá mostrar:

```text
SRI RECEPCIÓN        OPERATIVO
SRI AUTORIZACIÓN     OPERATIVO
BASE DE DATOS        OPERATIVO
CORREO               OPERATIVO
CERTIFICADO          VÁLIDO
```

---

# 36. Swagger / OpenAPI

Mantener Swagger y reorganizarlo por tags:

```text
Documentos

Facturas

Liquidaciones

Notas de crédito

Notas de débito

Retenciones

Guías de remisión

Configuración

Administración

Dashboard
```

Documentar ejemplos JSON reales de cada tipo documental.

---

# 37. FASE 4 — Migración

No reemplazar inmediatamente el flujo actual.

## Paso 1

Congelar y respaldar la versión productiva.

## Paso 2

Crear las nuevas tablas sin eliminar las actuales.

## Paso 3

Crear la nueva API `/api/v1`.

## Paso 4

Refactorizar los componentes actuales reutilizables:

```text
firma
SOAP SRI
autorización
JasperReports
correo
```

## Paso 5

Implementar primero FACTURA.

## Paso 6

Ejecutar pruebas paralelas.

Comparar:

```text
XML anterior == XML nuevo

firma válida

respuesta SRI

autorización

RIDE

correo
```

## Paso 7

Migrar RETENCIONES.

## Paso 8

Implementar:

```text
LIQUIDACIÓN DE COMPRA

NOTA DE CRÉDITO

NOTA DE DÉBITO

GUÍA DE REMISIÓN
```

## Paso 9

Implementar frontend completo.

## Paso 10

Cambiar progresivamente los consumidores.

## Paso 11

Mantener temporalmente endpoints legacy.

## Paso 12

Retirar código antiguo cuando no existan consumidores.

---

# 38. Orden recomendado de desarrollo

## Sprint / Etapa 0

Estabilización:

- pruebas;
- secretos;
- documentación;
- backup.

## Etapa 1

Base arquitectónica:

- modelo BD;
- entidades;
- repositorios;
- estados;
- historial;
- auditoría.

## Etapa 2

Motor común:

- recepción JSON;
- validación;
- persistencia;
- procesamiento;
- archivos;
- idempotencia.

## Etapa 3

Factura:

- JSON;
- XML;
- firma;
- SRI;
- autorización;
- RIDE;
- correo.

## Etapa 4

Frontend base:

- login;
- layout;
- dashboard;
- bandeja;
- detalle.

## Etapa 5

Retenciones.

## Etapa 6

Liquidaciones.

## Etapa 7

Notas de crédito.

## Etapa 8

Notas de débito.

## Etapa 9

Guías de remisión.

## Etapa 10

Administración:

- empresas;
- establecimientos;
- puntos de emisión;
- certificados;
- usuarios;
- roles;
- permisos.

## Etapa 11

Monitoreo, reintentos y auditoría avanzada.

## Etapa 12

Migración definitiva.

---

# 39. Criterios de aceptación generales

La reestructuración se considerará completa cuando:

- [ ] Un sistema externo pueda emitir un documento enviando solamente JSON.
- [ ] El documento quede almacenado antes de iniciar procesamiento.
- [ ] Cada cambio de estado tenga historial.
- [ ] Se genere XML conforme al tipo documental.
- [ ] El XML pueda firmarse correctamente.
- [ ] Se registre cada intento contra el SRI.
- [ ] Se recupere y almacene el XML autorizado.
- [ ] Se genere RIDE/PDF.
- [ ] Se gestione el correo independientemente de la autorización.
- [ ] Los errores puedan identificarse por etapa.
- [ ] Los procesos pendientes sobrevivan a reinicios.
- [ ] Exista control de reintentos.
- [ ] Exista idempotencia.
- [ ] Los documentos puedan consultarse desde el frontend.
- [ ] XML y RIDE puedan descargarse.
- [ ] Exista dashboard.
- [ ] Exista administración de usuarios, roles y permisos.
- [ ] Exista administración de certificados.
- [ ] Exista auditoría.
- [ ] Los seis tipos documentales estén soportados.
- [ ] Swagger documente la nueva API.
- [ ] Las pruebas automatizadas estén operativas.

---

# 40. Resultado esperado

Al finalizar, `sri-files` dejará de depender conceptualmente del ERP EPMAPA-T y se convertirá en un servicio independiente:

```text
             PLATAFORMA SRI-FILES
                    ▲
                    │
       ┌────────────┼────────────┐
       │            │            │
      ERP       FACTURACIÓN    OTROS
       │            │          SISTEMAS
       └────────────┼────────────┘
                    │
                   JSON
                    │
                    ▼
          ADMINISTRADOR SRI-FILES
                    │
                    ▼
                   SRI
```

El objetivo final es que cualquier aplicación autorizada pueda consumir la plataforma sin conocer los detalles internos de XML, XAdES, SOAP, autorización, JasperReports o correo.

---

# 41. Regla principal para la implementación

> **No destruir primero para construir después.**

La migración debe ser incremental.

Cada componente existente que ya funcione deberá:

1. identificarse;
2. aislarse;
3. cubrirse con pruebas;
4. encapsularse detrás de una interfaz;
5. reutilizarse en el nuevo flujo;
6. reemplazarse únicamente cuando exista una implementación nueva validada.

Esto reduce significativamente el riesgo de afectar la facturación electrónica productiva.

---

# ANEXO A — Estado de partida

El presente plan fue elaborado tomando como línea base el levantamiento técnico de `sri-files` del 14 de agosto de 2026. Entre los aspectos principales de ese levantamiento se encuentran: Spring Boot 3.4.3 y Java 17; integración SOAP con recepción y autorización SRI; PostgreSQL; JasperReports; firma XAdES; integración con un microservicio de correo; schedulers de procesamiento; Swagger/OpenAPI; compilación exitosa del artefacto con pruebas actualmente no operativas; alta concentración de lógica en `SRI_Controller`; y dependencia del ERP para parte del flujo actual.

El detalle original se conserva en `ESTADO_ACTUAL_SRI_FILES.md`.

---

# ANEXO B — Próximos documentos técnicos recomendados

Después de aprobar esta planificación se recomienda generar, en este orden:

1. `MODELO_BASE_DATOS_SRI_FILES.md`
2. `CONTRATOS_JSON_SRI_FILES.md`
3. `API_SRI_FILES_V1.md`
4. `ARQUITECTURA_BACKEND_SRI_FILES.md`
5. `ARQUITECTURA_FRONTEND_SRI_FILES.md`
6. `PLAN_MIGRACION_SRI_FILES.md`
7. scripts SQL/Flyway iniciales;
8. estructura base del backend;
9. estructura base del frontend.

Estos documentos permitirán convertir esta planificación general en tareas de programación concretas y verificables.
