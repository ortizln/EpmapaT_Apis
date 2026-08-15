#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 3 ]]; then
  echo "Uso: $0 <service_dir> <artifact_id> <default_port>" >&2
  exit 1
fi

SERVICE_DIR="$1"
ARTIFACT_ID="$2"
DEFAULT_PORT="$3"

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ABS_SERVICE_DIR="${ROOT_DIR}/${SERVICE_DIR}"

if [[ ! -d "${ABS_SERVICE_DIR}" ]]; then
  echo "[ERROR] No existe el directorio del servicio: ${ABS_SERVICE_DIR}" >&2
  exit 1
fi

cd "${ABS_SERVICE_DIR}"

if [[ -f ".env.prod" ]]; then
  set -a
  # shellcheck disable=SC1091
  source ".env.prod"
  set +a
fi

export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-prod}"
export SERVER_PORT="${SERVER_PORT:-${DEFAULT_PORT}}"
BUILD_BEFORE_RUN="${BUILD_BEFORE_RUN:-true}"
SKIP_DB_CHECK="${SKIP_DB_CHECK:-false}"
FOREGROUND="${FOREGROUND:-false}"
JAVA_OPTS="${JAVA_OPTS:-}"

LOG_DIR="${LOG_DIR:-${ABS_SERVICE_DIR}/logs}"
RUN_DIR="${RUN_DIR:-${ABS_SERVICE_DIR}/run}"
mkdir -p "${LOG_DIR}" "${RUN_DIR}"

PID_FILE="${RUN_DIR}/${ARTIFACT_ID}.pid"
LOG_FILE="${LOG_DIR}/${ARTIFACT_ID}.log"

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "[ERROR] No se encontro el comando requerido: $1" >&2
    exit 1
  fi
}

infer_db_host_port() {
  local url="${SPRING_DATASOURCE_URL:-${DB_URL:-}}"
  if [[ -n "${DB_HOST:-}" && -n "${DB_PORT:-}" ]]; then
    return 0
  fi
  if [[ "${url}" =~ ^jdbc:postgresql://([^/:]+):([0-9]+)/ ]]; then
    DB_HOST="${DB_HOST:-${BASH_REMATCH[1]}}"
    DB_PORT="${DB_PORT:-${BASH_REMATCH[2]}}"
  fi
}

check_db_connectivity() {
  infer_db_host_port
  if [[ -z "${DB_HOST:-}" || -z "${DB_PORT:-}" ]]; then
    echo "[WARN] No se pudo inferir DB_HOST/DB_PORT para validar conectividad previa."
    return 0
  fi

  echo "[INFO] Verificando conectividad TCP a PostgreSQL ${DB_HOST}:${DB_PORT} ..."
  if command -v nc >/dev/null 2>&1; then
    nc -z -w 5 "${DB_HOST}" "${DB_PORT}"
    echo "[INFO] Conectividad a base OK con nc."
    return 0
  fi

  if command -v timeout >/dev/null 2>&1; then
    timeout 5 bash -c "</dev/tcp/${DB_HOST}/${DB_PORT}" >/dev/null 2>&1
    echo "[INFO] Conectividad a base OK con /dev/tcp."
    return 0
  fi

  echo "[WARN] No existe nc ni timeout; se omite validacion de red."
}

stop_if_running() {
  if [[ -f "${PID_FILE}" ]]; then
    local pid
    pid="$(cat "${PID_FILE}")"
    if [[ -n "${pid}" ]] && kill -0 "${pid}" >/dev/null 2>&1; then
      echo "[INFO] Deteniendo proceso previo PID=${pid} ..."
      kill "${pid}" || true
      sleep 3
      if kill -0 "${pid}" >/dev/null 2>&1; then
        echo "[WARN] Proceso sigue activo; forzando stop PID=${pid} ..."
        kill -9 "${pid}" || true
      fi
    fi
    rm -f "${PID_FILE}"
  fi
}

find_jar() {
  find target -maxdepth 1 -type f -name "${ARTIFACT_ID}-*.jar" ! -name "*original*.jar" | sort | head -n 1
}

echo "[INFO] Fecha: 2026-08-08"
echo "[INFO] Servicio: ${ARTIFACT_ID}"
echo "[INFO] Directorio: ${ABS_SERVICE_DIR}"
echo "[INFO] Perfil: ${SPRING_PROFILES_ACTIVE}"
echo "[INFO] Puerto: ${SERVER_PORT}"
echo "[INFO] Datasource URL: ${SPRING_DATASOURCE_URL:-${DB_URL:-no-definida}}"

require_cmd java

if [[ "${SKIP_DB_CHECK}" != "true" ]]; then
  check_db_connectivity
fi

if [[ "${BUILD_BEFORE_RUN}" == "true" ]]; then
  require_cmd mvn
  echo "[INFO] Compilando y empaquetando ${ARTIFACT_ID} ..."
  mvn -q -DskipTests package
fi

JAR_FILE="$(find_jar)"
if [[ -z "${JAR_FILE}" ]]; then
  echo "[ERROR] No se encontro un jar valido en target para ${ARTIFACT_ID}" >&2
  exit 1
fi

stop_if_running

echo "[INFO] Jar: ${JAR_FILE}"
echo "[INFO] Log: ${LOG_FILE}"

if [[ "${FOREGROUND}" == "true" ]]; then
  exec java ${JAVA_OPTS} -jar "${JAR_FILE}" \
    --spring.profiles.active="${SPRING_PROFILES_ACTIVE}" \
    --server.port="${SERVER_PORT}"
fi

nohup java ${JAVA_OPTS} -jar "${JAR_FILE}" \
  --spring.profiles.active="${SPRING_PROFILES_ACTIVE}" \
  --server.port="${SERVER_PORT}" \
  > "${LOG_FILE}" 2>&1 &

NEW_PID=$!
echo "${NEW_PID}" > "${PID_FILE}"

sleep 2
if kill -0 "${NEW_PID}" >/dev/null 2>&1; then
  echo "[INFO] ${ARTIFACT_ID} iniciado correctamente. PID=${NEW_PID}"
else
  echo "[ERROR] ${ARTIFACT_ID} no quedo ejecutandose. Revisa ${LOG_FILE}" >&2
  exit 1
fi
