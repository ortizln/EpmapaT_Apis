#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-prod}"
export SPRING_DATASOURCE_URL="${SPRING_DATASOURCE_URL:-jdbc:postgresql://192.168.0.46:5432/ErpEpmapaT}"
export SPRING_DATASOURCE_USERNAME="${SPRING_DATASOURCE_USERNAME:-postgres}"
export SPRING_DATASOURCE_PASSWORD="${SPRING_DATASOURCE_PASSWORD:-3pmapaT24}"
export SERVER_PORT="${SERVER_PORT:-9090}"

DB_HOST="${DB_HOST:-192.168.0.46}"
DB_PORT="${DB_PORT:-5432}"
BUILD_BEFORE_RUN="${BUILD_BEFORE_RUN:-true}"
SKIP_DB_CHECK="${SKIP_DB_CHECK:-false}"
JAVA_OPTS="${JAVA_OPTS:-}"

echo "[INFO] Fecha: 2026-08-08"
echo "[INFO] Perfil activo: ${SPRING_PROFILES_ACTIVE}"
echo "[INFO] Datasource URL: ${SPRING_DATASOURCE_URL}"
echo "[INFO] Datasource user: ${SPRING_DATASOURCE_USERNAME}"
echo "[INFO] Puerto app: ${SERVER_PORT}"

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "[ERROR] No se encontro el comando requerido: $1" >&2
    exit 1
  fi
}

check_db_connectivity() {
  echo "[INFO] Verificando conectividad a PostgreSQL ${DB_HOST}:${DB_PORT} ..."

  if command -v nc >/dev/null 2>&1; then
    if nc -z -w 5 "$DB_HOST" "$DB_PORT"; then
      echo "[INFO] Conexion TCP a PostgreSQL OK con nc."
      return 0
    fi
    echo "[ERROR] No fue posible abrir conexion TCP a ${DB_HOST}:${DB_PORT} con nc." >&2
    return 1
  fi

  if command -v timeout >/dev/null 2>&1 && command -v bash >/dev/null 2>&1; then
    if timeout 5 bash -c "</dev/tcp/${DB_HOST}/${DB_PORT}" >/dev/null 2>&1; then
      echo "[INFO] Conexion TCP a PostgreSQL OK con /dev/tcp."
      return 0
    fi
    echo "[ERROR] No fue posible abrir conexion TCP a ${DB_HOST}:${DB_PORT} con /dev/tcp." >&2
    return 1
  fi

  echo "[WARN] No se pudo validar conectividad previa porque no existe nc ni timeout."
  return 0
}

if [[ "${SKIP_DB_CHECK}" != "true" ]]; then
  check_db_connectivity
fi

require_cmd java

if [[ "${BUILD_BEFORE_RUN}" == "true" ]]; then
  require_cmd mvn
  echo "[INFO] Compilando proyecto ..."
  mvn -q -DskipTests package
fi

JAR_FILE="$(ls -1 target/sri-files-*.jar 2>/dev/null | head -n 1 || true)"
if [[ -z "${JAR_FILE}" ]]; then
  echo "[ERROR] No se encontro el jar en target/sri-files-*.jar" >&2
  exit 1
fi

echo "[INFO] Iniciando ${JAR_FILE} ..."
exec java ${JAVA_OPTS} -jar "${JAR_FILE}" \
  --spring.profiles.active="${SPRING_PROFILES_ACTIVE}" \
  --server.port="${SERVER_PORT}"
