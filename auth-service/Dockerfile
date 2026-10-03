FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
COPY auth-service/pom.xml auth-service/
COPY auth-service/src auth-service/src
RUN mvn -B -DskipTests -pl auth-service -am package
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /build/auth-service/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java","-jar","app.jar"]
