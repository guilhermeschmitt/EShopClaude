# Build stage: Maven + JDK 21
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package

# Runtime stage: JRE 21 + OpenTelemetry Java agent (configured by Aspire's OTEL_* env vars)
FROM eclipse-temurin:21-jre
WORKDIR /app
ADD https://repo1.maven.org/maven2/io/opentelemetry/javaagent/opentelemetry-javaagent/2.34.0/opentelemetry-javaagent-2.34.0.jar /app/opentelemetry-javaagent.jar
COPY --from=build /src/target/app.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-javaagent:/app/opentelemetry-javaagent.jar", "-jar", "/app/app.jar"]
