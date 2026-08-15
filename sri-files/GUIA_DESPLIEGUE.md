# Guia de Despliegue

Esta guia documenta el despliegue separado de `backend` y `frontend` para la estructura actual del repositorio:

```text
sri-files/
├── backend/
├── frontend/
└── deploy-frontend-nginx.sh
```

## 1. Backend

Ubicacion:

```text
sri-files/backend
```

Script principal:

```bash
./deploy-backend-docker.sh
```

### Variables esperadas

Antes de desplegar, crea tu archivo:

```bash
cp .env.prod.example .env.prod
```

Luego ajusta valores como:

```env
SPRING_PROFILES_ACTIVE=prod
SERVER_PORT=9090
SPRING_DATASOURCE_URL=jdbc:postgresql://IP_O_HOST:5432/BASE_DATOS
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=CLAVE_DB
ERP_BACKEND_BASE_URL=http://IP_BACKEND_ERP:9080
EMAIL_MS_BASE_URL=http://IP_SERVICIO_EMAIL:9099
```

### Ejecucion

```bash
cd backend
chmod +x deploy-backend-docker.sh
./deploy-backend-docker.sh
```

### Que hace este script

- Compila el backend Spring Boot
- Construye la imagen Docker
- Elimina el contenedor anterior si existe
- Levanta el backend con `--env-file`

## 2. Frontend

Ubicacion:

```text
sri-files/frontend
```

Script principal:

```bash
./deploy-frontend-nginx.sh
```

Este script:

- instala dependencias del frontend
- genera el build Angular
- publica `dist/frontend/browser` en `/var/www/...`
- instala la configuracion Nginx
- valida Nginx
- recarga el servicio

### Ejecucion

Desde la raiz del repo:

```bash
chmod +x deploy-frontend-nginx.sh
SERVER_NAME=midominio.com \
API_BASE_URL=http://127.0.0.1:9090 \
PUBLIC_DIR=/var/www/sri-files-frontend \
./deploy-frontend-nginx.sh
```

## 3. Configuracion Nginx

La configuracion que iria en `sites-available` puede quedar asi:

Ruta sugerida:

```text
/etc/nginx/sites-available/sri-files-frontend.conf
```

Contenido:

```nginx
server {
    listen 80;
    server_name midominio.com;

    root /var/www/sri-files-frontend;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:9090/api/;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location ~* \.(js|css|png|jpg|jpeg|gif|svg|ico|woff|woff2)$ {
        expires 7d;
        add_header Cache-Control "public, max-age=604800, immutable";
        try_files $uri =404;
    }
}
```

### Habilitar el sitio

```bash
sudo ln -s /etc/nginx/sites-available/sri-files-frontend.conf /etc/nginx/sites-enabled/sri-files-frontend.conf
sudo nginx -t
sudo systemctl reload nginx
```

## 4. SSL opcional

Si despues activas HTTPS con Certbot, la configuracion quedaria sobre el mismo sitio y normalmente Certbot agregara los bloques `listen 443 ssl` y certificados automaticamente.

### Ejemplo con dominio real y HTTPS

Si tu dominio real fuera `srifiles.midominio.com`, una configuracion tipica quedaria asi:

```nginx
server {
    listen 80;
    server_name srifiles.midominio.com;

    location / {
        return 301 https://$host$request_uri;
    }
}

server {
    listen 443 ssl http2;
    server_name srifiles.midominio.com;

    ssl_certificate /etc/letsencrypt/live/srifiles.midominio.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/srifiles.midominio.com/privkey.pem;

    root /var/www/sri-files-frontend;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:9090/api/;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location ~* \.(js|css|png|jpg|jpeg|gif|svg|ico|woff|woff2)$ {
        expires 7d;
        add_header Cache-Control "public, max-age=604800, immutable";
        try_files $uri =404;
    }
}
```

### Generar certificados con Certbot

Si usas Ubuntu o Debian con Nginx:

```bash
sudo apt update
sudo apt install certbot python3-certbot-nginx -y
sudo certbot --nginx -d srifiles.midominio.com
```

Verificar renovacion automatica:

```bash
sudo systemctl status certbot.timer
```

Probar renovacion:

```bash
sudo certbot renew --dry-run
```

## 5. Despliegue bajo subruta

Si no quieres publicar el frontend en la raiz del dominio y prefieres algo como:

```text
https://midominio.com/sri-files/
```

entonces necesitas dos cosas:

1. configurar Angular con `baseHref`
2. configurar Nginx para servir esa subruta

### Build Angular para subruta

Compila asi:

```bash
cd frontend
npx ng build --configuration production --base-href /sri-files/
```

### Nginx con subruta

```nginx
server {
    listen 80;
    server_name midominio.com;

    location /sri-files/ {
        alias /var/www/sri-files-frontend/;
        index index.html;
        try_files $uri $uri/ /sri-files/index.html;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:9090/api/;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

### Nota importante sobre subruta

Cuando usas `/sri-files/`, el frontend debe compilarse con:

```bash
--base-href /sri-files/
```

Si no haces eso, Angular intentara cargar recursos desde `/` y la aplicacion fallara.

## 6. Comandos rapidos

Backend:

```bash
cd backend
./mvnw clean package -Dmaven.test.skip=true
./deploy-backend-docker.sh
```

Frontend:

```bash
cd frontend
npm install
npm run build
```

Nginx:

```bash
sudo nginx -t
sudo systemctl reload nginx
```
