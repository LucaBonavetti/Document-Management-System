# ==== Builder ====
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -e -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests clean package

# ==== Runtime ====
FROM eclipse-temurin:17-jre-noble
WORKDIR /opt/app

RUN apt-get update \
 && apt-get install -y --no-install-recommends \
      tesseract-ocr tesseract-ocr-eng \
      curl ca-certificates \
 && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/target/*.jar app.jar
ENTRYPOINT ["java","-jar","/opt/app/app.jar"]