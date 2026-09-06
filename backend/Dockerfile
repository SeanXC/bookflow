FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw \
    && ./mvnw --batch-mode --no-transfer-progress dependency:go-offline

COPY src/ src/
RUN ./mvnw --batch-mode --no-transfer-progress package -DskipTests \
    && find target -maxdepth 1 -type f -name "*.jar" \
        ! -name "*.jar.original" -exec cp {} app.jar \;

FROM eclipse-temurin:21-jre-alpine AS runtime

RUN addgroup -S bookflow \
    && adduser -S -G bookflow bookflow

WORKDIR /app

COPY --from=build --chown=bookflow:bookflow /workspace/app.jar app.jar

USER bookflow
EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
