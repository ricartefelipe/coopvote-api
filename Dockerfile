FROM maven:3.9.11-eclipse-temurin-21 AS build

WORKDIR /workspace
COPY pom.xml ./
RUN mvn --batch-mode --no-transfer-progress dependency:go-offline

COPY src ./src
RUN mvn --batch-mode --no-transfer-progress clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S coopvote && adduser -S coopvote -G coopvote
WORKDIR /app
COPY --from=build /workspace/target/*.jar app.jar
COPY docker-entrypoint.sh docker-entrypoint.sh
RUN chmod 0555 docker-entrypoint.sh

USER coopvote
EXPOSE 8080

ENTRYPOINT ["/app/docker-entrypoint.sh"]

