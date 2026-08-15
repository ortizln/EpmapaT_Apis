#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FRONTEND_DIR="${SCRIPT_DIR}/frontend"
cd "${FRONTEND_DIR}"

APP_NAME="${APP_NAME:-sri-files-frontend}"
BUILD_CONFIGURATION="${BUILD_CONFIGURATION:-production}"
PUBLIC_DIR="${PUBLIC_DIR:-/var/www/${APP_NAME}}"
NGINX_SITES_AVAILABLE="${NGINX_SITES_AVAILABLE:-/etc/nginx/sites-available}"
NGINX_SITES_ENABLED="${NGINX_SITES_ENABLED:-/etc/nginx/sites-enabled}"
NGINX_CONF_NAME="${NGINX_CONF_NAME:-${APP_NAME}.conf}"
SERVER_NAME="${SERVER_NAME:-_}"
API_BASE_URL="${API_BASE_URL:-http://127.0.0.1:9090}"
NGINX_SERVICE_NAME="${NGINX_SERVICE_NAME:-nginx}"
USE_SUDO="${USE_SUDO:-auto}"
INSTALL_NGINX_CONF="${INSTALL_NGINX_CONF:-true}"

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "[ERROR] No se encontro el comando requerido: $1" >&2
    exit 1
  fi
}

run_root() {
  if [[ "${USE_SUDO}" == "never" ]]; then
    "$@"
    return
  fi

  if [[ "${EUID}" -eq 0 ]]; then
    "$@"
    return
  fi

  if [[ "${USE_SUDO}" == "always" || "${USE_SUDO}" == "auto" ]]; then
    require_cmd sudo
    sudo "$@"
    return
  fi

  "$@"
}

echo "[INFO] Fecha: 2026-08-15"
echo "[INFO] Build Angular: ${BUILD_CONFIGURATION}"
echo "[INFO] Directorio publico Nginx: ${PUBLIC_DIR}"
echo "[INFO] API backend: ${API_BASE_URL}"

require_cmd npm
require_cmd node
require_cmd nginx

echo "[INFO] Instalando dependencias del frontend..."
npm install

echo "[INFO] Generando build Angular..."
npx ng build --configuration "${BUILD_CONFIGURATION}"

BUILD_DIR="${FRONTEND_DIR}/dist/frontend/browser"
if [[ ! -d "${BUILD_DIR}" ]]; then
  echo "[ERROR] No se encontro el directorio compilado ${BUILD_DIR}" >&2
  exit 1
fi

echo "[INFO] Publicando archivos estaticos..."
run_root mkdir -p "${PUBLIC_DIR}"
run_root find "${PUBLIC_DIR}" -mindepth 1 -maxdepth 1 -exec rm -rf {} +
run_root cp -R "${BUILD_DIR}/." "${PUBLIC_DIR}/"

if [[ "${INSTALL_NGINX_CONF}" == "true" ]]; then
  TEMPLATE_FILE="${SCRIPT_DIR}/frontend/deploy/nginx.sri-files.conf.template"
  if [[ ! -f "${TEMPLATE_FILE}" ]]; then
    echo "[ERROR] No existe la plantilla Nginx ${TEMPLATE_FILE}" >&2
    exit 1
  fi

  TMP_CONF="$(mktemp)"
  sed \
    -e "s|__SERVER_NAME__|${SERVER_NAME}|g" \
    -e "s|__PUBLIC_DIR__|${PUBLIC_DIR}|g" \
    -e "s|__API_BASE_URL__|${API_BASE_URL}|g" \
    "${TEMPLATE_FILE}" > "${TMP_CONF}"

  echo "[INFO] Instalando configuracion Nginx ${NGINX_CONF_NAME}..."
  run_root mkdir -p "${NGINX_SITES_AVAILABLE}" "${NGINX_SITES_ENABLED}"
  run_root cp "${TMP_CONF}" "${NGINX_SITES_AVAILABLE}/${NGINX_CONF_NAME}"
  run_root ln -sfn "${NGINX_SITES_AVAILABLE}/${NGINX_CONF_NAME}" "${NGINX_SITES_ENABLED}/${NGINX_CONF_NAME}"
  rm -f "${TMP_CONF}"

  echo "[INFO] Validando configuracion Nginx..."
  run_root nginx -t

  echo "[INFO] Recargando Nginx..."
  run_root systemctl reload "${NGINX_SERVICE_NAME}"
fi

echo "[INFO] Despliegue frontend completado."
echo "[INFO] Archivos publicados en ${PUBLIC_DIR}"
