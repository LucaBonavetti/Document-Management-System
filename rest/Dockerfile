# ==== Builder (cache Maven layers) ====
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
# Cache dependencies
COPY pom.xml .
RUN mvn -q -e -DskipTests dependency:go-offline
# Now copy sources
COPY src ./src
RUN mvn -q -DskipTests package

# ==== Runtime ====
FROM eclipse-temurin:17-jre
WORKDIR /opt/app

RUN apt-get update \
 && apt-get install -y --no-install-recommends curl ca-certificates \
 && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java","-jar","/opt/app/app.jar"]
