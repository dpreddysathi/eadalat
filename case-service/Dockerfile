# Build context is the repo root
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
COPY case-service/pom.xml case-service/pom.xml
COPY case-service/src case-service/src
RUN mvn -B -DskipTests -pl case-service -am package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /build/case-service/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
