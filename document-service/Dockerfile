# syntax=docker/dockerfile:1

# ---------- Build stage ----------
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

# Copy root aggregator pom plus this module's sources so Maven can resolve the parent.
COPY pom.xml ./
COPY document-service/pom.xml document-service/pom.xml
COPY document-service/src document-service/src

RUN mvn -B -DskipTests -pl document-service -am package

# ---------- Runtime stage ----------
FROM eclipse-temurin:17-jre
WORKDIR /app

ENV UPLOAD_DIR=/data/uploads
RUN mkdir -p /data/uploads

COPY --from=build /app/document-service/target/*.jar app.jar

EXPOSE 8084

ENTRYPOINT ["java", "-jar", "app.jar"]
