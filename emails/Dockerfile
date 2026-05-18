FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml pom.xml
COPY src src

RUN mvn package -DskipTests

FROM eclipse-temurin:17-jdk-jammy
LABEL authors="Alexis Ortiz"

RUN apt-get update && apt-get install -y \
    libfreetype6 \
    libfontconfig1 \
    libxrender1 \
    libxext6 \
    && rm -rf /var/lib/apt/lists/*

COPY --from=build /workspace/target/emails-0.0.1.jar app_emails.jar

EXPOSE 9099

ENTRYPOINT ["java", "-jar", "app_emails.jar"]
