# LO8: multi-stage build — compile in Maven image, run in slim JRE
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn -B -ntp package -DskipTests -Pci

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S photoapp && adduser -S photoapp -G photoapp \
    && mkdir -p /app/data /app/uploads \
    && chown -R photoapp:photoapp /app

COPY --from=build /app/target/photoapp-*.jar app.jar

USER photoapp

EXPOSE 8080

VOLUME ["/app/data", "/app/uploads"]

ENV SPRING_PROFILES_ACTIVE=docker

ENTRYPOINT ["java", "-jar", "app.jar"]
