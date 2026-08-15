#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEPLOY_SCRIPT="${ROOT_DIR}/scripts/deploy-java-service.sh"

SERVICES=(
  "bandred bandred 9091"
  "emails emails 9099"
  "epmapaapi epmapaapi 8080"
  "pagosonline pagosonline 9092"
  "sri-files sri-files 9090"
)

for item in "${SERVICES[@]}"; do
  echo "=================================================="
  echo "[INFO] Desplegando ${item}"
  bash "${DEPLOY_SCRIPT}" ${item}
done

echo "=================================================="
echo "[INFO] Despliegue finalizado."
