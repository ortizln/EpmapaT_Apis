# SRI Files

Repositorio organizado con frontend y backend separados.

## Estructura

```text
sri-files/
├── backend/
├── frontend/
├── MD/
└── README.md
```

## Backend

Ubicacion:

```text
sri-files/backend
```

Comandos principales:

```bash
cd backend
./mvnw clean package -Dmaven.test.skip=true
./deploy-backend-docker.sh
```

## Frontend

Ubicacion:

```text
sri-files/frontend
```

Comandos principales:

```bash
cd frontend
npm install
npm run build
```

Despliegue en Nginx nativo desde la raiz del repo:

```bash
./deploy-frontend-nginx.sh
```

## Notas

- El backend se despliega en Docker.
- El frontend se publica en Nginx nativo.
- La documentacion funcional y tecnica se encuentra en `MD/`.
