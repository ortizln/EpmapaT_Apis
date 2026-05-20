#!/bin/bash
set -e

MODULE="epmapaapi"
JAR_FILE="target/${MODULE}-0.0.1.jar"
IMAGE_NAME="${MODULE}-app"

echo "==============================="
echo " Construyendo $MODULE"
echo "==============================="

cd "$(dirname "$0")"

# 1. Compilar JAR
echo "[1/3] Compilando JAR..."
./mvnw clean package -DskipTests

# 2. Construir imagen Docker
echo "[2/3] Construyendo imagen Docker..."
docker build -t "$IMAGE_NAME" .

# 3. Desplegar con docker-compose
echo "[3/3] Desplegando con docker-compose..."
docker compose up -d --build

echo "==============================="
echo " ✔ $MODULE desplegado"
echo "==============================="
