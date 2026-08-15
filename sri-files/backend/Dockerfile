FROM eclipse-temurin:17-jdk-jammy
LABEL authors="Alexis Ortiz"

# Instalar librerías necesarias para JasperReports
RUN apt-get update && apt-get install -y \
    libfreetype6 \
    libfontconfig1 \
    libxrender1 \
    libxext6 \
    && rm -rf /var/lib/apt/lists/*

# Copiar el JAR compilado
ARG JAR_FILE=target/sri-files-0.0.1.jar
COPY ${JAR_FILE} app_sri-files.jar

EXPOSE 9090

ENTRYPOINT ["java", "-jar", "app_sri-files.jar"]
