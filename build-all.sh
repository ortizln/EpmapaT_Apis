#!/bin/bash
set -e

MODULES=("epmapaapi" "bandred" "emails" "pagosonline" "sri-files")
ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"

build_and_deploy() {
    local module="$1"
    echo ""
    echo "========================================"
    echo "  $module"
    echo "========================================"

    cd "$ROOT_DIR/$module"

    echo "[1/3] Compilando JAR..."
    chmod +x mvnw
    ./mvnw clean package -Dmaven.test.skip=true -q

    echo "[2/3] Construyendo imagen Docker..."
    docker build -t "${module}-app" . -q

    echo "[3/3] Desplegando..."
    docker compose up -d --build

    echo " ✔ $module listo"
}

if [ "$1" ]; then
    # Construir un módulo específico
    build_and_deploy "$1"
else
    # Construir todos los módulos
    for m in "${MODULES[@]}"; do
        build_and_deploy "$m"
    done
    echo ""
    echo "========================================"
    echo " Todos los módulos desplegados"
    echo "========================================"
fi
