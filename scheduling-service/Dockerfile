# Build context is the repository root.
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /build

# Copy the parent pom first for better layer caching, then this module.
COPY pom.xml ./pom.xml
COPY scheduling-service/pom.xml ./scheduling-service/pom.xml
COPY scheduling-service/src ./scheduling-service/src

RUN mvn -B -DskipTests -pl scheduling-service -am package

FROM eclipse-temurin:17-jre

WORKDIR /app
COPY --from=build /build/scheduling-service/target/*.jar app.jar

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "app.jar"]
