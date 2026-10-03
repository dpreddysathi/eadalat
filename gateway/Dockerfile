# syntax=docker/dockerfile:1
# Build context: repository root (gateway/pom.xml, gateway/src, root pom.xml)

FROM maven:3.9.8-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml ./
COPY gateway/pom.xml gateway/pom.xml
COPY gateway/src gateway/src
RUN mvn -B -DskipTests -pl gateway -am package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/gateway/target/gateway-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
