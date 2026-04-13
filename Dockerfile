FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
COPY src src

RUN chmod +x mvnw && ./mvnw -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app

RUN addgroup --system sentimospadel && adduser --system --ingroup sentimospadel sentimospadel

COPY --from=build /app/target/backend-0.0.1-SNAPSHOT.jar app.jar

RUN mkdir -p /var/sentimospadel/player-profile-photos && chown -R sentimospadel:sentimospadel /app /var/sentimospadel

USER sentimospadel

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
