FROM node:22-alpine AS ui
WORKDIR /app/ui
COPY ui/package.json ui/package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY ui/ ./
RUN npm run lint && npm run build

FROM maven:3.9.11-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY src/ src/
COPY --from=ui /app/target/classes/static/ target/classes/static/
RUN mvn -B verify

FROM eclipse-temurin:17-jre
RUN groupadd --gid 10001 kiosk && useradd --uid 10001 --gid kiosk --create-home kiosk \
    && mkdir /data && chown kiosk:kiosk /data
WORKDIR /app
COPY --from=build /workspace/target/edge-kiosk-0.0.1-SNAPSHOT.jar app.jar
USER kiosk
ENV EDGE_DATABASE=/data/kiosk.db EDGE_BIND_ADDRESS=0.0.0.0
EXPOSE 8090
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
