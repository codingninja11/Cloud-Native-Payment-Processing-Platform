# Build context must be the repository root, e.g.:
#   docker build -f services/fraud-service/Dockerfile .
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /workspace
COPY services/fraud-service/pom.xml .
RUN mvn -q -e -B dependency:go-offline
COPY services/fraud-service/src ./src
RUN mvn -q -e -B package -DskipTests

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app
USER app
WORKDIR /app
COPY --from=build /workspace/target/fraud-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java","-jar","/app/app.jar"]

