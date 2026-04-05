# Build context must be the repository root, e.g.:
#   docker build -f services/payment-service/Dockerfile .
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /workspace
COPY services/payment-service/pom.xml .
RUN mvn -q -e -B dependency:go-offline
COPY services/payment-service/src ./src
RUN mvn -q -e -B package -DskipTests

FROM eclipse-temurin:21-jre-alpine
# Ensure TLS trust store can validate Supabase (and other cloud) Postgres endpoints on minimal Alpine images.
RUN apk add --no-cache ca-certificates && update-ca-certificates
RUN addgroup -S app && adduser -S app -G app
USER app
WORKDIR /app
COPY --from=build /workspace/target/payment-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
# Prefer IPv4 when connecting to external Postgres (e.g. Supabase from Render); avoids some IPv6-only DNS paths that never complete.
ENTRYPOINT ["java","-Djava.net.preferIPv4Stack=true","-jar","/app/app.jar"]

