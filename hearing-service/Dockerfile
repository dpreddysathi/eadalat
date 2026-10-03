# Build from repo root: docker build -f hearing-service/Dockerfile .
FROM eclipse-temurin:17-jdk AS build
WORKDIR /repo
COPY pom.xml .
COPY hearing-service/pom.xml hearing-service/pom.xml
COPY hearing-service/src hearing-service/src
RUN mvn -B -DskipTests -pl hearing-service -am package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /repo/hearing-service/target/hearing-service-*.jar app.jar
EXPOSE 8086
ENTRYPOINT ["java", "-jar", "app.jar"]
