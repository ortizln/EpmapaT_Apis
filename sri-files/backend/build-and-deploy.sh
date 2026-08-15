#!/bin/bash
set -e

MODULE="sri-files"
cd "$(dirname "$0")"

echo "==============================="
echo " Construyendo $MODULE (backend)"
echo "==============================="

echo "[1/3] Compilando JAR..."
if [ -f ./mvnw ]; then
  chmod +x mvnw || true
  ./mvnw clean package -Dmaven.test.skip=true
else
  mvn clean package -Dmaven.test.skip=true
fi

echo "[2/3] Construyendo imagen Docker..."
docker build -t "${MODULE}-app" .

echo "[3/3] Desplegando con docker-compose..."
docker compose up -d --build

echo "==============================="
echo " Backend $MODULE desplegado"
echo "==============================="
