# syntax=docker/dockerfile:1.6
# Image unique pour tous les microservices : Maven compile tout UNE fois (étape "build"),
# puis chaque service a sa petite image d'exécution (cibles discovery, gateway, ...).
# Utilisé par docker-compose.yml (profil "apps") : seul Docker est nécessaire, pas de Java/Maven sur le PC.

FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /src
COPY . .
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp package -DskipTests

FROM eclipse-temurin:17-jre AS runtime
WORKDIR /app
# Mémoire limitée : 8 services tournent en même temps sur le PC
ENV JAVA_TOOL_OPTIONS="-Xms64m -Xmx256m -XX:+UseSerialGC -XX:TieredStopAtLevel=1"
ENTRYPOINT ["java", "-jar", "app.jar"]

FROM runtime AS discovery
COPY --from=build /src/discovery-service/target/discovery-service-1.0.0-SNAPSHOT.jar app.jar

FROM runtime AS gateway
COPY --from=build /src/gateway-service/target/gateway-service-1.0.0-SNAPSHOT.jar app.jar

FROM runtime AS referentiel
COPY --from=build /src/referentiel-service/target/referentiel-service-1.0.0-SNAPSHOT.jar app.jar

FROM runtime AS crm
COPY --from=build /src/crm-service/target/crm-service-1.0.0-SNAPSHOT.jar app.jar

FROM runtime AS vente
COPY --from=build /src/vente-service/target/vente-service-1.0.0-SNAPSHOT.jar app.jar

FROM runtime AS tresorerie
COPY --from=build /src/tresorerie-service/target/tresorerie-service-1.0.0-SNAPSHOT.jar app.jar

FROM runtime AS postvente
COPY --from=build /src/postvente-service/target/postvente-service-1.0.0-SNAPSHOT.jar app.jar

FROM runtime AS admin
COPY --from=build /src/admin-service/target/admin-service-1.0.0-SNAPSHOT.jar app.jar
