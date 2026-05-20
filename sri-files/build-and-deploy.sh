#!/bin/bash
set -e

MODULE="sri-files"
cd "$(dirname "$0")"

echo "==============================="
echo " Construyendo $MODULE"
echo "==============================="

echo "[1/3] Compilando JAR..."
chmod +x mvnw
./mvnw clean package -DskipTests

echo "[2/3] Construyendo imagen Docker..."
docker build -t "${MODULE}-app" .

echo "[3/3] Desplegando con docker-compose..."
docker compose up -d --build

echo "==============================="
echo " ✔ $MODULE desplegado"
echo "==============================="
