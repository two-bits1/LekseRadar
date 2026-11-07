# Сборка и запуск бэкенда (Railway, Render, любой Docker-хостинг)
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B package -DskipTests

FROM eclipse-temurin:21-jre
RUN useradd --system --uid 10001 app
WORKDIR /app
COPY --from=build /src/target/lekseradar-backend-0.1.0.jar app.jar
USER app
ENV PORT=8080
EXPOSE 8080
# Память под небольшой контейнер; PORT задаёт хостинг
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
