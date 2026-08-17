# ============================================================
# Stage 1 - Build do JAR com Maven e JDK 21
# ============================================================
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline

COPY src ./src
RUN mvn -B -ntp clean package -DskipTests

# ============================================================
# Stage 2 - Runtime enxuto com JRE 21 e usuario nao-root
# ============================================================
FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app

RUN groupadd --system spring && useradd --system --gid spring spring \
    && mkdir -p /data/uploads/images \
    && chown -R spring:spring /data

COPY --from=build /app/target/vitral-backend-*.jar app.jar

USER spring
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
