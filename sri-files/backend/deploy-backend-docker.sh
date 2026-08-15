#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "${SCRIPT_DIR}"

APP_NAME="${APP_NAME:-sri-files}"
IMAGE_NAME="${IMAGE_NAME:-${APP_NAME}:latest}"
CONTAINER_NAME="${CONTAINER_NAME:-sri-files-backend}"
APP_PORT="${APP_PORT:-9090}"
HOST_PORT="${HOST_PORT:-9090}"
ENV_FILE="${ENV_FILE:-.env.prod}"
DOCKER_NETWORK="${DOCKER_NETWORK:-}"
VOLUME_STORAGE_HOST="${VOLUME_STORAGE_HOST:-}"
VOLUME_STORAGE_CONTAINER="${VOLUME_STORAGE_CONTAINER:-/app/storage}"
EXTRA_DOCKER_ARGS="${EXTRA_DOCKER_ARGS:-}"
SKIP_TESTS="${SKIP_TESTS:-true}"

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "[ERROR] No se encontro el comando requerido: $1" >&2
    exit 1
  fi
}

echo "[INFO] Fecha: 2026-08-15"
echo "[INFO] Aplicacion: ${APP_NAME}"
echo "[INFO] Imagen Docker: ${IMAGE_NAME}"
echo "[INFO] Contenedor: ${CONTAINER_NAME}"
echo "[INFO] Puerto host -> contenedor: ${HOST_PORT}:${APP_PORT}"

require_cmd docker
require_cmd mvn

if [[ ! -f "${ENV_FILE}" ]]; then
  echo "[ERROR] No existe el archivo de entorno ${ENV_FILE}" >&2
  echo "[INFO] Puedes crearlo basado en .env.prod.example" >&2
  exit 1
fi

echo "[INFO] Compilando backend Spring Boot..."
if [[ "${SKIP_TESTS}" == "true" ]]; then
  mvn clean package -Dmaven.test.skip=true
else
  mvn clean package
fi

echo "[INFO] Construyendo imagen Docker..."
docker build -t "${IMAGE_NAME}" .

if docker ps -a --format '{{.Names}}' | grep -Fxq "${CONTAINER_NAME}"; then
  echo "[INFO] Eliminando contenedor anterior ${CONTAINER_NAME}..."
  docker rm -f "${CONTAINER_NAME}" >/dev/null
fi

NETWORK_ARGS=()
if [[ -n "${DOCKER_NETWORK}" ]]; then
  if ! docker network inspect "${DOCKER_NETWORK}" >/dev/null 2>&1; then
    echo "[INFO] Creando red Docker ${DOCKER_NETWORK}..."
    docker network create "${DOCKER_NETWORK}" >/dev/null
  fi
  NETWORK_ARGS+=(--network "${DOCKER_NETWORK}")
fi

VOLUME_ARGS=()
if [[ -n "${VOLUME_STORAGE_HOST}" ]]; then
  mkdir -p "${VOLUME_STORAGE_HOST}"
  VOLUME_ARGS+=(-v "${VOLUME_STORAGE_HOST}:${VOLUME_STORAGE_CONTAINER}")
fi

echo "[INFO] Iniciando contenedor ${CONTAINER_NAME}..."
# shellcheck disable=SC2086
docker run -d \
  --name "${CONTAINER_NAME}" \
  --restart unless-stopped \
  -p "${HOST_PORT}:${APP_PORT}" \
  --env-file "${ENV_FILE}" \
  "${NETWORK_ARGS[@]}" \
  "${VOLUME_ARGS[@]}" \
  ${EXTRA_DOCKER_ARGS} \
  "${IMAGE_NAME}"

echo "[INFO] Despliegue backend completado."
echo "[INFO] Logs: docker logs -f ${CONTAINER_NAME}"
