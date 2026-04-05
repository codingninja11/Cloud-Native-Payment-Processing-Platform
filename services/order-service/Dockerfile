# Build context must be the repository root, e.g.:
#   docker build -f services/order-service/Dockerfile .
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /workspace
COPY services/order-service/pom.xml .
RUN mvn -q -e -B dependency:go-offline
COPY services/order-service/src ./src
RUN mvn -q -e -B package -DskipTests

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache ca-certificates && update-ca-certificates
RUN addgroup -S app && adduser -S app -G app
USER app
WORKDIR /app
COPY --from=build /workspace/target/order-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java","-jar","/app/app.jar"]

