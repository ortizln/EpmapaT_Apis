FROM eclipse-temurin:17-jdk-jammy
LABEL authors="Alexis Ortiz"

ARG JAR_FILE=target/bandred-0.0.1.jar
COPY ${JAR_FILE} app_bandred.jar

EXPOSE 9091
ENTRYPOINT ["java", "-jar", "app_bandred.jar"]
