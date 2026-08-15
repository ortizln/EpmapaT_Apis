# ARQUITECTURA BACKEND — SRI-FILES

**Proyecto:** Plataforma de Administración de Documentos Electrónicos SRI  
**Backend:** Java 17 + Spring Boot 3.4.x  
**Persistencia:** PostgreSQL + Spring Data JPA  
**Migraciones:** Flyway  
**Integración SRI:** SOAP / Jakarta JAX-WS  
**Firma:** XAdES / xades4j / xmlsec  
**Reportes:** JasperReports  
**API:** REST `/api/v1`  
**Fecha:** 2026-08-14  
**Versión:** 1.0

---

# 1. Objetivo

Reestructurar el backend actual de `sri-files` para convertirlo en un servicio independiente, modular, auditable y extensible para el procesamiento de documentos electrónicos.

El backend deberá administrar:

```text
JSON
 ↓
Persistencia
 ↓
Validación
 ↓
Generación XML
 ↓
Validación XSD
 ↓
Firma XAdES
 ↓
Recepción SRI
 ↓
Autorización SRI
 ↓
XML autorizado
 ↓
RIDE/PDF
 ↓
Correo
 ↓
FINALIZADO
```

El objetivo principal es eliminar la concentración de lógica actualmente existente en el controlador principal y separar claramente:

```text
API
DOMINIO
APLICACIÓN
PERSISTENCIA
INTEGRACIONES
PROCESAMIENTO
SEGURIDAD
AUDITORÍA
```

---

# 2. Regla de migración

La reestructuración NO deberá comenzar eliminando el código productivo existente.

Aplicar:

```text
IDENTIFICAR
   ↓
AISLAR
   ↓
CUBRIR CON PRUEBAS
   ↓
ENCAPSULAR
   ↓
REUTILIZAR
   ↓
MIGRAR
   ↓
RETIRAR LEGACY
```

Los componentes actuales relacionados con:

- firma electrónica;
- SOAP SRI;
- consulta de autorización;
- JasperReports;
- generación PDF;
- correo;

deberán reutilizarse siempre que su comportamiento sea correcto.

---

# 3. Arquitectura general

```text
                    ┌───────────────────────┐
                    │       CLIENTES        │
                    │ ERP / Apps / Frontend │
                    └───────────┬───────────┘
                                │
                                ▼
                    ┌───────────────────────┐
                    │    REST CONTROLLERS   │
                    │       /api/v1         │
                    └───────────┬───────────┘
                                │
                                ▼
                    ┌───────────────────────┐
                    │ APPLICATION SERVICES  │
                    └───────────┬───────────┘
                                │
                   ┌────────────┼─────────────┐
                   ▼            ▼             ▼
               DOMAIN       SECURITY       AUDIT
                   │
                   ▼
              PROCESSORS
                   │
       ┌───────────┼──────────────┐
       ▼           ▼              ▼
     XML         FIRMA           SRI
       │                           │
       └────────────┬──────────────┘
                    ▼
                  RIDE
                    │
                    ▼
                 CORREO
                    │
                    ▼
               POSTGRESQL
```

---

# 4. Estilo arquitectónico

Se recomienda una arquitectura modular inspirada en:

```text
Clean Architecture
+
Hexagonal Architecture
+
Layered Architecture
```

sin introducir complejidad innecesaria.

Las reglas centrales serán:

```text
Controller
   ↓
Application Service
   ↓
Domain / Processor
   ↓
Ports
   ↓
Infrastructure adapters
```

Los controladores nunca deberán:

- construir XML;
- firmar XML;
- llamar directamente al SOAP del SRI;
- generar Jasper;
- manipular archivos físicos;
- ejecutar consultas SQL;
- controlar reintentos.

---

# 5. Estructura de paquetes propuesta

```text
src/main/java/ec/com/epmapat/srifiles/

SriFilesApplication.java

config/
    AsyncConfig.java
    JacksonConfig.java
    OpenApiConfig.java
    SecurityConfig.java
    StorageConfig.java
    SriProperties.java
    ProcessingProperties.java

controller/
    DocumentoController.java
    DocumentoArchivoController.java
    DocumentoOperacionController.java
    DashboardController.java
    CatalogoController.java
    EmpresaController.java
    EstablecimientoController.java
    PuntoEmisionController.java
    CertificadoController.java
    ConfiguracionSriController.java
    UsuarioController.java
    RolController.java
    AuditoriaController.java
    HealthController.java

dto/
    request/
        DocumentoRequest.java
        EmisorRequest.java
        SecuencialRequest.java
        ReceptorRequest.java
        CorreoRequest.java
        InformacionAdicionalRequest.java

        factura/
        liquidacion/
        notaCredito/
        notaDebito/
        retencion/
        guiaRemision/

    response/
        DocumentoAceptadoResponse.java
        DocumentoDetalleResponse.java
        DocumentoEstadoResponse.java
        DocumentoResumenResponse.java
        HistorialResponse.java
        ErrorResponse.java
        DashboardResponse.java

domain/
    enums/
        TipoDocumento.java
        EstadoDocumento.java
        TipoArchivo.java
        TipoOperacionSri.java
        EstadoCorreo.java
        AmbienteSri.java

    model/
    exception/
    validation/

entity/
    EmpresaEntity.java
    EstablecimientoEntity.java
    PuntoEmisionEntity.java
    SecuencialEntity.java
    DocumentoElectronicoEntity.java
    DocumentoEstadoHistorialEntity.java
    DocumentoArchivoEntity.java
    DocumentoErrorEntity.java
    DocumentoIntentoSriEntity.java
    DocumentoCorreoEntity.java
    DocumentoEventoEntity.java
    CertificadoDigitalEntity.java
    ConfiguracionSriEntity.java
    ConfiguracionCorreoEntity.java
    PlantillaRideEntity.java
    RecursoEmpresaEntity.java
    UsuarioEntity.java
    RolEntity.java
    PermisoEntity.java
    AuditoriaEntity.java

repository/
    EmpresaRepository.java
    EstablecimientoRepository.java
    PuntoEmisionRepository.java
    SecuencialRepository.java
    DocumentoElectronicoRepository.java
    DocumentoEstadoHistorialRepository.java
    DocumentoArchivoRepository.java
    DocumentoErrorRepository.java
    DocumentoIntentoSriRepository.java
    DocumentoCorreoRepository.java
    CertificadoDigitalRepository.java
    AuditoriaRepository.java

mapper/
    DocumentoMapper.java
    EmpresaMapper.java
    UsuarioMapper.java

service/
    DocumentoApplicationService.java
    DocumentoConsultaService.java
    DocumentoOperacionService.java
    EstadoDocumentoService.java
    SecuencialService.java
    ArchivoDocumentoService.java
    CertificadoService.java
    DashboardService.java
    AuditoriaService.java

processor/
    DocumentoProcessor.java
    DocumentoProcessorFactory.java
    AbstractDocumentoProcessor.java

    factura/
        FacturaProcessor.java

    liquidacion/
        LiquidacionCompraProcessor.java

    notaCredito/
        NotaCreditoProcessor.java

    notaDebito/
        NotaDebitoProcessor.java

    retencion/
        RetencionProcessor.java

    guiaRemision/
        GuiaRemisionProcessor.java

validation/
    DocumentoValidator.java
    factura/
        FacturaValidator.java
    liquidacion/
        LiquidacionCompraValidator.java
    notaCredito/
        NotaCreditoValidator.java
    notaDebito/
        NotaDebitoValidator.java
    retencion/
        RetencionValidator.java
    guiaRemision/
        GuiaRemisionValidator.java

xml/
    XmlGenerator.java
    XmlValidationService.java

    factura/
        FacturaXmlGenerator.java

    liquidacion/
        LiquidacionCompraXmlGenerator.java

    notaCredito/
        NotaCreditoXmlGenerator.java

    notaDebito/
        NotaDebitoXmlGenerator.java

    retencion/
        RetencionXmlGenerator.java

    guiaRemision/
        GuiaRemisionXmlGenerator.java

signature/
    FirmaElectronicaService.java
    CertificateLoader.java
    CertificateValidator.java

sri/
    port/
        SriRecepcionPort.java
        SriAutorizacionPort.java

    adapter/
        soap/
            SriRecepcionSoapAdapter.java
            SriAutorizacionSoapAdapter.java

    model/
        RecepcionSriResult.java
        AutorizacionSriResult.java
        MensajeSri.java

ride/
    RideService.java
    JasperRideService.java
    RideTemplateResolver.java

mail/
    CorreoDocumentoService.java
    EmailClient.java
    EmailMicroserviceClient.java

storage/
    StorageService.java
    LocalStorageService.java
    StoragePathResolver.java

processing/
    DocumentoProcessingService.java
    DocumentoWorker.java
    DocumentoClaimService.java
    RetryPolicyService.java
    RecoveryService.java

scheduler/
    DocumentoPendingScheduler.java
    AutorizacionPendingScheduler.java
    CorreoPendingScheduler.java
    RecoveryScheduler.java

security/
    JwtAuthenticationFilter.java
    JwtService.java
    UserDetailsServiceImpl.java
    PermissionService.java

audit/
    AuditAspect.java
    AuditContext.java

exception/
    GlobalExceptionHandler.java
    BusinessException.java
    ValidationException.java
    SriException.java
    SignatureException.java
    StorageException.java

util/
    ClaveAccesoGenerator.java
    Modulo11.java
    HashUtils.java
    DateUtils.java
```

---

# 6. División por responsabilidad

## controller

Responsable únicamente de:

```text
HTTP request
 ↓
validación superficial
 ↓
application service
 ↓
HTTP response
```

No contiene reglas tributarias.

---

# 7. DocumentoController

Endpoints principales:

```text
POST /api/v1/documentos

GET /api/v1/documentos

GET /api/v1/documentos/{uuid}

GET /api/v1/documentos/{uuid}/estado

GET /api/v1/documentos/{uuid}/historial
```

Ejemplo conceptual:

```java
@RestController
@RequestMapping("/api/v1/documentos")
@RequiredArgsConstructor
public class DocumentoController {

    private final DocumentoApplicationService documentoService;

    @PostMapping
    public ResponseEntity<DocumentoAceptadoResponse> crear(
            @Valid @RequestBody DocumentoRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false)
            String idempotencyKey) {

        var response = documentoService.recibir(
            request,
            idempotencyKey
        );

        return ResponseEntity
            .accepted()
            .body(response);
    }
}
```

---

# 8. DocumentoApplicationService

Responsabilidades:

1. validar consumidor;
2. validar empresa;
3. validar establecimiento;
4. validar punto de emisión;
5. verificar idempotencia;
6. registrar documento;
7. registrar estado inicial;
8. programar procesamiento;
9. devolver UUID.

NO deberá procesar completamente el documento dentro de la petición HTTP.

---

# 9. Flujo de recepción

```text
POST /api/v1/documentos
          │
          ▼
Authentication
          │
          ▼
Bean Validation
          │
          ▼
Validar empresa / punto emisión
          │
          ▼
Idempotencia
          │
          ▼
Guardar JSON original
          │
          ▼
estado = RECIBIDO
          │
          ▼
Commit
          │
          ▼
202 ACCEPTED
          │
          ▼
Worker
```

La persistencia deberá completarse antes de responder.

---

# 10. DTO base

No utilizar:

```java
Map<String,Object>
```

como contrato principal.

Crear DTOs tipados.

Conceptualmente:

```java
public record DocumentoRequest(
    String version,
    TipoDocumento tipoDocumento,
    String externalId,
    EmisorRequest emisor,
    SecuencialRequest secuencial,
    Object documento,
    Object receptor
) {}
```

Sin embargo, para evitar `Object`, la implementación preferida será polimórfica mediante Jackson.

---

# 11. DTO polimórfico

Ejemplo conceptual:

```java
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "tipoDocumento",
    visible = true
)
@JsonSubTypes({
    @JsonSubTypes.Type(
        value = FacturaRequest.class,
        name = "FACTURA"
    ),
    @JsonSubTypes.Type(
        value = RetencionRequest.class,
        name = "RETENCION"
    )
})
public interface DocumentoRequest {
    TipoDocumento getTipoDocumento();
}
```

Agregar progresivamente los seis tipos.

---

# 12. DocumentoProcessor

Cada documento utilizará el mismo contrato de procesamiento.

```java
public interface DocumentoProcessor<T extends DocumentoRequest> {

    TipoDocumento soporta();

    void validar(T request);

    GeneratedXml generarXml(
        DocumentoElectronicoEntity documento,
        T request
    );

    byte[] generarRide(
        DocumentoElectronicoEntity documento,
        String xmlAutorizado
    );
}
```

La firma y el SRI pueden mantenerse fuera del processor específico porque son comunes.

---

# 13. DocumentoProcessorFactory

```java
@Component
public class DocumentoProcessorFactory {

    private final Map<TipoDocumento, DocumentoProcessor<?>> processors;

    public DocumentoProcessor<?> obtener(
        TipoDocumento tipoDocumento
    ) {
        // resolver processor
    }
}
```

Esto elimina estructuras gigantes como:

```java
if factura...
else if retencion...
else if nota credito...
```

---

# 14. Pipeline común

El motor central será:

```text
DocumentoProcessingService
```

Flujo:

```text
claim documento
     ↓
resolver processor
     ↓
VALIDANDO
     ↓
processor.validar()
     ↓
VALIDADO
     ↓
processor.generarXml()
     ↓
validar XSD
     ↓
guardar XML_GENERADO
     ↓
XML_GENERADO
     ↓
FirmaElectronicaService
     ↓
guardar XML_FIRMADO
     ↓
FIRMADO
     ↓
SriRecepcionPort
     ↓
RECIBIDO_SRI
     ↓
SriAutorizacionPort
     ↓
AUTORIZADO
     ↓
guardar XML_AUTORIZADO
     ↓
RideService
     ↓
RIDE_GENERADO
     ↓
CorreoDocumentoService
     ↓
FINALIZADO
```

---

# 15. Máquina de estados

Crear:

```text
EstadoDocumento
```

```java
public enum EstadoDocumento {

    RECIBIDO,
    VALIDANDO,
    VALIDADO,
    XML_GENERADO,
    FIRMADO,

    ENVIANDO_SRI,
    RECIBIDO_SRI,
    PENDIENTE_AUTORIZACION,
    AUTORIZADO,

    RIDE_GENERADO,

    CORREO_PENDIENTE,
    CORREO_ENVIADO,

    FINALIZADO,

    ERROR_VALIDACION,
    ERROR_XML,
    ERROR_FIRMA,
    ERROR_ENVIO_SRI,
    DEVUELTO_SRI,
    NO_AUTORIZADO,
    ERROR_AUTORIZACION,
    ERROR_RIDE,
    ERROR_CORREO,

    REQUIERE_INTERVENCION,
    CANCELADO
}
```

---

# 16. EstadoDocumentoService

Nunca realizar:

```java
documento.setEstadoActual("A");
repository.save(documento);
```

desde múltiples clases.

Centralizar:

```java
estadoDocumentoService.cambiar(
    documento,
    EstadoDocumento.AUTORIZADO,
    "Documento autorizado por el SRI"
);
```

Este servicio deberá:

1. validar transición;
2. actualizar estado actual;
3. insertar historial;
4. registrar metadata;
5. persistir dentro de transacción.

---

# 17. Transiciones permitidas

Ejemplo:

```text
RECIBIDO
  → VALIDANDO

VALIDANDO
  → VALIDADO
  → ERROR_VALIDACION

VALIDADO
  → XML_GENERADO
  → ERROR_XML

XML_GENERADO
  → FIRMADO
  → ERROR_FIRMA

FIRMADO
  → ENVIANDO_SRI

ENVIANDO_SRI
  → RECIBIDO_SRI
  → DEVUELTO_SRI
  → ERROR_ENVIO_SRI

RECIBIDO_SRI
  → PENDIENTE_AUTORIZACION

PENDIENTE_AUTORIZACION
  → AUTORIZADO
  → NO_AUTORIZADO
  → ERROR_AUTORIZACION

AUTORIZADO
  → RIDE_GENERADO
  → ERROR_RIDE

RIDE_GENERADO
  → CORREO_PENDIENTE
  → FINALIZADO

CORREO_PENDIENTE
  → CORREO_ENVIADO
  → ERROR_CORREO

CORREO_ENVIADO
  → FINALIZADO
```

---

# 18. Validación

Separar:

```text
Bean Validation
Business Validation
XML/XSD Validation
```

## Bean Validation

Formato del JSON.

## Business Validation

Reglas del documento.

## XSD

Validez del XML generado.

---

# 19. XML generators

Interfaz:

```java
public interface XmlGenerator<T> {

    String generate(
        DocumentoElectronicoEntity documento,
        T request
    );
}
```

Implementaciones:

```text
FacturaXmlGenerator
LiquidacionCompraXmlGenerator
NotaCreditoXmlGenerator
NotaDebitoXmlGenerator
RetencionXmlGenerator
GuiaRemisionXmlGenerator
```

No construir XML en controladores.

---

# 20. XmlValidationService

Responsable de:

```text
resolver XSD
cargar schema
validar XML
devolver errores estructurados
```

Flujo:

```text
XML generado
   ↓
XSD válido?
   │
   ├── NO → ERROR_XML
   │
   └── SÍ → FIRMA
```

---

# 21. Versiones XML

Crear un resolver:

```text
XmlSchemaResolver
```

Ejemplo:

```text
FACTURA
   → versión configurada
   → factura.xsd

RETENCION
   → versión configurada
   → comprobanteRetencion.xsd
```

La versión XML no deberá venir arbitrariamente desde el ERP.

---

# 22. Firma electrónica

Encapsular código actual en:

```text
FirmaElectronicaService
```

Contrato:

```java
public interface FirmaElectronicaService {

    SignedXmlResult firmar(
        String xml,
        CertificadoDigital certificado
    );
}
```

Responsabilidades:

```text
cargar certificado
validar vigencia
resolver contraseña
firmar
validar resultado
devolver XML firmado
```

---

# 23. CertificateLoader

Separar la lectura física del certificado:

```text
CertificateLoader
```

Esto permite posteriormente utilizar:

```text
archivo local
Docker secret
Vault
HSM
servicio externo
```

sin modificar el procesamiento documental.

---

# 24. Integración SRI mediante ports

No acoplar el dominio directamente al cliente SOAP generado.

Crear:

```java
public interface SriRecepcionPort {

    RecepcionSriResult enviar(
        String xmlFirmado
    );
}
```

y:

```java
public interface SriAutorizacionPort {

    AutorizacionSriResult consultar(
        String claveAcceso
    );
}
```

---

# 25. Adaptadores SOAP

La implementación actual SOAP deberá encapsularse en:

```text
SriRecepcionSoapAdapter
SriAutorizacionSoapAdapter
```

Estos adaptadores podrán reutilizar:

```text
WSDL locales
JAX-WS
clases generadas
```

que ya existen en el proyecto.

---

# 26. Resultado de recepción

No devolver directamente objetos SOAP hacia otras capas.

Crear:

```java
public record RecepcionSriResult(
    boolean recibido,
    String estado,
    List<MensajeSri> mensajes
) {}
```

---

# 27. Resultado autorización

```java
public record AutorizacionSriResult(
    boolean autorizado,
    boolean pendiente,
    String estado,
    String numeroAutorizacion,
    OffsetDateTime fechaAutorizacion,
    String xmlAutorizado,
    List<MensajeSri> mensajes
) {}
```

---

# 28. Persistencia de intentos SRI

Cada llamada deberá registrar:

```text
documento
operación
número intento
inicio
fin
duración
resultado
código
mensaje
respuesta
```

Nunca depender exclusivamente del log de consola.

---

# 29. Polling de autorización

No realizar un loop HTTP largo:

```java
while (!autorizado) {
    Thread.sleep(...);
}
```

dentro del request original.

Utilizar estados persistentes:

```text
RECIBIDO_SRI
       ↓
PENDIENTE_AUTORIZACION
```

y posteriormente:

```text
AutorizacionPendingScheduler
```

---

# 30. Workers

Crear:

```text
DocumentoWorker
```

Los workers reclamarán documentos pendientes desde PostgreSQL.

Consulta conceptual:

```sql
SELECT id
FROM documento_electronico
WHERE estado_actual = 'RECIBIDO'
ORDER BY fecha_recepcion
FOR UPDATE SKIP LOCKED
LIMIT 10;
```

Esto permite múltiples workers sin procesar dos veces el mismo registro.

---

# 31. Procesamiento inicial sin broker

Para la primera versión:

```text
PostgreSQL
+
Spring Scheduler
+
Workers
```

es suficiente y reduce infraestructura.

No introducir RabbitMQ obligatoriamente desde el primer día.

La arquitectura deberá permitir agregarlo posteriormente.

---

# 32. Configuración de worker

```yaml
sri-files:
  processing:
    enabled: true
    batch-size: 10
    fixed-delay: 2000
    max-retries: 5
```

---

# 33. Schedulers separados

No crear un scheduler gigantesco.

Crear:

```text
DocumentoPendingScheduler
AutorizacionPendingScheduler
CorreoPendingScheduler
RecoveryScheduler
```

Cada uno procesa una responsabilidad.

---

# 34. RetryPolicyService

Determinar:

```text
¿es recuperable?
¿cuántos intentos?
¿cuándo reintentar?
```

Ejemplo:

```text
1 → inmediato
2 → 30 segundos
3 → 1 minuto
4 → 5 minutos
5 → 15 minutos
```

Después:

```text
REQUIERE_INTERVENCION
```

---

# 35. Errores no reintentables

Ejemplos:

```text
identificación inválida
XML inválido por datos
secuencial duplicado
documento no autorizado por regla funcional
certificado definitivamente expirado
```

No ejecutar cinco veces una operación que requiere intervención humana.

---

# 36. Errores potencialmente recuperables

Ejemplos:

```text
timeout SRI
conexión temporal
HTTP 5xx del microservicio de correo
problema temporal de almacenamiento
```

---

# 37. RecoveryService

Al iniciar la aplicación:

```text
RecoveryService
```

deberá detectar documentos que quedaron en estados transitorios.

Ejemplo:

```text
ENVIANDO_SRI
```

durante un reinicio.

No asumir que debe reenviarse inmediatamente.

Primero consultar la situación persistida y, cuando corresponda, verificar autorización por clave antes de repetir el envío.

---

# 38. StorageService

Interfaz:

```java
public interface StorageService {

    StoredFile save(
        String path,
        byte[] content,
        String contentType
    );

    byte[] read(String path);

    void delete(String path);

    boolean exists(String path);
}
```

Implementación inicial:

```text
LocalStorageService
```

---

# 39. Rutas

No construir rutas físicas dispersas.

Crear:

```text
StoragePathResolver
```

Ejemplo:

```text
2026/08/factura/{claveAcceso}/generado.xml

2026/08/factura/{claveAcceso}/firmado.xml

2026/08/factura/{claveAcceso}/autorizado.xml

2026/08/factura/{claveAcceso}/ride.pdf
```

---

# 40. Hash

Al guardar archivos calcular:

```text
SHA-256
```

y registrar en:

```text
documento_archivo.hash_sha256
```

Esto ayuda a detectar alteraciones.

---

# 41. RIDE

Crear:

```text
RideService
```

Implementación:

```text
JasperRideService
```

Responsabilidades:

```text
resolver plantilla
leer XML autorizado
generar datasource
cargar logo
generar PDF
guardar archivo
```

---

# 42. RideTemplateResolver

Resolver plantilla por:

```text
empresa
tipo documento
versión
predeterminada
```

No utilizar rutas Jasper hardcoded en controladores.

---

# 43. Correo

Crear:

```text
CorreoDocumentoService
```

y:

```text
EmailClient
```

La integración actual con `msvc-emails` deberá quedar detrás de:

```text
EmailMicroserviceClient
```

Así el dominio no depende directamente del microservicio actual.

---

# 44. Fallo de correo

Si el documento ya está:

```text
AUTORIZADO
```

y falla correo:

```text
NO cambiar a NO_AUTORIZADO.
```

Estado:

```text
ERROR_CORREO
```

El XML autorizado y RIDE continúan siendo válidos.

---

# 45. Reenvío de correo

Endpoint:

```text
POST /api/v1/documentos/{uuid}/reenviar-correo
```

Debe:

```text
validar permiso
verificar XML autorizado
verificar RIDE
crear nuevo intento
registrar auditoría
```

---

# 46. Secuenciales

Crear:

```text
SecuencialService
```

Método conceptual:

```java
@Transactional
public String siguiente(
    Long puntoEmisionId,
    TipoDocumento tipo
)
```

Utilizar:

```text
pessimistic locking
```

o:

```text
UPDATE ... RETURNING
```

---

# 47. Clave de acceso

Crear servicio/utilidad pura:

```text
ClaveAccesoGenerator
```

Responsable de construir y validar la clave.

Crear:

```text
Modulo11
```

como componente aislado y completamente probado.

---

# 48. Idempotencia

Antes de crear:

```text
buscar empresa + externalId
```

y/o:

```text
empresa + Idempotency-Key
```

Si existe:

```text
devolver documento existente
```

No volver a procesar automáticamente.

---

# 49. Transacciones

No envolver todo el ciclo SRI en una sola transacción.

Incorrecto:

```text
BEGIN
guardar
firmar
SOAP
esperar
Jasper
email
COMMIT
```

Correcto:

```text
TX corta → registrar
TX corta → cambiar estado
operación externa
TX corta → guardar resultado
```

Esto evita bloqueos prolongados.

---

# 50. Auditoría

Crear:

```text
AuditoriaService
```

y opcionalmente:

```text
@Auditable
```

con AspectJ/Spring AOP.

Registrar acciones administrativas:

```text
DOCUMENTO_REPROCESAR
DOCUMENTO_REENVIAR_CORREO
DOCUMENTO_DESCARGAR_XML
DOCUMENTO_DESCARGAR_RIDE
CERTIFICADO_CARGAR
CERTIFICADO_REEMPLAZAR
CONFIGURACION_MODIFICAR
USUARIO_CREAR
ROL_MODIFICAR
```

---

# 51. Seguridad

Utilizar:

```text
Spring Security
JWT
RBAC
```

Flujo:

```text
JWT
 ↓
usuario
 ↓
roles
 ↓
permisos
 ↓
endpoint
```

---

# 52. Permisos

Ejemplo:

```java
@PreAuthorize(
    "hasAuthority('DOCUMENTO_REPROCESAR')"
)
```

No depender únicamente de:

```text
ROLE_ADMIN
```

Los permisos finos permitirán administrar operaciones críticas.

---

# 53. Consumidores externos

Separar conceptualmente:

```text
USUARIOS DEL FRONTEND
```

de:

```text
CLIENTES API
```

A futuro se recomienda una tabla:

```text
api_client
```

con:

```text
client_id
empresa
nombre
activo
permisos
```

La primera versión puede usar JWT si ya existe una infraestructura adecuada.

---

# 54. GlobalExceptionHandler

Formato uniforme:

```json
{
  "timestamp": "2026-08-14T10:31:02-05:00",
  "status": 400,
  "code": "DOC_VALIDATION_ERROR",
  "message": "El documento contiene errores",
  "details": []
}
```

Mapear:

```text
ValidationException → 400

Authentication → 401

Permission → 403

NotFound → 404

Conflict / idempotencia especial → 409 cuando aplique

Internal → 500
```

---

# 55. Logging

Utilizar logs estructurados.

Incluir:

```text
requestId
documentUuid
externalId
claveAcceso
tipoDocumento
etapa
```

No incluir:

```text
password
JWT completo
password certificado
secretos
```

---

# 56. Correlation ID

Filtro:

```text
CorrelationIdFilter
```

Leer:

```text
X-Request-Id
```

o generar UUID.

Propagar mediante MDC:

```text
requestId
```

para correlacionar todo el procesamiento.

---

# 57. Actuator

Agregar:

```text
Spring Boot Actuator
```

Endpoints:

```text
/actuator/health
/actuator/info
```

No exponer públicamente endpoints sensibles.

---

# 58. Health indicators

Crear indicadores para:

```text
PostgreSQL
almacenamiento
certificado
msvc-emails
```

Para SRI se deberá evitar que una comprobación demasiado frecuente genere carga innecesaria; puede mostrarse el último estado de conectividad conocido.

---

# 59. OpenAPI

Agrupar:

```text
Documentos
Archivos
Operaciones
Dashboard
Catálogos
Configuración
Usuarios
Auditoría
```

Documentar:

```text
request
response
errores
ejemplos
```

---

# 60. Flyway

Ruta:

```text
src/main/resources/db/migration
```

Usar las migraciones definidas en el modelo de BD.

Producción:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate

  flyway:
    enabled: true
```

---

# 61. Perfiles

```text
application.yml
application-dev.yml
application-test.yml
application-prod.yml
```

No mezclar credenciales.

---

# 62. Configuración tipada

No utilizar múltiples:

```java
@Value("${...}")
```

dispersos.

Crear:

```java
@ConfigurationProperties(
    prefix = "sri-files"
)
```

Ejemplo:

```yaml
sri-files:

  processing:
    batch-size: 10
    max-retries: 5

  storage:
    root: /data/sri-files

  sri:
    default-environment: 2
```

---

# 63. Tests

Agregar:

```text
spring-boot-starter-test
```

y estructurar:

```text
unit/
integration/
contract/
```

---

# 64. Unit tests prioritarios

```text
Modulo11Test
ClaveAccesoGeneratorTest

FacturaValidatorTest
RetencionValidatorTest

FacturaXmlGeneratorTest
RetencionXmlGeneratorTest

EstadoDocumentoServiceTest

RetryPolicyServiceTest

SecuencialServiceTest
```

---

# 65. Contract tests

Utilizar los JSON definidos en:

```text
src/test/resources/contracts/
```

Comprobar:

```text
JSON
 ↓
DTO
 ↓
validación
 ↓
XML
 ↓
XSD
```

---

# 66. Integration tests

Utilizar idealmente:

```text
Testcontainers PostgreSQL
```

para probar:

```text
Flyway
JPA
constraints
idempotencia
secuenciales
workers
```

---

# 67. SOAP tests

No depender del SRI real para todas las pruebas.

Crear adaptadores simulables:

```text
SriRecepcionPort
SriAutorizacionPort
```

Esto permite:

```java
@MockBean
SriRecepcionPort
```

en tests.

---

# 68. Primera migración del código actual

Orden concreto:

## Paso 1

Crear branch:

```text
feature/sri-files-v2
```

## Paso 2

Corregir tests actuales.

## Paso 3

Agregar Flyway.

## Paso 4

Crear nuevas tablas.

## Paso 5

Crear enums.

## Paso 6

Crear entities/repositories.

## Paso 7

Crear `EstadoDocumentoService`.

## Paso 8

Crear `StorageService`.

## Paso 9

Extraer firma actual del controlador.

## Paso 10

Extraer recepción SOAP.

## Paso 11

Extraer autorización SOAP.

## Paso 12

Extraer Jasper.

## Paso 13

Extraer correo.

En este punto todavía no eliminar endpoints antiguos.

---

# 69. Segunda migración — Factura

Implementar:

```text
FacturaRequest
FacturaValidator
FacturaXmlGenerator
FacturaProcessor
```

Nuevo endpoint:

```text
POST /api/v1/documentos
```

Ejecutar factura en paralelo con el flujo actual.

---

# 70. Comparación obligatoria

Para una misma factura de pruebas:

```text
FLUJO LEGACY
vs
FLUJO V2
```

comparar:

```text
clave acceso
XML
firma
recepción
autorización
XML autorizado
RIDE
correo
```

No pasar a producción hasta validar equivalencia funcional.

---

# 71. Tercera migración — Retención

Cuando factura esté estable:

```text
RetencionRequest
RetencionValidator
RetencionXmlGenerator
RetencionProcessor
```

Reutilizar la funcionalidad actual de retenciones encapsulada.

---

# 72. Documentos restantes

Orden:

```text
NotaCredito
NotaDebito
LiquidacionCompra
GuiaRemision
```

Cada uno deberá superar:

```text
unit tests
contract tests
XSD
ambiente de pruebas SRI
RIDE
```

---

# 73. Compatibilidad legacy

Mantener temporalmente:

```text
/api/singsend/*
```

pero marcar internamente:

```text
LEGACY
```

No agregar nuevas funcionalidades importantes al controlador antiguo.

Toda nueva funcionalidad deberá ir a:

```text
/api/v1
```

---

# 74. Fachada legacy opcional

Para reducir duplicación, los endpoints antiguos pueden adaptarse progresivamente para invocar:

```text
DocumentoApplicationService
```

en lugar de mantener dos motores.

---

# 75. Eliminación de SRI_Controller

No eliminar hasta que:

- [ ] Factura use V2.
- [ ] Retención use V2.
- [ ] No existan consumidores legacy relevantes.
- [ ] Los schedulers antiguos hayan sido reemplazados.
- [ ] Swagger nuevo esté operativo.
- [ ] Las pruebas estén verdes.
- [ ] Producción haya superado período de estabilización.

---

# 76. Docker

Mantener contenedor Spring Boot.

Agregar volumen:

```yaml
volumes:
  - sri_files_data:/data/sri-files
```

No guardar documentos dentro de capas efímeras del contenedor.

---

# 77. Shutdown

Configurar graceful shutdown para reducir procesos interrumpidos.

```yaml
server:
  shutdown: graceful

spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s
```

Los estados persistentes permitirán recuperación posterior.

---

# 78. Escalabilidad

La primera versión podrá ejecutar:

```text
1 instancia
```

Posteriormente:

```text
N instancias
```

si los workers utilizan correctamente:

```text
FOR UPDATE SKIP LOCKED
```

y no dependen de memoria local para la coordinación.

---

# 79. Posible evolución a RabbitMQ

No es requisito inicial.

La arquitectura permitirá reemplazar:

```text
PostgreSQL polling
```

por:

```text
RabbitMQ
```

sin cambiar:

```text
FacturaProcessor
FirmaElectronicaService
SriRecepcionPort
SriAutorizacionPort
RideService
```

---

# 80. Métricas

Medir:

```text
documentos recibidos
documentos autorizados
documentos no autorizados
errores
tiempo procesamiento
tiempo autorización
intentos SRI
correos enviados
correos fallidos
```

Posteriormente se podrá integrar:

```text
Micrometer
Prometheus
Grafana
```

sin que sea requisito de la primera entrega.

---

# 81. API de dashboard

Ejemplo:

```text
GET /api/v1/dashboard/resumen

GET /api/v1/dashboard/documentos-por-dia

GET /api/v1/dashboard/por-tipo

GET /api/v1/dashboard/por-estado

GET /api/v1/dashboard/errores

GET /api/v1/dashboard/tiempos
```

No mezclar consultas estadísticas dentro de `DocumentoController`.

---

# 82. API de archivos

```text
GET /api/v1/documentos/{uuid}/archivos

GET /api/v1/documentos/{uuid}/xml

GET /api/v1/documentos/{uuid}/xml-firmado

GET /api/v1/documentos/{uuid}/xml-autorizado

GET /api/v1/documentos/{uuid}/ride
```

Validar permisos antes de descargar.

---

# 83. Operaciones administrativas

```text
POST /api/v1/documentos/{uuid}/reprocesar

POST /api/v1/documentos/{uuid}/consultar-autorizacion

POST /api/v1/documentos/{uuid}/regenerar-ride

POST /api/v1/documentos/{uuid}/reenviar-correo
```

Evitar inicialmente un botón genérico:

```text
REENVIAR TODO
```

porque puede provocar duplicidad o acciones incorrectas.

---

# 84. Reprocesamiento seguro

`DocumentoOperacionService` deberá decidir desde qué etapa continuar.

Ejemplo:

```text
ERROR_RIDE
```

NO debe:

```text
generar XML
firmar
reenviar al SRI
```

Debe continuar:

```text
XML AUTORIZADO
 ↓
RIDE
```

---

# 85. Matriz de recuperación

```text
ERROR_VALIDACION
    → requiere corrección / nuevo documento

ERROR_XML
    → regenerar XML si la causa fue corregida

ERROR_FIRMA
    → firma

ERROR_ENVIO_SRI
    → verificar antes de reenviar

PENDIENTE_AUTORIZACION
    → consultar autorización

ERROR_AUTORIZACION
    → consultar nuevamente si recuperable

ERROR_RIDE
    → generar RIDE

ERROR_CORREO
    → reenviar correo
```

---

# 86. Regla crítica contra duplicidad

Ante duda después de un timeout en recepción:

```text
NO asumir automáticamente que el SRI no recibió.
```

Registrar el intento y ejecutar una estrategia segura basada en:

```text
clave de acceso
estado persistido
respuesta disponible
consulta posterior
```

La duplicidad tributaria debe considerarse un riesgo crítico.

---

# 87. Datos inmutables

Después de iniciar procesamiento, tratar como inmutables:

```text
json_original
external_id
tipo_documento
empresa
establecimiento
punto_emision
secuencial asignado
clave_acceso
```

Si el documento tiene datos incorrectos, no editar silenciosamente el JSON original.

Crear un nuevo documento cuando funcionalmente corresponda.

---

# 88. Soft delete

No eliminar físicamente documentos electrónicos desde el frontend.

Para configuraciones administrativas puede utilizarse:

```text
activo = false
```

Los documentos deberán conservar trazabilidad.

---

# 89. Código limpio

Reglas:

```text
Controller < 200 líneas idealmente
Service con responsabilidad definida
Métodos pequeños
No SQL en controller
No SOAP en controller
No rutas hardcoded
No secretos hardcoded
No estados mágicos
No códigos tributarios dispersos
```

---

# 90. Enums en lugar de códigos mágicos

Evitar:

```java
if (estado.equals("C"))
```

Utilizar:

```java
if (
    documento.getEstadoActual()
        == EstadoDocumento.AUTORIZADO
)
```

Para códigos SRI crear catálogos/resolvers explícitos.

---

# 91. Lombok

Puede utilizarse para reducir boilerplate si ya forma parte de las convenciones del proyecto.

No utilizar `@Data` indiscriminadamente sobre entidades JPA complejas.

Preferir:

```text
@Getter
@Setter
@NoArgsConstructor
```

según necesidad.

---

# 92. Mappers

No devolver entidades JPA directamente desde REST.

Usar:

```text
Entity
 ↓
Mapper
 ↓
Response DTO
```

Esto evita exponer accidentalmente:

```text
campos internos
relaciones
password hash
rutas físicas
metadata sensible
```

---

# 93. Criterios de aceptación backend

- [ ] `mvn test` funciona.
- [ ] Flyway administra el esquema.
- [ ] Existe `/api/v1`.
- [ ] No se utiliza `SRI_Controller` para nueva funcionalidad.
- [ ] Los DTOs son tipados.
- [ ] Existe idempotencia.
- [ ] Existe máquina de estados.
- [ ] Cada transición genera historial.
- [ ] Existe procesamiento asíncrono.
- [ ] Los workers sobreviven a reinicios.
- [ ] Existe control de concurrencia.
- [ ] La firma está encapsulada.
- [ ] SOAP recepción está encapsulado.
- [ ] SOAP autorización está encapsulado.
- [ ] Jasper está encapsulado.
- [ ] Correo está encapsulado.
- [ ] Existe `StorageService`.
- [ ] Los archivos tienen SHA-256.
- [ ] Existen reintentos controlados.
- [ ] Los errores se clasifican por etapa.
- [ ] Existe recuperación segura.
- [ ] Existe JWT/RBAC.
- [ ] Existe auditoría.
- [ ] Existe OpenAPI.
- [ ] Existe Actuator.
- [ ] Factura funciona end-to-end.
- [ ] Retención funciona end-to-end.
- [ ] Los seis tipos documentales pueden conectarse al pipeline común.

---

# 94. Roadmap de implementación backend

## FASE B0 — Estabilización

```text
pom.xml
tests
secrets
configuración
backup
```

## FASE B1 — Persistencia

```text
Flyway
entities
repositories
enums
```

## FASE B2 — Núcleo documental

```text
DocumentoApplicationService
EstadoDocumentoService
historial
idempotencia
StorageService
```

## FASE B3 — Infraestructura actual

Extraer:

```text
firma
SOAP recepción
SOAP autorización
Jasper
correo
```

## FASE B4 — Worker

```text
processing
scheduler
retry
recovery
```

## FASE B5 — Factura

```text
DTO
validator
XML
processor
XSD
end-to-end
```

## FASE B6 — Retención

Mismo patrón.

## FASE B7 — Otros documentos

```text
NC
ND
Liquidación
Guía
```

## FASE B8 — Administración

```text
empresa
establecimiento
punto emisión
certificados
usuarios
roles
permisos
```

## FASE B9 — Dashboard

```text
estadísticas
errores
monitoreo
```

## FASE B10 — Retiro legacy

Solo después de estabilización productiva.

---

# 95. Primer bloque concreto de trabajo

Para iniciar la programación, realizar exactamente en este orden:

```text
1. Crear branch feature/sri-files-v2

2. Agregar spring-boot-starter-test

3. Dejar mvn test en verde

4. Agregar Flyway

5. Crear V1...Vn de base

6. Crear TipoDocumento

7. Crear EstadoDocumento

8. Crear entities principales

9. Crear repositories

10. Crear EstadoDocumentoService

11. Crear DocumentoApplicationService

12. Crear POST /api/v1/documentos

13. Persistir JSON como RECIBIDO

14. Implementar idempotencia

15. Responder HTTP 202

16. Crear StorageService

17. Extraer firma actual

18. Extraer SOAP recepción

19. Extraer SOAP autorización

20. Implementar FacturaProcessor
```

En este punto ya existirá la columna vertebral del nuevo sistema.

---

# 96. Resultado esperado

Al finalizar esta arquitectura:

```text
ERP
 │
 │ JSON
 ▼
API V1
 │
 ▼
POSTGRESQL
 │
 ▼
WORKER
 │
 ├── VALIDACIÓN
 │
 ├── XML
 │
 ├── XSD
 │
 ├── FIRMA
 │
 ├── SRI
 │
 ├── AUTORIZACIÓN
 │
 ├── RIDE
 │
 └── CORREO
 │
 ▼
FINALIZADO
```

y cada etapa será:

```text
independiente
testeable
auditable
recuperable
reintentable
observable
```

---

# 97. Decisión arquitectónica final

El nuevo `sri-files` deberá funcionar como un **motor de procesamiento documental**, no como un conjunto de endpoints que ejecutan procedimientos aislados.

El componente central no será:

```text
SRI_Controller
```

sino:

```text
DocumentoProcessingService
+
DocumentoProcessorFactory
+
EstadoDocumentoService
```

Los componentes tributarios especializados se conectarán a este pipeline.

Esta separación permitirá incorporar nuevos comprobantes, nuevas versiones XML, almacenamiento externo, nuevos mecanismos de cola o nuevos servicios de correo sin volver a rediseñar todo el backend.

---

# 98. Siguiente entregable recomendado

Una vez aprobada esta arquitectura, crear:

```text
API_SRI_FILES_V1.md
```

con la especificación detallada de:

```text
autenticación
endpoints
parámetros
filtros
paginación
requests
responses
códigos HTTP
códigos de error
descargas
reprocesamiento
dashboard
configuración
usuarios
auditoría
```

Posteriormente:

```text
PLAN_IMPLEMENTACION_BACKEND_SRI_FILES.md
```

dividiendo la programación en tareas concretas y secuenciales para ejecutar sobre el repositorio actual.
