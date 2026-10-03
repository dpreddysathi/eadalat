# ---- build stage: build context is the repo root ----
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

# copy root pom first so the parent/module metadata is available
COPY pom.xml .
COPY notification-service/pom.xml notification-service/pom.xml
COPY notification-service/src notification-service/src

RUN mvn -B -DskipTests -pl notification-service -am package

# ---- runtime stage ----
FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /app/notification-service/target/notification-service-*.jar app.jar

EXPOSE 8085
ENTRYPOINT ["java", "-jar", "app.jar"]
