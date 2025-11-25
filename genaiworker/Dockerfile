# ==== Builder ====
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Cache dependencies
COPY pom.xml .
RUN mvn -q -e -DskipTests dependency:go-offline

# Copy sources
COPY src ./src
RUN mvn -q -DskipTests clean package

# ==== Runtime ====
FROM eclipse-temurin:17-jre
WORKDIR /opt/app

COPY --from=build /app/target/*.jar app.jar

ENTRYPOINT ["java","-jar","/opt/app/app.jar"]