# Stage 1: build with Maven
FROM maven:3.9.4-eclipse-temurin-17 AS builder
WORKDIR /app
# copiar pom y fuentes
COPY pom.xml .
COPY src ./src
# build (omitir tests para acelerar; quitar -DskipTests si quieres ejecutar tests)
RUN mvn -B -DskipTests package

# Stage 2: runtime
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
# copiar el jar generado (usa wildcard para evitar depender del nombre exacto)
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java","-jar","/app/app.jar"]
