# Backend SRI Files

Este directorio contiene el backend Spring Boot del proyecto `sri-files`.

## Estructura

- `src/`: codigo fuente y pruebas
- `pom.xml`: configuracion Maven
- `Dockerfile`: imagen Docker del backend
- `docker-compose.yml`: despliegue local del backend
- `.env.example` y `.env.prod.example`: variables de entorno de referencia

## Comandos utiles

Compilar sin ejecutar ni compilar tests:

```bash
./mvnw clean package -Dmaven.test.skip=true
```

Ejecutar local:

```bash
./start-prod.sh
```

Desplegar en Docker:

```bash
./deploy-backend-docker.sh
```

## Nota tecnica

El paquete Java valido usado por el proyecto es `com.erp.sri_files`.
