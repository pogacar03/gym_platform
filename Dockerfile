FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

RUN groupadd --system fitmate && useradd --system --gid fitmate fitmate
COPY --from=build /workspace/target/fitmate-0.0.1-SNAPSHOT.jar /app/fitmate.jar

USER fitmate
EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/fitmate.jar"]
