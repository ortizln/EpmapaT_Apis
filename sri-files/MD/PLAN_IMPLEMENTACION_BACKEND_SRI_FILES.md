# PLAN DE IMPLEMENTACIÓN BACKEND — SRI-FILES V2

**Proyecto:** Reestructuración del motor de documentos electrónicos `sri-files`  
**Backend:** Java + Spring Boot  
**Base de datos:** PostgreSQL  
**API objetivo:** `/api/v1`  
**Estrategia:** Migración incremental sin romper el flujo productivo actual  
**Fecha:** 2026-08-14  
**Versión del plan:** 1.0

---

# 1. Objetivo

Convertir la arquitectura definida para `sri-files` en un plan de programación ejecutable, ordenado y verificable.

El resultado final será un backend capaz de recibir un JSON y administrar de forma independiente todo el ciclo:

```text
JSON
 ↓
REGISTRO EN BD
 ↓
VALIDACIÓN
 ↓
GENERACIÓN XML
 ↓
VALIDACIÓN XSD
 ↓
FIRMA ELECTRÓNICA
 ↓
ENVÍO SRI
 ↓
CONSULTA AUTORIZACIÓN
 ↓
XML AUTORIZADO
 ↓
RIDE / PDF
 ↓
CORREO
 ↓
FINALIZADO
```

Documentos soportados:

```text
01  FACTURA
03  LIQUIDACION_COMPRA
04  NOTA_CREDITO
05  NOTA_DEBITO
06  GUIA_REMISION
07  RETENCION
```

---

# 2. Documentos base de esta implementación

La programación deberá respetar conjuntamente:

```text
MODELO_BASE_DATOS_SRI_FILES.md
CONTRATOS_JSON_SRI_FILES.md
ARQUITECTURA_BACKEND_SRI_FILES.md
API_SRI_FILES_V1.md
```

Cada documento cumple una función:

| Documento | Responsabilidad |
|---|---|
| Modelo BD | Persistencia |
| Contratos JSON | Entrada documental |
| Arquitectura Backend | Organización del código |
| API V1 | Contrato HTTP |
| Este documento | Orden de implementación |

---

# 3. Principio principal

No realizar una reescritura completa de una sola vez.

La estrategia será:

```text
LEGACY FUNCIONANDO
       │
       ├──────────────┐
       │              │
       ▼              ▼
    LEGACY           V2
       │              │
       └──────┬───────┘
              ▼
         VALIDACIÓN
              │
              ▼
        MIGRACIÓN TOTAL
              │
              ▼
       RETIRO DEL LEGACY
```

Durante la implementación deberán coexistir temporalmente:

```text
/api/singsend/*
```

y:

```text
/api/v1/*
```

---

# 4. Reglas obligatorias de trabajo

1. No eliminar código productivo antes de tener reemplazo probado.
2. No modificar simultáneamente todas las clases legacy.
3. Crear primero interfaces alrededor de funcionalidades existentes.
4. Mantener commits pequeños.
5. Ejecutar pruebas después de cada fase.
6. No mezclar cambios funcionales con refactorizaciones masivas.
7. No introducir RabbitMQ inicialmente.
8. PostgreSQL será la fuente de verdad del procesamiento.
9. Todo documento tendrá historial de estados.
10. Todo error relevante quedará persistido.
11. Toda operación administrativa sensible será auditada.
12. No almacenar secretos en Git.
13. No editar documentos electrónicos ya procesados de forma silenciosa.

---

# 5. Estrategia Git

Crear:

```bash
git checkout -b feature/sri-files-v2
```

Subramas opcionales:

```text
feature/sri-v2-database
feature/sri-v2-core
feature/sri-v2-signature
feature/sri-v2-soap
feature/sri-v2-factura
feature/sri-v2-retencion
feature/sri-v2-dashboard
```

Formato recomendado de commits:

```text
feat(db): add electronic document schema

feat(core): add document state machine

refactor(signature): isolate XAdES signing service

refactor(sri): isolate reception SOAP adapter

feat(invoice): add factura processor

test(invoice): add factura contract fixtures
```

---

# 6. FASE 0 — RESPALDO Y DIAGNÓSTICO

## Objetivo

Congelar una referencia segura del sistema antes de modificarlo.

## Tareas

- [ ] Crear branch V2.
- [ ] Crear tag del estado actual.
- [ ] Respaldar `pom.xml`.
- [ ] Respaldar `application.properties/yml`.
- [ ] Respaldar WSDL.
- [ ] Respaldar XSD.
- [ ] Respaldar `.jrxml/.jasper`.
- [ ] Respaldar certificados solo fuera del repositorio.
- [ ] Identificar endpoints productivos.
- [ ] Identificar schedulers actuales.
- [ ] Identificar flujo de factura.
- [ ] Identificar flujo de retención.
- [ ] Identificar firma.
- [ ] Identificar recepción SRI.
- [ ] Identificar autorización.
- [ ] Identificar RIDE.
- [ ] Identificar correo.

## Entregable

```text
docs/legacy/INVENTARIO_LEGACY.md
```

---

# 7. Inventario mínimo legacy

Documentar:

```text
CLASE
MÉTODO
RESPONSABILIDAD
DEPENDENCIAS
UTILIZADO POR
REUTILIZABLE
REQUIERE REFACTOR
```

Ejemplo:

```text
SRI_Controller
 ├─ firmar
 ├─ enviar
 ├─ autorizar
 ├─ generarRide
 └─ enviarCorreo
```

El inventario deberá reflejar el código real encontrado; no asumir nombres que no existan.

---

# 8. Criterio FASE 0

No avanzar hasta poder responder:

```text
¿Cómo se genera actualmente una factura?

¿Cómo se firma?

¿Cómo se llama al SRI?

¿Cómo se obtiene autorización?

¿Cómo se genera RIDE?

¿Cómo se envía correo?

¿Qué tareas programadas existen?

¿Qué archivos se escriben?
```

---

# 9. FASE 1 — ESTABILIZAR BUILD

## Objetivo

Tener una base compilable y testeable.

## Archivos a modificar

```text
pom.xml
```

Agregar o verificar:

```text
spring-boot-starter-test
spring-boot-starter-validation
spring-boot-starter-data-jpa
postgresql
flyway-core
springdoc-openapi
spring-boot-starter-actuator
spring-security
```

Solo agregar dependencias que realmente se utilicen.

---

# 10. Prueba inicial

Ejecutar:

```bash
mvn clean test
```

y:

```bash
mvn clean package
```

Registrar fallos preexistentes antes de atribuirlos a V2.

---

# 11. Smoke test

Crear:

```text
src/test/java/.../SriFilesApplicationTests.java
```

Verificar que:

```text
Spring Context Loads
```

---

# 12. Criterio FASE 1

```text
mvn clean test = SUCCESS
mvn clean package = SUCCESS
```

o, si existen fallos legacy imposibles de corregir inmediatamente, deberán estar identificados y documentados.

---

# 13. FASE 2 — CONFIGURACIÓN SEGURA

## Objetivo

Eliminar configuración sensible y dispersa.

## Crear

```text
config/SriFilesProperties.java
config/ProcessingProperties.java
config/StorageProperties.java
```

## Modificar

```text
application.yml
application-dev.yml
application-test.yml
application-prod.yml
```

---

# 14. Variables externas

Mover a variables de entorno:

```text
DB_URL
DB_USERNAME
DB_PASSWORD

JWT_SECRET

CERTIFICATE_PASSWORD

EMAIL_SERVICE_URL

STORAGE_ROOT
```

No subir:

```text
.p12
.pfx
passwords
tokens
secrets
```

---

# 15. Configuración inicial

Ejemplo:

```yaml
sri-files:
  processing:
    enabled: true
    batch-size: 10
    fixed-delay: 2000
    max-retries: 5

  storage:
    root: ${STORAGE_ROOT:/data/sri-files}
```

---

# 16. Criterio FASE 2

La aplicación deberá iniciar utilizando configuración externa y perfiles sin modificar código Java.

---

# 17. FASE 3 — FLYWAY Y BASE DE DATOS

## Objetivo

Crear la nueva BD administrativa de documentos electrónicos.

## Crear

```text
src/main/resources/db/migration/
```

Migraciones recomendadas:

```text
V1__create_company_structure.sql
V2__create_document_structure.sql
V3__create_document_tracking.sql
V4__create_sri_configuration.sql
V5__create_security_structure.sql
V6__create_audit_structure.sql
V7__create_indexes.sql
```

La división exacta deberá seguir el modelo de BD aprobado.

---

# 18. Configuración JPA

Producción:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate

  flyway:
    enabled: true
```

No utilizar:

```text
ddl-auto=create
ddl-auto=update
```

como estrategia productiva.

---

# 19. Tablas prioritarias

Primero implementar:

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

usuario
rol
permiso
auditoria
```

---

# 20. Constraints críticos

Implementar:

```text
UNIQUE empresa + external_id

UNIQUE establecimiento + punto_emision + tipo_documento + secuencial

UNIQUE clave_acceso
```

según el modelo definitivo.

---

# 21. Pruebas FASE 3

Probar:

```text
BD vacía → Flyway crea esquema

BD creada → Flyway no repite migraciones

constraint externalId

constraint claveAcceso

foreign keys

índices
```

---

# 22. Criterio FASE 3

Una BD PostgreSQL vacía deberá quedar completamente preparada únicamente ejecutando la aplicación/Flyway.

---

# 23. FASE 4 — ENUMS Y ENTIDADES

## Crear enums

```text
TipoDocumento
EstadoDocumento
TipoArchivo
TipoOperacionSri
EstadoCorreo
AmbienteSri
```

## Crear entidades

```text
EmpresaEntity
EstablecimientoEntity
PuntoEmisionEntity
SecuencialEntity

DocumentoElectronicoEntity
DocumentoEstadoHistorialEntity
DocumentoArchivoEntity
DocumentoErrorEntity
DocumentoIntentoSriEntity
DocumentoCorreoEntity

CertificadoDigitalEntity
ConfiguracionSriEntity

UsuarioEntity
RolEntity
PermisoEntity
AuditoriaEntity
```

---

# 24. Regla JPA

No exponer entidades desde controllers.

No colocar lógica tributaria dentro de entidades.

Evitar relaciones bidireccionales innecesarias.

---

# 25. Repositories

Crear:

```text
EmpresaRepository
EstablecimientoRepository
PuntoEmisionRepository
SecuencialRepository

DocumentoElectronicoRepository
DocumentoEstadoHistorialRepository
DocumentoArchivoRepository
DocumentoErrorRepository
DocumentoIntentoSriRepository
DocumentoCorreoRepository

CertificadoDigitalRepository
ConfiguracionSriRepository
```

---

# 26. Queries prioritarias

```text
findByUuid

findByEmpresaAndExternalId

findByClaveAcceso

findByEstadoActual

findByEstadoActualIn

findByNumeroDocumento
```

No cargar archivos binarios completos al listar documentos.

---

# 27. Criterio FASE 4

Tests de repository deberán demostrar:

```text
crear documento
buscar externalId
buscar claveAcceso
guardar historial
guardar error
guardar intento SRI
```

---

# 28. FASE 5 — MÁQUINA DE ESTADOS

## Crear

```text
service/EstadoDocumentoService.java
```

## Estados

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

# 29. Implementar transición

Método:

```text
cambiarEstado(documento, nuevoEstado, descripción)
```

Debe:

```text
validar transición
actualizar documento
insertar historial
guardar timestamp
guardar contexto
```

---

# 30. Pruebas de estados

Ejemplos:

```text
RECIBIDO → VALIDANDO = OK

VALIDANDO → VALIDADO = OK

VALIDANDO → ERROR_VALIDACION = OK

RECIBIDO → AUTORIZADO = ERROR

AUTORIZADO → VALIDANDO = ERROR
```

---

# 31. Criterio FASE 5

Ningún módulo nuevo deberá modificar `estado_actual` directamente.

---

# 32. FASE 6 — STORAGE

## Crear

```text
storage/StorageService.java
storage/LocalStorageService.java
storage/StoragePathResolver.java
```

## Métodos

```text
save
read
exists
delete
```

---

# 33. Estructura física

```text
/data/sri-files/
└── 2026/
    └── 08/
        └── factura/
            └── {claveAcceso}/
                ├── generado.xml
                ├── firmado.xml
                ├── autorizado.xml
                └── ride.pdf
```

---

# 34. Registro de archivo

Después de guardar:

```text
tipo
ruta
mime
tamaño
SHA-256
fecha
```

en:

```text
documento_archivo
```

---

# 35. Pruebas Storage

```text
guardar
leer
hash
archivo inexistente
ruta segura
sobrescritura controlada
```

Evitar path traversal.

---

# 36. FASE 7 — DTOs Y RECEPCIÓN API

## Crear

```text
dto/request/
dto/response/
```

Implementar primero:

```text
FacturaRequest
```

junto con los DTO comunes:

```text
EmisorRequest
SecuencialRequest
ReceptorRequest
CorreoRequest
InformacionAdicionalRequest
```

---

# 37. Controller inicial

Crear:

```text
controller/DocumentoController.java
```

Implementar:

```text
POST /api/v1/documentos
GET /api/v1/documentos/{uuid}
GET /api/v1/documentos/{uuid}/estado
GET /api/v1/documentos/{uuid}/historial
```

---

# 38. DocumentoApplicationService

Crear:

```text
service/DocumentoApplicationService.java
```

`recibir()` deberá:

```text
validar empresa
validar establecimiento
validar punto emisión
validar externalId
verificar idempotencia
guardar JSON original
crear documento
estado RECIBIDO
commit
```

---

# 39. Idempotencia

Caso:

```text
POST documento A
POST documento A otra vez
```

Resultado:

```text
un solo registro documental
```

El segundo request devolverá el documento existente.

---

# 40. Pruebas FASE 7

```text
request válido → 202

request inválido → 400

empresa inexistente → error

punto emisión inexistente → error

externalId repetido → respuesta idempotente

GET uuid → documento

GET historial → RECIBIDO
```

---

# 41. Criterio FASE 7

Debe ser posible enviar un JSON de factura y verlo almacenado en BD como:

```text
RECIBIDO
```

sin ejecutar todavía SRI.

Este es el primer milestone funcional.

---

# 42. FASE 8 — EXTRAER FIRMA LEGACY

## Objetivo

Reutilizar la firma actual sin mantenerla acoplada al controlador.

## Crear

```text
signature/FirmaElectronicaService.java
signature/CertificateLoader.java
signature/CertificateValidator.java
```

---

# 43. Migración de firma

Mover/encapsular únicamente el código necesario.

Antes:

```text
Controller
  → código XAdES
```

Después:

```text
Controller legacy
          │
          ▼
FirmaElectronicaService
          ▲
          │
       Pipeline V2
```

Así ambos flujos usan temporalmente la misma implementación.

---

# 44. Tests firma

Usar certificado exclusivamente de pruebas.

Verificar:

```text
XML válido → XML firmado

certificado expirado → error

password incorrecto → error

XML inválido → error controlado
```

Nunca utilizar el certificado productivo en tests automatizados.

---

# 45. FASE 9 — EXTRAER RECEPCIÓN SRI

## Crear

```text
sri/port/SriRecepcionPort.java

sri/adapter/soap/SriRecepcionSoapAdapter.java
```

---

# 46. Adaptación

El adapter podrá usar las clases SOAP/WSDL existentes.

No retornar objetos SOAP fuera del adapter.

Transformar a:

```text
RecepcionSriResult
```

---

# 47. Tests recepción

Con mock:

```text
RECIBIDA
DEVUELTA
TIMEOUT
ERROR CONEXIÓN
```

No depender del SRI real en unit tests.

---

# 48. FASE 10 — EXTRAER AUTORIZACIÓN

Crear:

```text
SriAutorizacionPort
SriAutorizacionSoapAdapter
AutorizacionSriResult
```

Casos:

```text
AUTORIZADO
NO AUTORIZADO
PENDIENTE
TIMEOUT
ERROR
```

---

# 49. Regla crítica

Un timeout después de enviar al SRI no deberá provocar automáticamente un nuevo envío.

Primero deberá determinarse si corresponde:

```text
consultar autorización
```

o:

```text
reintentar recepción
```

según estado persistido.

---

# 50. FASE 11 — EXTRAER RIDE

## Crear

```text
ride/RideService.java
ride/JasperRideService.java
ride/RideTemplateResolver.java
```

Encapsular la lógica Jasper actual.

---

# 51. Tests RIDE

```text
XML autorizado válido → PDF

plantilla inexistente → error

logo inexistente → fallback

XML inválido → error
```

---

# 52. FASE 12 — EXTRAER CORREO

## Crear

```text
mail/CorreoDocumentoService.java
mail/EmailClient.java
mail/EmailMicroserviceClient.java
```

Encapsular la integración existente con correo.

---

# 53. Tests correo

```text
envío correcto

timeout

HTTP error

destinatario inválido

reintento
```

El fallo de correo nunca cambia un documento autorizado a no autorizado.

---

# 54. MILESTONE 2

Al finalizar fases 8–12 deberán existir servicios independientes para:

```text
FIRMA
RECEPCIÓN
AUTORIZACIÓN
RIDE
CORREO
```

sin depender directamente del controlador V2.

---

# 55. FASE 13 — VALIDACIÓN DE FACTURA

## Crear

```text
validation/factura/FacturaValidator.java
```

Validar:

```text
fecha emisión
receptor
detalles
cantidad
precio
descuento
impuestos
pagos
totales
```

---

# 56. BigDecimal

Todos los cálculos monetarios:

```java
BigDecimal
```

Nunca:

```java
float
double
```

Definir una política única de:

```text
escala
redondeo
comparación
```

---

# 57. Tests factura validator

Fixtures:

```text
factura-minima.json
factura-completa.json
factura-sin-receptor.json
factura-sin-detalles.json
factura-total-incorrecto.json
factura-impuesto-invalido.json
```

---

# 58. FASE 14 — GENERADOR XML FACTURA

## Crear

```text
xml/factura/FacturaXmlGenerator.java
```

Entrada:

```text
FacturaRequest
+
configuración empresa
+
secuencial
+
clave acceso
```

Salida:

```text
XML sin firmar
```

---

# 59. No copiar JSON directamente

El generador deberá transformar:

```text
DTO
 ↓
modelo tributario
 ↓
XML
```

No concatenar XML manualmente si ya existe una estrategia JAXB fiable y mantenible.

---

# 60. XmlValidationService

Crear:

```text
xml/XmlValidationService.java
```

Ejecutar:

```text
XML → XSD
```

antes de firma.

---

# 61. Golden master

Tomar facturas válidas conocidas y comparar:

```text
XML LEGACY
vs
XML V2
```

No exigir igualdad textual si existen diferencias irrelevantes de serialización.

Comparar semánticamente:

```text
campos
valores
totales
clave
estructura
XSD
```

---

# 62. FASE 15 — CLAVE DE ACCESO

Crear:

```text
util/ClaveAccesoGenerator.java
util/Modulo11.java
```

Tests obligatorios con claves conocidas.

No duplicar este cálculo dentro de cada generador XML.

---

# 63. FASE 16 — SECUENCIALES

Crear:

```text
service/SecuencialService.java
```

Debe soportar:

```text
AUTO
EXTERNO
```

---

# 64. AUTO

Usar locking transaccional.

Prueba de concurrencia:

```text
100 solicitudes simultáneas
```

deben producir:

```text
100 secuenciales únicos
```

---

# 65. EXTERNO

Validar:

```text
formato
rango
duplicidad
punto emisión
tipo documento
```

---

# 66. FASE 17 — FACTURA PROCESSOR

Crear:

```text
processor/DocumentoProcessor.java
processor/DocumentoProcessorFactory.java
processor/factura/FacturaProcessor.java
```

---

# 67. FacturaProcessor

Responsable de operaciones específicas de factura:

```text
validar
generar XML
generar RIDE específico
```

No deberá implementar SOAP directamente.

---

# 68. Registrar processor

```text
FACTURA → FacturaProcessor
```

en:

```text
DocumentoProcessorFactory
```

---

# 69. FASE 18 — MOTOR DE PROCESAMIENTO

Crear:

```text
processing/DocumentoProcessingService.java
```

Flujo:

```text
RECIBIDO

→ VALIDANDO
→ VALIDADO
→ XML_GENERADO
→ FIRMADO
→ ENVIANDO_SRI
→ RECIBIDO_SRI
→ PENDIENTE_AUTORIZACION
→ AUTORIZADO
→ RIDE_GENERADO
→ CORREO_PENDIENTE
→ CORREO_ENVIADO
→ FINALIZADO
```

---

# 70. Persistencia entre etapas

Después de cada operación importante:

```text
guardar resultado
cambiar estado
commit
```

No mantener una transacción abierta durante todo el proceso.

---

# 71. FASE 19 — WORKER

Crear:

```text
processing/DocumentoWorker.java
processing/DocumentoClaimService.java
scheduler/DocumentoPendingScheduler.java
```

---

# 72. Claim seguro

Usar estrategia equivalente a:

```sql
FOR UPDATE SKIP LOCKED
```

para evitar procesamiento duplicado.

---

# 73. Primera configuración

```text
batch-size = 10
delay = 2 segundos
```

Estos valores serán configurables.

---

# 74. Tests worker

```text
1 worker procesa documento

2 workers no procesan el mismo documento

fallo reiniciable conserva estado

reinicio aplicación recupera pendientes
```

---

# 75. FASE 20 — AUTORIZACIÓN ASÍNCRONA

Crear:

```text
scheduler/AutorizacionPendingScheduler.java
```

Procesar:

```text
PENDIENTE_AUTORIZACION
```

sin bloquear workers iniciales innecesariamente.

---

# 76. FASE 21 — RETRIES

Crear:

```text
processing/RetryPolicyService.java
```

Clasificar:

```text
RECUPERABLE
NO_RECUPERABLE
```

---

# 77. Backoff

Ejemplo configurable:

```text
1 → inmediato
2 → 30 s
3 → 1 min
4 → 5 min
5 → 15 min
```

Al exceder:

```text
REQUIERE_INTERVENCION
```

---

# 78. FASE 22 — RECOVERY

Crear:

```text
processing/RecoveryService.java
scheduler/RecoveryScheduler.java
```

Buscar estados transitorios abandonados.

Ejemplo:

```text
ENVIANDO_SRI durante 30 minutos
```

requiere análisis seguro.

No reenviar indiscriminadamente.

---

# 79. MILESTONE 3 — FACTURA END-TO-END

Probar:

```text
POST JSON factura
 ↓
RECIBIDO
 ↓
XML
 ↓
FIRMA
 ↓
SRI CERTIFICACIÓN
 ↓
AUTORIZACIÓN
 ↓
RIDE
 ↓
CORREO
 ↓
FINALIZADO
```

---

# 80. Evidencias requeridas

Guardar:

```text
JSON
XML generado
XML firmado
respuesta recepción
XML autorizado
RIDE
registro correo
historial BD
```

Comparar contra flujo legacy.

---

# 81. No avanzar a otros documentos

Hasta que factura complete satisfactoriamente:

```text
tests
XSD
firma
certificación SRI
RIDE
correo
recuperación
```

---

# 82. FASE 23 — RETENCIÓN

Crear:

```text
dto/request/retencion/*
validation/retencion/RetencionValidator
xml/retencion/RetencionXmlGenerator
processor/retencion/RetencionProcessor
```

---

# 83. Retención tests

Incluir:

```text
un documento sustento

múltiples documentos sustento

múltiples retenciones

códigos inválidos

bases incorrectas

retención completa
```

---

# 84. MILESTONE 4

```text
FACTURA + RETENCIÓN
```

funcionando completamente por V2.

---

# 85. FASE 24 — NOTA DE CRÉDITO

Crear:

```text
NotaCreditoRequest
NotaCreditoValidator
NotaCreditoXmlGenerator
NotaCreditoProcessor
```

Validar referencia al documento modificado.

---

# 86. FASE 25 — NOTA DE DÉBITO

Crear:

```text
NotaDebitoRequest
NotaDebitoValidator
NotaDebitoXmlGenerator
NotaDebitoProcessor
```

Validar motivos e impuestos.

---

# 87. FASE 26 — LIQUIDACIÓN DE COMPRA

Crear:

```text
LiquidacionCompraRequest
LiquidacionCompraValidator
LiquidacionCompraXmlGenerator
LiquidacionCompraProcessor
```

---

# 88. FASE 27 — GUÍA DE REMISIÓN

Crear:

```text
GuiaRemisionRequest
GuiaRemisionValidator
GuiaRemisionXmlGenerator
GuiaRemisionProcessor
```

Validar:

```text
transportista
placa
fechas
destinatarios
detalles
documento sustento
```

---

# 89. MILESTONE 5 — SEIS DOCUMENTOS

El pipeline deberá soportar:

```text
FACTURA
LIQUIDACION_COMPRA
NOTA_CREDITO
NOTA_DEBITO
RETENCION
GUIA_REMISION
```

sin crear seis pipelines independientes.

---

# 90. FASE 28 — CONSULTAS Y BANDEJA

Implementar:

```text
GET /api/v1/documentos
GET /api/v1/documentos/{uuid}
GET /api/v1/documentos/{uuid}/estado
GET /api/v1/documentos/{uuid}/historial
GET /api/v1/documentos/{uuid}/errores
GET /api/v1/documentos/{uuid}/intentos-sri
```

---

# 91. Filtros

Implementar:

```text
tipo
estado
fechaDesde
fechaHasta
identificacion
numeroDocumento
claveAcceso
externalId
establecimiento
puntoEmision
page
size
sort
```

---

# 92. Índices

Revisar `EXPLAIN ANALYZE` para consultas principales antes de crear índices adicionales indiscriminadamente.

---

# 93. FASE 29 — DESCARGAS

Implementar:

```text
GET /documentos/{uuid}/xml
GET /documentos/{uuid}/xml-firmado
GET /documentos/{uuid}/xml-autorizado
GET /documentos/{uuid}/ride
```

No exponer rutas físicas.

---

# 94. FASE 30 — OPERACIONES ADMINISTRATIVAS

Implementar:

```text
POST /documentos/{uuid}/reprocesar

POST /documentos/{uuid}/consultar-autorizacion

POST /documentos/{uuid}/regenerar-ride

POST /documentos/{uuid}/reenviar-correo
```

---

# 95. Reprocesamiento por etapa

Ejemplo:

```text
ERROR_CORREO
```

deberá ejecutar:

```text
CORREO
```

y no:

```text
XML → FIRMA → SRI
```

---

# 96. Tests de operaciones

Crear tests para cada estado permitido/no permitido.

---

# 97. FASE 31 — DASHBOARD

Crear:

```text
DashboardController
DashboardService
```

Endpoints:

```text
/resumen
/documentos-por-tipo
/documentos-por-estado
/documentos-por-dia
/errores-por-etapa
/tiempos
```

---

# 98. Consultas dashboard

No cargar documentos completos para calcular estadísticas.

Usar agregaciones SQL/JPA específicas.

---

# 99. FASE 32 — EMPRESA

Implementar CRUD controlado:

```text
empresa
establecimiento
punto_emision
```

No permitir borrado físico si existen documentos asociados.

---

# 100. FASE 33 — CERTIFICADOS

Implementar:

```text
listar
cargar
verificar
activar
desactivar
```

No devolver contraseña.

---

# 101. Alerta de expiración

Preparar consulta para:

```text
certificados que expiran en N días
```

Esto podrá mostrarse posteriormente en dashboard.

---

# 102. FASE 34 — CONFIGURACIÓN SRI

Administrar:

```text
ambiente
timeouts
reintentos
versiones documentales
estado
```

No permitir que un usuario sin permiso técnico modifique configuración crítica.

---

# 103. FASE 35 — CONFIGURACIÓN CORREO

Administrar:

```text
remitente
nombre
adjuntos
asunto
plantilla
```

---

# 104. FASE 36 — RECURSOS Y LOGOS

Implementar almacenamiento de:

```text
logo principal
logo secundario
marca de agua
```

Los RIDE deberán resolverlos por empresa.

---

# 105. FASE 37 — SEGURIDAD

Implementar:

```text
SecurityConfig
JwtAuthenticationFilter
JwtService
UserDetailsServiceImpl
PermissionService
```

---

# 106. Roles iniciales

```text
SUPER_ADMIN
ADMIN
OPERADOR
CONSULTA
AUDITOR
```

---

# 107. Permisos

Implementar permisos finos definidos en API V1.

Ejemplo:

```text
DOCUMENTO_VER
DOCUMENTO_DESCARGAR
DOCUMENTO_REPROCESAR
CERTIFICADO_ADMINISTRAR
AUDITORIA_VER
```

---

# 108. Tests seguridad

```text
sin token → 401

token sin permiso → 403

token correcto → 200/202

usuario inactivo → acceso denegado
```

---

# 109. FASE 38 — AUDITORÍA

Crear:

```text
AuditoriaService
AuditAspect
```

Registrar:

```text
usuario
acción
entidad
id entidad
fecha
IP
requestId
resultado
datos relevantes
```

---

# 110. Acciones obligatoriamente auditadas

```text
reprocesar
consultar manualmente
regenerar RIDE
reenviar correo
cargar certificado
cambiar secuencial
cambiar configuración
crear usuario
modificar rol
```

---

# 111. FASE 39 — OPENAPI

Documentar todos los endpoints.

Agregar ejemplos de:

```text
factura
retención
errores
paginación
operaciones
```

---

# 112. FASE 40 — ACTUATOR Y MONITOREO

Implementar:

```text
health
info
```

y endpoint funcional:

```text
/api/v1/monitoring/status
```

---

# 113. FASE 41 — LOGGING

Crear correlación:

```text
X-Request-Id
```

Agregar a MDC:

```text
requestId
documentId
externalId
claveAcceso
```

---

# 114. No registrar

```text
passwords
secretos
JWT completo
password certificado
```

---

# 115. FASE 42 — DOCKER

Actualizar:

```text
Dockerfile
docker-compose.yml
```

Configurar:

```text
PostgreSQL
sri-files
volumen documentos
variables ambiente
healthcheck
```

---

# 116. Volumen

```text
sri_files_data:/data/sri-files
```

Los XML/PDF no deberán desaparecer al recrear contenedor.

---

# 117. FASE 43 — GRACEFUL SHUTDOWN

Configurar:

```yaml
server:
  shutdown: graceful
```

y recuperación de documentos interrumpidos.

---

# 118. FASE 44 — TESTS DE CONCURRENCIA

Probar:

```text
secuenciales
workers
idempotencia
múltiples requests
```

Casos mínimos:

```text
100 requests mismo externalId → 1 documento

100 requests AUTO → 100 secuenciales únicos

2 workers → ningún documento duplicado
```

---

# 119. FASE 45 — PRUEBAS DE FALLO

Simular:

```text
SRI offline

timeout recepción

timeout autorización

reinicio después de firma

reinicio después de recepción

storage lleno

correo caído

certificado expirado
```

Verificar recuperación.

---

# 120. FASE 46 — PRUEBAS SRI CERTIFICACIÓN

Para cada tipo documental:

```text
JSON
XML
XSD
firma
recepción
autorización
RIDE
```

Guardar evidencias.

---

# 121. Matriz de pruebas

Crear:

```text
docs/testing/MATRIZ_PRUEBAS_SRI_V2.md
```

Columnas:

```text
ID
TIPO DOCUMENTO
CASO
JSON
RESULTADO ESPERADO
RESULTADO SRI
AUTORIZACIÓN
RIDE
ESTADO
OBSERVACIÓN
```

---

# 122. FASE 47 — MIGRACIÓN DE CONSUMIDORES

Identificar sistemas que llaman:

```text
/api/singsend/*
```

Migrarlos progresivamente a:

```text
/api/v1/documentos
```

---

# 123. Estrategia consumidor

Por cada consumidor:

```text
1. identificar endpoint legacy
2. construir JSON V1
3. probar certificación
4. activar V1
5. monitorear
6. retirar llamada legacy
```

---

# 124. FASE 48 — ADAPTADOR LEGACY

Cuando sea conveniente, convertir endpoints legacy en fachadas que llamen internamente a V2.

Así se evita mantener dos motores documentales.

---

# 125. FASE 49 — PERÍODO DE ESTABILIZACIÓN

Durante un período definido por el equipo:

Comparar:

```text
volumen
autorizaciones
errores
tiempos
duplicados
correos
```

---

# 126. FASE 50 — RETIRO LEGACY

Solo cuando:

- [ ] todos los consumidores críticos usan V2;
- [ ] los seis documentos están probados;
- [ ] no existen schedulers legacy necesarios;
- [ ] no existen dependencias productivas ocultas;
- [ ] existe respaldo;
- [ ] existe plan rollback.

Entonces:

```text
deprecated → disabled → removed
```

---

# 127. Plan de rollback

Antes del despliegue productivo:

```text
imagen Docker anterior
tag Git anterior
backup BD
configuración anterior
procedimiento para detener workers V2
```

No intentar revertir migraciones destructivas sin estrategia.

Las primeras migraciones deberán ser preferentemente aditivas.

---

# 128. Orden resumido obligatorio

```text
01 Diagnóstico
02 Build
03 Config
04 Flyway
05 Entidades
06 Estados
07 Storage
08 API recepción
09 Firma
10 Recepción SRI
11 Autorización
12 RIDE
13 Correo
14 Validación factura
15 XML factura
16 Clave acceso
17 Secuenciales
18 Processor factura
19 Processing service
20 Worker
21 Scheduler autorización
22 Retry
23 Recovery
24 Factura E2E
25 Retención
26 Nota crédito
27 Nota débito
28 Liquidación
29 Guía remisión
30 Consultas
31 Descargas
32 Operaciones
33 Dashboard
34 Administración
35 Certificados
36 Configuración
37 Seguridad
38 Auditoría
39 OpenAPI
40 Monitoring
41 Docker
42 Concurrencia
43 Fallos
44 Certificación
45 Migración consumidores
46 Estabilización
47 Retiro legacy
```

---

# 129. Prioridad funcional

## P0 — CRÍTICO

```text
BD
estados
idempotencia
factura
firma
SRI
autorización
storage
recovery
```

## P1 — ALTO

```text
RIDE
correo
retención
errores
historial
descargas
```

## P2 — MEDIO

```text
otros documentos
dashboard
configuración
certificados
```

## P3 — ADMINISTRATIVO

```text
usuarios
roles
auditoría
monitoring
exportaciones
```

---

# 130. Dependencias entre tareas

```text
BASE DATOS
    ↓
ENTIDADES
    ↓
ESTADOS
    ↓
RECEPCIÓN API
    ↓
PROCESSING
```

y:

```text
FACTURA DTO
    ↓
VALIDATOR
    ↓
XML
    ↓
XSD
    ↓
PROCESSOR
```

y:

```text
FIRMA
SRI RECEPCIÓN
SRI AUTORIZACIÓN
RIDE
CORREO
     ↓
PROCESSING SERVICE
```

---

# 131. Definition of Done por tarea

Una tarea no estará terminada solo porque compile.

Debe cumplir:

- [ ] código implementado;
- [ ] compilación correcta;
- [ ] test correspondiente;
- [ ] manejo de error;
- [ ] logging;
- [ ] sin secretos;
- [ ] sin duplicar lógica;
- [ ] documentación mínima;
- [ ] integración verificada;
- [ ] criterio funcional aprobado.

---

# 132. Definition of Done por documento electrónico

Ejemplo FACTURA:

- [ ] contrato JSON;
- [ ] DTO;
- [ ] validator;
- [ ] cálculos;
- [ ] clave acceso;
- [ ] XML;
- [ ] XSD;
- [ ] firma;
- [ ] recepción;
- [ ] autorización;
- [ ] XML autorizado;
- [ ] RIDE;
- [ ] correo;
- [ ] estados;
- [ ] errores;
- [ ] fixtures;
- [ ] unit tests;
- [ ] integration tests;
- [ ] prueba SRI certificación.

---

# 133. Estructura final esperada

```text
sri-files/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── .../srifiles/
│   │   │       ├── config/
│   │   │       ├── controller/
│   │   │       ├── dto/
│   │   │       ├── domain/
│   │   │       ├── entity/
│   │   │       ├── repository/
│   │   │       ├── mapper/
│   │   │       ├── service/
│   │   │       ├── processor/
│   │   │       ├── validation/
│   │   │       ├── xml/
│   │   │       ├── signature/
│   │   │       ├── sri/
│   │   │       ├── ride/
│   │   │       ├── mail/
│   │   │       ├── storage/
│   │   │       ├── processing/
│   │   │       ├── scheduler/
│   │   │       ├── security/
│   │   │       ├── audit/
│   │   │       └── exception/
│   │   │
│   │   └── resources/
│   │       ├── db/migration/
│   │       ├── xsd/
│   │       ├── reports/
│   │       └── application.yml
│   │
│   └── test/
│       ├── java/
│       └── resources/
│           └── contracts/
│
├── docs/
│   ├── legacy/
│   └── testing/
│
├── Dockerfile
├── docker-compose.yml
└── pom.xml
```

---

# 134. Primer sprint técnico recomendado

El primer bloque de implementación deberá detenerse deliberadamente antes de integrar el SRI.

Objetivo:

```text
JSON FACTURA
   ↓
POST /api/v1/documentos
   ↓
VALIDACIÓN DE CONTEXTO
   ↓
POSTGRESQL
   ↓
RECIBIDO
   ↓
HISTORIAL
   ↓
202
```

Tareas:

```text
1. Branch
2. Build estable
3. Flyway
4. Tablas principales
5. Entities
6. Repositories
7. Enums
8. EstadoDocumentoService
9. DTO factura
10. DocumentoApplicationService
11. DocumentoController
12. Idempotencia
13. GET detalle
14. GET estado
15. GET historial
16. Tests
```

---

# 135. Segundo sprint técnico recomendado

Objetivo:

```text
RECIBIDO
 ↓
VALIDADO
 ↓
XML_GENERADO
 ↓
XSD_VALIDADO
 ↓
FIRMADO
```

Tareas:

```text
FacturaValidator
ClaveAccesoGenerator
SecuencialService
FacturaXmlGenerator
XmlValidationService
StorageService
FirmaElectronicaService
```

---

# 136. Tercer sprint técnico recomendado

Objetivo:

```text
FIRMADO
 ↓
SRI RECEPCIÓN
 ↓
AUTORIZACIÓN
 ↓
XML AUTORIZADO
```

Tareas:

```text
SriRecepcionPort
SriRecepcionSoapAdapter
SriAutorizacionPort
SriAutorizacionSoapAdapter
DocumentoIntentoSri
retry
pending authorization
```

---

# 137. Cuarto sprint técnico recomendado

Objetivo:

```text
AUTORIZADO
 ↓
RIDE
 ↓
CORREO
 ↓
FINALIZADO
```

Tareas:

```text
JasperRideService
Storage PDF
EmailClient
DocumentoCorreo
retry correo
descargas
```

---

# 138. Quinto sprint

Objetivo:

```text
FACTURA PRODUCTIVAMENTE ESTABLE
+
RETENCIÓN
```

No iniciar simultáneamente los otros cuatro documentos si factura todavía presenta problemas de arquitectura.

---

# 139. Lista de archivos iniciales a crear

```text
config/SriFilesProperties.java

domain/enums/TipoDocumento.java
domain/enums/EstadoDocumento.java
domain/enums/TipoArchivo.java

entity/EmpresaEntity.java
entity/EstablecimientoEntity.java
entity/PuntoEmisionEntity.java
entity/SecuencialEntity.java
entity/DocumentoElectronicoEntity.java
entity/DocumentoEstadoHistorialEntity.java

repository/EmpresaRepository.java
repository/EstablecimientoRepository.java
repository/PuntoEmisionRepository.java
repository/SecuencialRepository.java
repository/DocumentoElectronicoRepository.java
repository/DocumentoEstadoHistorialRepository.java

service/EstadoDocumentoService.java
service/DocumentoApplicationService.java
service/DocumentoConsultaService.java

controller/DocumentoController.java

exception/GlobalExceptionHandler.java
exception/BusinessException.java

dto/request/factura/FacturaRequest.java

dto/response/DocumentoAceptadoResponse.java
dto/response/DocumentoDetalleResponse.java
dto/response/DocumentoEstadoResponse.java
```

---

# 140. Primer endpoint a completar

```text
POST /api/v1/documentos
```

Debe estar terminado antes de crear dashboard o administración visual.

Prueba:

```bash
curl -X POST \
  http://localhost:9090/api/v1/documentos \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: TEST-FACTURA-001" \
  -d @factura-minima.json
```

Resultado esperado:

```text
HTTP 202
```

con UUID persistido.

---

# 141. Segunda prueba

Enviar exactamente la misma petición.

Resultado:

```text
NO crear otro documento
```

y devolver el documento previamente registrado.

---

# 142. Tercera prueba

```text
GET /api/v1/documentos/{uuid}/historial
```

Resultado mínimo:

```text
RECIBIDO
```

---

# 143. Cuarta prueba

Reiniciar aplicación.

Consultar nuevamente UUID.

El documento deberá continuar existiendo.

Esto confirma que el procesamiento no depende de memoria temporal.

---

# 144. Regla para agentes de programación

Si este documento se utiliza con un agente IA:

```text
NO implementar todas las fases en una sola solicitud.
```

Entregar una fase o grupo pequeño de tareas.

Ejemplo:

```text
"Implementa únicamente FASE 3 y FASE 4.
No modifiques controladores legacy.
Al finalizar ejecuta mvn test y entrega listado
de archivos creados/modificados."
```

---

# 145. Plantilla de instrucción para cada fase

```text
Analiza primero el código actual relacionado con esta fase.

Implementa únicamente:
[FASE X]

Respeta:
- MODELO_BASE_DATOS_SRI_FILES.md
- CONTRATOS_JSON_SRI_FILES.md
- ARQUITECTURA_BACKEND_SRI_FILES.md
- API_SRI_FILES_V1.md
- PLAN_IMPLEMENTACION_BACKEND_SRI_FILES.md

Restricciones:
- No eliminar código legacy.
- No modificar endpoints productivos salvo indicación.
- No introducir dependencias innecesarias.
- Mantener compatibilidad.
- Crear pruebas.
- Ejecutar mvn test.

Al finalizar informa:
1. archivos creados;
2. archivos modificados;
3. migraciones;
4. pruebas ejecutadas;
5. problemas encontrados;
6. siguiente tarea recomendada.
```

---

# 146. Checklist antes de producción

## Base de datos

- [ ] Backup.
- [ ] Flyway probado.
- [ ] Constraints.
- [ ] Índices.
- [ ] Rollback operativo.

## Documentos

- [ ] Seis tipos validados.
- [ ] XSD.
- [ ] Certificación SRI.
- [ ] Secuenciales.

## Firma

- [ ] Certificado válido.
- [ ] Password seguro.
- [ ] Fecha expiración monitoreada.

## SRI

- [ ] Ambiente correcto.
- [ ] URLs correctas.
- [ ] Timeouts.
- [ ] Retries.
- [ ] Prevención duplicidad.

## Storage

- [ ] Volumen persistente.
- [ ] Permisos.
- [ ] Espacio.
- [ ] Backup.

## Correo

- [ ] Servicio disponible.
- [ ] Remitente.
- [ ] Adjuntos.
- [ ] Retry.

## Seguridad

- [ ] JWT.
- [ ] Roles.
- [ ] Permisos.
- [ ] Secrets.
- [ ] CORS.

## Operación

- [ ] Dashboard.
- [ ] Historial.
- [ ] Errores.
- [ ] Auditoría.
- [ ] Health.

---

# 147. Resultado final esperado

El backend dejará de funcionar conceptualmente como:

```text
Endpoint
  ↓
procedimiento enorme
  ↓
respuesta
```

y pasará a:

```text
API
 ↓
DOCUMENTO
 ↓
BD / ESTADO
 ↓
WORKER
 ↓
PROCESSOR
 ↓
SERVICIOS ESPECIALIZADOS
 ↓
SRI
 ↓
RESULTADOS PERSISTIDOS
```

---

# 148. Resultado para el frontend

Cuando este plan alcance las fases administrativas, el frontend podrá construir:

```text
Dashboard

Bandeja de documentos

Factura

Liquidaciones

Notas de crédito

Notas de débito

Retenciones

Guías

Detalle documental

Historial

Errores

XML / RIDE

Reprocesamiento

Empresas

Establecimientos

Puntos de emisión

Certificados

Configuración

Usuarios

Roles

Auditoría

Monitoreo
```

sin depender de procesos internos del SRI.

---

# 149. Siguiente etapa del proyecto

Una vez iniciada la implementación del backend, el documento complementario recomendado será:

```text
ARQUITECTURA_FRONTEND_SRI_FILES.md
```

para definir:

```text
Angular
layout
dashboard
bandejas
detalle documental
timeline de estados
visor XML
descargas
errores
reprocesamiento
empresas
certificados
usuarios
roles
auditoría
guards
interceptors
servicios API
modelos TypeScript
rutas
componentes
responsive
```

Después:

```text
PLAN_IMPLEMENTACION_FRONTEND_SRI_FILES.md
```

---

# 150. Conclusión

Este plan convierte la reestructuración de `sri-files` en una migración incremental y controlada.

La prioridad no es desarrollar primero todas las pantallas ni todos los comprobantes.

El orden correcto es:

```text
PERSISTENCIA
    ↓
TRAZABILIDAD
    ↓
FACTURA
    ↓
PIPELINE
    ↓
SRI
    ↓
RECUPERACIÓN
    ↓
RETENCIÓN
    ↓
RESTO DOCUMENTOS
    ↓
ADMINISTRACIÓN
    ↓
FRONTEND
```

La primera meta real debe ser sencilla y verificable:

```text
UN JSON DE FACTURA
        ↓
UN DOCUMENTO PERSISTIDO
        ↓
UN UUID
        ↓
UN HISTORIAL
        ↓
UN PROCESAMIENTO RECUPERABLE
        ↓
UN XML AUTORIZADO
        ↓
UN RIDE
        ↓
UN CORREO
```

Una vez que ese flujo sea sólido, el mismo motor podrá soportar los demás documentos sin volver a duplicar toda la lógica.
