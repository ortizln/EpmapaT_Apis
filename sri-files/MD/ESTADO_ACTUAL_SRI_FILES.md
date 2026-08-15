# Estado Actual del Servicio `sri-files`

Fecha de levantamiento: 2026-08-14

## 1. Resumen ejecutivo

`sri-files` es un microservicio Spring Boot orientado a facturacion electronica y retenciones SRI para EPMAPA-T. Actualmente el servicio:

- Genera, firma y envia comprobantes electronicos al SRI.
- Consulta autorizaciones SRI con polling.
- Genera PDF de facturas y retenciones desde XML autorizado.
- Expone endpoints HTTP para procesos manuales y consultas.
- Ejecuta tareas programadas para envio automatico y recuperacion de XML.
- Se integra con PostgreSQL, un backend ERP y el microservicio `emails`.

El estado general es `operativo con deuda tecnica`.

## 2. Estado funcional actual

### Funcionalidades implementadas

- API REST principal bajo `/api/singsend`.
- Flujo manual de facturas:
  - firma y envio desde XML cargado como archivo.
  - firma y envio desde XML en texto.
  - generacion y envio de factura electronica por `idfactura`.
  - generacion de PDF.
- Consulta de facturas por abonado y por cedula del cliente.
- Flujo de retenciones:
  - validacion.
  - firma.
  - consulta de autorizacion.
  - generacion de PDF.
  - descarga de XML/PDF.
  - envio por correo.
- Correo saliente delegado a `msvc-emails`.
- Scheduler para:
  - envio automatizado de facturas en estado `I`.
  - recuperacion de XML/autorizacion para facturas en estado `C` y `O`.
- Swagger/OpenAPI habilitado.

### Tecnologias detectadas

- Java 17
- Spring Boot 3.4.3
- Spring Web
- Spring Data JPA
- PostgreSQL
- JAX-WS / SOAP Jakarta para integracion con SRI
- JasperReports para PDFs
- iText / PDFBox
- xades4j / xmlsec para firma y validacion XML
- springdoc-openapi
- spring-dotenv

## 3. Validacion tecnica realizada

### Resultado de compilacion

- `./mvnw -q -DskipTests package`: exitoso.
- `./mvnw test`: falla.

### Motivo de falla en pruebas

El proyecto tiene clase de prueba `SriFilesApplicationTests`, pero en `pom.xml` no esta declarada la dependencia de pruebas de Spring Boot/JUnit. Por eso hoy el modulo no compila la capa de test.

Impacto:

- El artefacto de aplicacion puede generarse.
- No existe validacion automatizada confiable en CI con `mvn test`.

## 4. Configuracion y despliegue

### Perfiles y configuracion

Se detectaron:

- `application.yml`
- `application-prod.yml`
- `.env`
- `.env.prod.example`

Variables relevantes:

- `SERVER_PORT`
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `ERP_BACKEND_BASE_URL`
- `EMAIL_MS_BASE_URL`
- `SRI_AMBIENTE`
- cron de schedulers SRI

### Ejecucion

Se detectaron varios caminos de despliegue:

- `Dockerfile`
- `docker-compose.yml`
- `build-and-deploy.sh`
- `start-prod.sh`
- `deploy-prod.sh`

Observaciones:

- El `Dockerfile` empaqueta el jar compilado y expone puerto `9090`.
- `docker-compose.yml` levanta el servicio `sri-app`.
- `start-prod.sh` permite levantar el jar con perfil `prod`.
- `deploy-prod.sh` ya apunta a un script comun `deploy-java-service.sh`.

## 5. Integraciones externas detectadas

- Base de datos PostgreSQL.
- Servicios web SOAP del SRI:
  - recepcion de comprobantes.
  - autorizacion de comprobantes.
- Backend ERP por HTTP REST.
- Microservicio `emails` para encolar correos.

Tambien existen WSDL locales en `src/main/resources/wsdl`, lo cual ayuda a desacoplar la construccion del cliente SOAP del acceso remoto al endpoint.

## 6. Riesgos y hallazgos

### Riesgo alto

- No hay pruebas automatizadas operativas.
- Existen credenciales y URLs sensibles en archivos de despliegue y ejemplos locales.
- El controlador principal `SRI_Controller` concentra demasiada logica de negocio y es muy grande, lo que complica mantenimiento y pruebas.

### Riesgo medio

- Hay mezcla de flujos manuales, batch y correo dentro del mismo servicio.
- El estado de factura usa codigos cortos (`I`, `P`, `A`, `O`, `C`, `N`, `M`) que requieren documentacion formal para evitar errores funcionales.
- Hay dependencias fuertes a infraestructura interna:
  - PostgreSQL en red privada.
  - ERP backend en IP privada.
  - microservicio de correo.

### Riesgo bajo / deuda tecnica

- Hay recursos graficos duplicados dentro de `src/main/resources`.
- Existe clase de ejemplo o residual (`YourDataModel.java`) que parece no responder al dominio principal.
- El repositorio incluye carpeta `target`, lo que sugiere artefactos compilados presentes en el proyecto.

## 7. Estado del repositorio

Se detectaron cambios locales sin commit:

- modificacion en `.env`
- archivos nuevos de despliegue y ejemplos en varios servicios hermanos
- archivos nuevos locales en `sri-files` como `.env.prod.example` y `deploy-prod.sh`

Esto no bloquea el uso del servicio, pero indica que el entorno actual esta en movimiento y no necesariamente representa un estado completamente consolidado.

## 8. Endpoints visibles detectados

Todos bajo base path `/api/singsend`.

Entre los endpoints identificados estan:

- `/factura/xml`
- `/factura/string`
- `/factura`
- `/generar-pdf`
- `/factura_electronica`
- `/retenciones/pdf`
- `/retencion/download`
- `/retencion/mail`
- `/retenciones/download`
- `/retenciones/pdf`
- `/retenciones/xml`
- `/send`
- `/send-template`
- `/health`
- `/facturas-por-abonado`
- `/facturas-por-cliente-cedula`

Adicionalmente, Swagger esta configurado en:

- `/swagger-ui.html`
- `/v3/api-docs`

## 9. Conclusion

El servicio `sri-files` esta bastante avanzado y cubre procesos clave del flujo SRI, incluyendo envio, autorizacion, PDF y correo. A nivel de construccion, el jar se genera correctamente, por lo que el servicio es potencialmente desplegable.

Sin embargo, el estado actual todavia presenta deuda tecnica importante:

- pruebas rotas por dependencias faltantes.
- acoplamiento alto en el controlador principal.
- configuraciones sensibles mezcladas con scripts de despliegue.
- necesidad de documentar mejor estados de negocio y flujo operativo.

## 10. Recomendaciones inmediatas

1. Agregar `spring-boot-starter-test` al `pom.xml` y dejar `mvn test` en verde.
2. Documentar formalmente el significado de estados de factura: `I`, `P`, `A`, `O`, `C`, `N`, `M`.
3. Separar logica pesada de `SRI_Controller` en servicios especializados.
4. Mover secretos reales fuera de scripts y archivos versionados.
5. Agregar un README operativo con:
   - variables requeridas
   - dependencias externas
   - flujo manual
   - flujo batch
   - pasos de despliegue
6. Evaluar excluir `target/` del seguimiento si no debe permanecer en el repositorio.

